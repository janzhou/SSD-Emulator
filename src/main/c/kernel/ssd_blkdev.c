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

#include "ssd_blkdev.h"

#define DRIVER_NAME "ssd_blkdev"
#define PDEBUG(fmt,args...) pr_debug("%s: "fmt,DRIVER_NAME, ##args)
#define PERR(fmt,args...) pr_err("%s: "fmt,DRIVER_NAME,##args)
#define PINFO(fmt,args...) pr_info("%s: "fmt,DRIVER_NAME, ##args)

#define SSD_DEV_NAME		"ssd_blkdev"
#define SSD_DEV_REQUEST_QUEUE 	1024
#define SSD_DEV_MAX_PARTITION	1

#define SSD_DEV_MAX_MINORS 4

#define SSD_INVOLVE_USER

static struct ssd_request_map *request_map;
static unsigned int request_size;

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
	unsigned long ppn;
	u8 buff[SSD_PAGE_SIZE];
};

static struct ssd_page_buff ssd_page_buff;

static u8 ssd_dev_data[SSD_TOTAL_SIZE];

int major;

static void ssd_dev_move_page(struct ssd_move_page ssd_move_page)
{
	unsigned long new_ppn = ssd_move_page.new_ppn;
	unsigned long old_ppn = ssd_move_page.old_ppn;

	memcpy(ssd_dev_data + new_ppn * SSD_PAGE_SIZE,
			ssd_dev_data + old_ppn * SSD_PAGE_SIZE, SSD_PAGE_SIZE);

	ssd_page_buff.ppn = new_ppn;
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
		copy_to_user((struct ssd_request_map __user *) arg, request_map, sizeof(*request_map) * request_size);
		lba_wait_flag = 0;
		break;

	case SSD_BLKDEV_SET_PPN:
		copy_from_user(request_map, (struct ssd_request_map __user *) arg, sizeof(*request_map) * request_size);
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

static void ssd_dev_read_page(unsigned long ppn)
{
	if (ppn >= SSD_TOTAL_SIZE / SSD_PAGE_SIZE) {
			PERR("Reached read capacity\n");
			return;
	}

	if (ppn == ssd_page_buff.ppn)
		return;

	memcpy(ssd_page_buff.buff, ssd_dev_data + ppn * SSD_PAGE_SIZE, SSD_PAGE_SIZE);
	ssd_page_buff.ppn = ppn;
}

static void ssd_dev_read(struct ssd_request_map *req_map)
{
	struct ssd_page_map *page_map = &req_map->page_map;
	unsigned long ppn = page_map->ppn;
	unsigned long num_sectors = req_map->num_sectors;
	unsigned short sector_offset = req_map->start_sector % SSD_NR_SECTORS_PER_PAGE;
	void *kern_buff = req_map->request_buff;

	/* Read before a write request */
	if (ppn == SSD_MAP_TABLE_SIZE)
		return;

	/* Read the page from the disk to the page buffer */
	ssd_dev_read_page(ppn);

	memcpy(kern_buff, ssd_page_buff.buff + sector_offset * SSD_SECTOR_SIZE,
			num_sectors * SSD_SECTOR_SIZE);
}

static void ssd_dev_write_page(unsigned long new_ppn)
{
	if (new_ppn >= SSD_TOTAL_SIZE / SSD_PAGE_SIZE) {
		PERR("Reached write capacity\n");
		return;
	}

	memcpy(ssd_dev_data + new_ppn * SSD_PAGE_SIZE,
			ssd_page_buff.buff, SSD_PAGE_SIZE);
}

static void ssd_dev_write(struct ssd_request_map *req_map)
{
	struct ssd_page_map *page_map = &req_map->page_map;
	unsigned long ppn = page_map->ppn;
	unsigned long new_ppn = page_map->new_ppn;
	unsigned long num_sectors = req_map->num_sectors;
	unsigned short sector_offset = req_map->start_sector % SSD_NR_SECTORS_PER_PAGE;
	void *kern_buff = req_map->request_buff;

	/* Perform a Read-Modify-Update Operation */
	ssd_dev_read_page(ppn);

	memcpy(ssd_page_buff.buff + sector_offset * SSD_SECTOR_SIZE,
			kern_buff, num_sectors * SSD_SECTOR_SIZE);

	ssd_dev_write_page(new_ppn);

	ssd_page_buff.ppn = new_ppn;
}

static int ssd_transfer(struct request *req)
{
	int dir = rq_data_dir(req);
	sector_t start_sector = blk_rq_pos(req);
	unsigned int nr_sectors = blk_rq_sectors(req);

	struct bio_vec bv;
	struct req_iterator iter;
	sector_t sector_offset = 0;

	int i;
	int ret = 0;

	request_size = nr_sectors / SSD_REQUEST_SIZE;
	if (request_size == 0)
		request_size = 1;
#ifdef SSD_INVOLVE_USER
	req_size_flag = 1;
	wake_up_interruptible(&req_size_wq);
#endif

	request_map = kzalloc(request_size * sizeof(struct ssd_request_map), GFP_KERNEL);
	if (!request_map)
		return -ENOMEM;

//	PINFO("Request: Dir: %d; Sector: %lu; Cnt: %d; Request_size: %u\n",
//			dir, start_sector, nr_sectors, request_size);

	/* Buffer requests */
	i = 0;
	rq_for_each_segment(bv, req, iter) {
		struct ssd_request_map *req_map = &request_map[i++];
		struct ssd_page_map *page_map = &req_map->page_map;
		unsigned long lba = start_sector + sector_offset;

		page_map->lpn = lba / SSD_NR_SECTORS_PER_PAGE;
		page_map->dir = dir;

		req_map->start_sector = lba;
		req_map->request_buff = page_address(bv.bv_page) + bv.bv_offset;
		req_map->num_sectors = bv.bv_len / SSD_SECTOR_SIZE;
		if (req_map->num_sectors > SSD_REQUEST_SIZE)
			return -EIO;

//		PINFO("Bio1: Sector offset: %lu; lpn: %lu; Length: %u (%lu sectors)\n",
//				sector_offset, page_map->lpn, bv.bv_len, req_map->num_sectors);

		sector_offset += req_map->num_sectors;
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
		struct ssd_request_map *req_map = &request_map[i];
		struct ssd_page_map *page_map = &req_map->page_map;

#ifndef SSD_INVOLVE_USER
		page_map->ppn = page_map->new_ppn = page_map->lpn;
#endif

		if (page_map->dir == READ)
			ssd_dev_read(req_map);
		else
			ssd_dev_write(req_map);
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
	struct request *req;

	while ((req = blk_fetch_request(ssd_queue)) != NULL) {
		if (user_app)
			ret = ssd_transfer(req);

		__blk_end_request_all(req, ret);
	}
}

static void ssd_make_request(struct request_queue *q)
{
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

	return 0;

disk_fail:
	blk_cleanup_queue(ssd_queue);
	return ret;
}

static void ssd_dev_destroy(void)
{
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
