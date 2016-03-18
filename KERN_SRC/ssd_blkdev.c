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

#define DRIVER_NAME "ssd_blkdev"
#define PDEBUG(fmt,args...) pr_debug("%s: "fmt,DRIVER_NAME, ##args)
#define PERR(fmt,args...) pr_err("%s: "fmt,DRIVER_NAME,##args)
#define PINFO(fmt,args...) pr_info("%s: "fmt,DRIVER_NAME, ##args)

#define SSD_DEV_NAME		"ssd_blkdev"
#define SSD_DEV_REQUEST_QUEUE 	1024
#define SSD_DEV_MAX_PARTITION	1
#define SSD_DEV_NUM_SECTORS	1024 * 1024
#define SSD_DEV_SECTOR_SIZE	512
#define NUM_SSD_DEVS		1

static struct gendisk *ssd_disk;
static struct request_queue *ssd_queue;
static spinlock_t ssd_lck;

static u8 *ssd_dev_data;

int major;

static void ssd_dev_write(sector_t sector_offset, u8 *buff, unsigned int sectors)
{
	memcpy(ssd_dev_data + sector_offset * SSD_DEV_SECTOR_SIZE,
			buff, sectors * SSD_DEV_SECTOR_SIZE);
}

static void ssd_dev_read(sector_t sector_offset, u8 *buff, unsigned int sectors)
{
	memcpy(buff, ssd_dev_data + sector_offset * SSD_DEV_SECTOR_SIZE,
			sectors * SSD_DEV_SECTOR_SIZE);
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
	u8 *buff;

	int ret = 0;

	PINFO("Request: Dir: %d; Sector: %lu; Cnt: %d\n",
			dir, start_sector, nr_sectors);

	rq_for_each_segment(bv, req, iter) {
		buff = page_address(bv.bv_page) + bv.bv_offset;
		sectors = bv.bv_len / SSD_DEV_SECTOR_SIZE;

		PINFO("Bio: Sector offset: %lu; Buffer: %p; Length: %d sectors\n",
				sector_offset, buff, sectors);

		if (dir == WRITE)
			ssd_dev_write(start_sector + sector_offset, buff, sectors);
		else
			ssd_dev_read(start_sector + sector_offset, buff, sectors);

		sector_offset += sectors;
	}

	if (sector_offset != nr_sectors) {
		PERR("Bio info doesn't match with req info\n");
	}

	return ret;
}

static void ssd_make_request(struct request_queue *q)
{
	int ret;
	struct request *req;

	while ((req = blk_fetch_request(q)) != NULL) {
		ret = ssd_transfer(req);
		__blk_end_request_all(req, ret);
	}
}

static const struct block_device_operations ssd_fops = {
		.owner = THIS_MODULE,
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
	ssd_disk->fops = &ssd_fops;
	ssd_disk->queue = ssd_queue;
	sprintf(ssd_disk->disk_name, "ssd_ramdisk");
	set_capacity(ssd_disk, SSD_DEV_NUM_SECTORS);

	ssd_dev_data = vmalloc(SSD_DEV_SECTOR_SIZE * SSD_DEV_NUM_SECTORS);
	if (!ssd_dev_data) {
		PERR("Failed to allocate memory for the disk space\n");
		ret = PTR_ERR(ssd_dev_data);
		goto vmalloc_fail;
	}

	return 0;

vmalloc_fail:
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

	PINFO("Allocated major for %s = %d\n", DRIVER_NAME, major);

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
