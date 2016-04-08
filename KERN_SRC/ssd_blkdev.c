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

#define DRIVER_NAME "ssd_blkdev"
#define PDEBUG(fmt,args...) pr_debug("%s: "fmt,DRIVER_NAME, ##args)
#define PERR(fmt,args...) pr_err("%s: "fmt,DRIVER_NAME,##args)
#define PINFO(fmt,args...) pr_info("%s: "fmt,DRIVER_NAME, ##args)

#define SSD_DEV_NAME		"ssd_blkdev"
#define SSD_DEV_REQUEST_QUEUE 	1024
#define SSD_DEV_MAX_PARTITION	1

#define SSD_DEV_MAX_MINORS 4

static struct sector_request_map request_map;

static struct gendisk *ssd_disk;
static struct request_queue *ssd_queue;
static spinlock_t ssd_lck;

static wait_queue_head_t sector_lba_wq;
static wait_queue_head_t sector_ppn_wq;

static u8 lba_wait_flag, ppn_wait_flag;

static struct task_struct *user_app;

struct ssd_page_buff {
	u8 buff[SSD_PAGE_SIZE];
};

static struct ssd_page_buff ssd_page_buff;
static void *ssd_dev_data;

int major;

static int ssd_dev_ioctl(struct block_device *blkdev, fmode_t mode,
		unsigned cmd, unsigned long arg)
{
	switch (cmd) {
	case SSD_BLKDEV_REGISTER_APP:
		user_app = current;
		break;

	case SSD_BLKDEV_GET_LBN:
		wait_event_interruptible(sector_lba_wq, lba_wait_flag);
		copy_to_user((struct sector_request_map __user *) arg, &request_map, sizeof(request_map));
		lba_wait_flag = 0;
		break;

	case SSD_BLKDEV_SET_PPN:
		copy_from_user(&request_map, (struct sector_request_map __user *) arg, sizeof(request_map));
		ppn_wait_flag = 1;
		wake_up_interruptible(&sector_ppn_wq);
		break;

	default:
		return -ENOTTY;
	}

	return 0;
}

static void ssd_dev_read_page(unsigned long psn, unsigned long lba)
{
	unsigned long ppn = psn / SSD_NR_SECTORS_PER_PAGE;
//	PINFO("Read: PPN: %lu\n", ppn);

	memcpy(ssd_page_buff.buff, ssd_dev_data + ppn * SSD_PAGE_SIZE, SSD_PAGE_SIZE);
}

static void ssd_dev_read(unsigned long psn, unsigned long lba, u8 *buff)
{
	unsigned long sector_offset;

	/* Read before a write request */
	if (psn == SSD_MAP_TABLE_SIZE)
		return;

//	PINFO("Read: PSN: %lu\n", psn);

	/* Read the page from the disk to the page buffer */
	ssd_dev_read_page(psn, lba);

	/* Pick the requested sector and put the data into the buffer */
	sector_offset = psn % SSD_NR_SECTORS_PER_PAGE;
//	PINFO("Read: sector_offset: %lu\n", sector_offset);
	memcpy(buff, ssd_page_buff.buff + sector_offset * SSD_SECTOR_SIZE, SSD_SECTOR_SIZE);
}

static void ssd_dev_write_page(unsigned long psn, unsigned long lba)
{
	unsigned long ppn = psn / SSD_NR_SECTORS_PER_PAGE;

	memcpy(ssd_dev_data + ppn * SSD_PAGE_SIZE, ssd_page_buff.buff, SSD_PAGE_SIZE);
}

static void ssd_dev_write(unsigned long psn, unsigned long lba, u8 *buff)
{
	unsigned long sector_offset;

	/* Perform a Read-Modify-Update Operation */
	ssd_dev_read_page(psn, lba);

	sector_offset = psn % SSD_NR_SECTORS_PER_PAGE;
	memcpy(ssd_page_buff.buff + sector_offset * SSD_SECTOR_SIZE,
			buff, SSD_SECTOR_SIZE);

	ssd_dev_write_page(psn, lba);
}

static int ssd_transfer(struct request *req)
{
	int dir = rq_data_dir(req);
	sector_t start_sector = blk_rq_pos(req);
	unsigned int nr_sectors = blk_rq_sectors(req);

	struct bio_vec bv;
	struct req_iterator iter;

	sector_t sector_offset = 0;
	unsigned int sectors;
	void *buff, *temp_buff;

	int ret = 0;
	int i;

//	PINFO("Request: Dir: %d; Sector: %lu; Cnt: %d\n",
//			dir, start_sector, nr_sectors);

	rq_for_each_segment(bv, req, iter) {
		buff = page_address(bv.bv_page) + bv.bv_offset;
		sectors = bv.bv_len / SSD_SECTOR_SIZE;

//		PINFO("Bio: Sector offset: %lu; buff: %p; Length: %u (%d sectors)\n",
//				sector_offset, buff, bv.bv_len, sectors);

		request_map.lba = start_sector + sector_offset;
		request_map.dir = dir;
//		request_map.num_sectors = sectors;
		request_map.num_sectors = 1;

		for (i = 0; i < sectors; i++, request_map.lba++) {

//			lba_wait_flag = 1;
//			wake_up_interruptible(&sector_lba_wq);
//
//			wait_event_interruptible(sector_ppn_wq, ppn_wait_flag);
//			ppn_wait_flag = 0;
			request_map.psn = request_map.lba;

			temp_buff = buff + i * SSD_SECTOR_SIZE;
//			PINFO("[%d]: temp_buff: %p\n", i, temp_buff);

			if (dir == WRITE)
				ssd_dev_write(request_map.psn, request_map.lba, temp_buff);
			else
				ssd_dev_read(request_map.psn, request_map.lba, temp_buff);
		}

		sector_offset += sectors;
	}

	if (sector_offset != nr_sectors) {
		PERR("Bio info doesn't match with req info\n");
	}

	return ret;
}

static void ssd_make_request(struct request_queue *q)
{
	int ret = 0;
	struct request *req;

	while ((req = blk_fetch_request(q)) != NULL) {
		if (user_app)
			ret = ssd_transfer(req);

		__blk_end_request_all(req, ret);
	}
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

	add_disk(ssd_disk);

	init_waitqueue_head(&sector_lba_wq);
	init_waitqueue_head(&sector_ppn_wq);

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
