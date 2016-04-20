/*
===============================================================================
Driver Name		:		ssd_blkdev
Author			:		RAGHAVENDRA
License			:		GPL
Description		:		LINUX DEVICE DRIVER PROJECT
===============================================================================
*/

#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/blkdev.h>
#include <linux/vmalloc.h>

#include "ssd_blkdev.h"
#include "config.h"

#define DRIVER_NAME "ssd_blkdev"
#define PDEBUG(fmt,args...) pr_debug("%s: "fmt,DRIVER_NAME, ##args)
#define PERR(fmt,args...) pr_err("%s: "fmt,DRIVER_NAME,##args)
#define PINFO(fmt,args...) pr_info("%s: "fmt,DRIVER_NAME, ##args)

#define SSD_DEV_NAME		"ssd_blkdev"
#define SSD_DEV_REQUEST_QUEUE 	1024
#define SSD_DEV_MAX_PARTITION	1

#define SSD_DEV_MAX_MINORS 4

#define SSD_INVOLVE_USER

static struct sector_request_map *request_map;
static unsigned int request_size;

struct ssd_request_list {
	struct request_queue *q;
	struct list_head list;
};

static LIST_HEAD(ssd_request_list_head);

static struct work_struct ssd_request_wrk;

static struct gendisk *ssd_disk;
static struct request_queue *ssd_queue;
static spinlock_t ssd_lck;

static wait_queue_head_t sector_lba_wq;
static wait_queue_head_t sector_ppn_wq;
static wait_queue_head_t req_size_wq;

static u8 lba_wait_flag, ppn_wait_flag, req_size_flag;

static struct task_struct *user_app;

struct ssd_page_buff {
	u8 buff[SSD_PAGE_SIZE];
};

static struct ssd_page_buff ssd_page_buff;

static void *ssd_dev_data;

int major;

static void ssd_dev_move_page(struct ssd_move_page ssd_move_page)
{
	unsigned long new_ppn = ssd_move_page.new_ppn;
	unsigned long old_ppn = ssd_move_page.old_ppn;

	memcpy(ssd_dev_data + new_ppn * SSD_PAGE_SIZE,
			ssd_dev_data + old_ppn * SSD_PAGE_SIZE, SSD_PAGE_SIZE);
}

static int ssd_dev_ioctl(struct block_device *blkdev, fmode_t mode,
		unsigned cmd, unsigned long arg)
{
	struct ssd_move_page ssd_move_page;

	switch (cmd) {
	case SSD_BLKDEV_REGISTER_APP:
		user_app = current;
		break;

	case SSD_BLKDEV_GET_REQ_SIZE:
		wait_event_interruptible(req_size_wq, req_size_flag);
		put_user(request_size, (unsigned long __user *) arg);
		req_size_flag = 0;
		break;

	case SSD_BLKDEV_GET_LBN:
		wait_event_interruptible(sector_lba_wq, lba_wait_flag);
		copy_to_user((struct sector_request_map __user *) arg, request_map, sizeof(*request_map) * request_size);
		lba_wait_flag = 0;
		break;

	case SSD_BLKDEV_SET_PPN:
		copy_from_user(request_map, (struct sector_request_map __user *) arg, sizeof(*request_map) * request_size);
		ppn_wait_flag = 1;
		wake_up_interruptible(&sector_ppn_wq);
		break;

	case SSD_BLKDEV_MOVE_PAGE:
		copy_from_user((struct ssd_move_page *) &ssd_move_page,
		(struct ssd_move_page __user *)arg, sizeof(ssd_move_page));
		ssd_dev_move_page(ssd_move_page);
		break;

	default:
		return -ENOTTY;
	}

	return 0;
}

static void ssd_dev_read_page(unsigned long psn)
{
	unsigned long ppn = psn / SSD_NR_SECTORS_PER_PAGE;

	if (ppn >= SSD_TOTAL_SIZE / SSD_PAGE_SIZE) {
			PERR("Reached read capacity\n");
			return;
	}

	memcpy(ssd_page_buff.buff, ssd_dev_data + ppn * SSD_PAGE_SIZE, SSD_PAGE_SIZE);
}

static void ssd_dev_read(unsigned long psn, u8 *buff)
{
	unsigned long sector_offset;

	/* Read before a write request */
	if (psn == SSD_MAP_TABLE_SIZE)
		return;

//	PINFO("Read: PSN: %lu\n", psn);

	/* Read the page from the disk to the page buffer */
	ssd_dev_read_page(psn);

	/* Pick the requested sector and put the data into the buffer */
	sector_offset = psn % SSD_NR_SECTORS_PER_PAGE;
//	PINFO("Read: sector_offset: %lu\n", sector_offset);
	memcpy(buff, ssd_page_buff.buff + sector_offset * SSD_SECTOR_SIZE, SSD_SECTOR_SIZE);
}

static void ssd_dev_write_page(unsigned long psn)
{
	unsigned long ppn = psn / SSD_NR_SECTORS_PER_PAGE;

	if (ppn >= SSD_TOTAL_SIZE / SSD_PAGE_SIZE) {
		PERR("Reached write capacity\n");
		return;
	}

	memcpy(ssd_dev_data + ppn * SSD_PAGE_SIZE, ssd_page_buff.buff, SSD_PAGE_SIZE);
}

static void ssd_dev_write(unsigned long psn, u8 *buff)
{
	unsigned long sector_offset;

	/* Perform a Read-Modify-Update Operation */
	ssd_dev_read_page(psn);

	sector_offset = psn % SSD_NR_SECTORS_PER_PAGE;
	memcpy(ssd_page_buff.buff + sector_offset * SSD_SECTOR_SIZE,
			buff, SSD_SECTOR_SIZE);

	ssd_dev_write_page(psn);
}

static int ssd_transfer(struct request *req)
{
	int dir = rq_data_dir(req);
	sector_t start_sector = blk_rq_pos(req);
	unsigned int nr_sectors = blk_rq_sectors(req);

	struct bio_vec bv;
	struct req_iterator iter;
	sector_t sector_offset = 0;

	void *temp_buff;

	int ret = 0;
	int i, j;

	request_size = nr_sectors / SSD_REQUEST_SIZE;
	if (request_size == 0)
		request_size = 1;
#ifdef SSD_INVOLVE_USER
	req_size_flag = 1;
	wake_up_interruptible(&req_size_wq);
#endif

	request_map = kzalloc(request_size * sizeof(struct sector_request_map), GFP_KERNEL);
	if (!request_map)
		return -ENOMEM;

//	PINFO("Request: Dir: %d; Sector: %lu; Cnt: %d; Request_size: %u\n",
//			dir, start_sector, nr_sectors, request_size);

	/* Buffer requests */
	i = 0;
	rq_for_each_segment(bv, req, iter) {
		struct sector_request_map *req_map = &request_map[i];

		req_map->request_buff = page_address(bv.bv_page) + bv.bv_offset;
		req_map->num_sectors = bv.bv_len / SSD_SECTOR_SIZE;
		if (req_map->num_sectors > SSD_REQUEST_SIZE)
			return -EIO;

//		PINFO("Bio1: Sector offset: %lu; buff: %p; Length: %u (%d sectors)\n",
//				sector_offset, req_map->request_buff, bv.bv_len, req_map->num_sectors);

		req_map->dir = dir;
		req_map->start_lba = start_sector + sector_offset;

		sector_offset += req_map->num_sectors;
		i++;
	}

	/* Synchronize with the user */
#ifdef SSD_INVOLVE_USER
	lba_wait_flag = 1;
	wake_up_interruptible(&sector_lba_wq);

	wait_event_interruptible(sector_ppn_wq, ppn_wait_flag);
	ppn_wait_flag = 0;
#endif

	/* Perform I/O */
	for (i = 0; i < request_size; i++) {
		struct sector_request_map *req_map = &request_map[i];
#ifndef SSD_INVOLVE_USER
		unsigned int lba = req_map->start_lba;
#endif

//		PINFO("Bio2: buff: %p; Length: %d sectors\n",
//				req_map->request_buff, req_map->num_sectors);

		for (j = 0; j < req_map->num_sectors; j++) {
			temp_buff = req_map->request_buff + j * SSD_SECTOR_SIZE;
#ifndef SSD_INVOLVE_USER
			req_map->psn[j] = lba++;
#endif

			if (req_map->dir == WRITE)
				ssd_dev_write(req_map->psn[j], temp_buff);
			else
				ssd_dev_read(req_map->psn[j], temp_buff);
		}
	}

	kfree(request_map);

	if (sector_offset != nr_sectors) {
		PERR("Bio info doesn't match with req info\n");
	}

	return ret;
}

static void ssd_request_func(struct work_struct *work)
{
	int ret = 0;
	struct request_queue *q;
	struct request *req;
	struct ssd_request_list *ssd_req_node, *temp_req_node;

//	rcu_read_lock();

	list_for_each_entry_safe(ssd_req_node, temp_req_node, &ssd_request_list_head, list) {
//		rcu_read_unlock();

		q = ssd_req_node->q;

		while ((req = blk_fetch_request(q)) != NULL) {
			if (user_app)
				ret = ssd_transfer(req);

			__blk_end_request_all(req, ret);
		}

		list_del_rcu(&ssd_req_node->list);
		kfree(ssd_req_node);

//		rcu_read_lock();
	}

//	rcu_read_unlock();
}

static void ssd_make_request(struct request_queue *q)
{
	struct ssd_request_list *ssd_req_node;

	ssd_req_node = kzalloc(sizeof(*ssd_req_node), GFP_KERNEL);
	if (!ssd_req_node)
		return;

	ssd_req_node->q = q;
	list_add_tail_rcu(&ssd_req_node->list, &ssd_request_list_head);

	/* Start the I/O request processing in the process context */
	schedule_work(&ssd_request_wrk);
}

void ssd_dev_release(struct gendisk *gendisk, fmode_t mode)
{
	if (user_app != current)
		return;

	user_app = NULL;
}

static const struct block_device_operations ssd_fops = {
		.owner = THIS_MODULE,
		.ioctl = ssd_dev_ioctl,
		.release = ssd_dev_release
};

static int ssd_dev_create(void)
{
	int ret = 0;

	spin_lock_init(&ssd_lck);
	ssd_queue = blk_init_queue(ssd_make_request, &ssd_lck);
	if (!ssd_queue) {
		PERR("Failed to allocate the request queue\n");
		return PTR_ERR(ssd_queue);
	}

	blk_queue_max_hw_sectors(ssd_queue, SSD_DEV_REQUEST_QUEUE);

	ssd_disk = alloc_disk(SSD_DEV_MAX_PARTITION);
	if (!ssd_disk) {
		PERR("Failed to allocate gendisk\n");
		ret = PTR_ERR(ssd_disk);
		goto disk_fail;
	}

	ssd_disk->major = major;
	ssd_disk->first_minor = 0;
	ssd_disk->minors = SSD_DEV_MAX_MINORS;
	ssd_disk->fops = &ssd_fops;
	ssd_disk->queue = ssd_queue;
	sprintf(ssd_disk->disk_name, "ssd_ramdisk");
	set_capacity(ssd_disk, SSD_NR_PAGES * SSD_NR_SECTORS_PER_PAGE);

	/* Allocate SSD flash memory and page buffer */
	ssd_dev_data = vzalloc(SSD_TOTAL_SIZE);
	if (!ssd_dev_data) {
		PERR("Failed to allocate memory for the disk space\n");
		ret = -ENOMEM;
		goto vzalloc_fail;
	}

	return 0;

vzalloc_fail:
	put_disk(ssd_disk);
disk_fail:
	blk_cleanup_queue(ssd_queue);
	return ret;
}

static void ssd_dev_destroy(void)
{
	vfree(ssd_dev_data);
	put_disk(ssd_disk);
	blk_cleanup_queue(ssd_queue);
}

static int __init ssd_blkdev_init(void)
{
	int ret = 0;

	major = register_blkdev(0, SSD_DEV_NAME);
	if (major < 0) {
		PERR("Failed to allocate a major number\n");
		return major;
	}

	ret = ssd_dev_create();
	if (ret < 0) {
		PERR("Failed to create the disk\n");
		goto create_fail;
	}

	init_waitqueue_head(&sector_lba_wq);
	init_waitqueue_head(&sector_ppn_wq);
	init_waitqueue_head(&req_size_wq);

	INIT_WORK(&ssd_request_wrk, ssd_request_func);

	add_disk(ssd_disk);

	return 0;

create_fail:
	unregister_blkdev(major, SSD_DEV_NAME);
	return 0;
}

static void __exit ssd_blkdev_exit(void)
{
	del_gendisk(ssd_disk);
	ssd_dev_destroy();
	unregister_blkdev(major, SSD_DEV_NAME);
}

module_init(ssd_blkdev_init);
module_exit(ssd_blkdev_exit);

MODULE_LICENSE("GPL");
MODULE_AUTHOR("RAGHAVENDRA");
