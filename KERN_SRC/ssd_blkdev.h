#ifndef SSD_BLKDEV_IOCTL_H_
#define SSD_BLKDEV_IOCTL_H_

#define SSD_BLKDEV_MAGIC	'x'

#define SSD_BLKDEV_REGISTER_APP	_IO(SSD_BLKDEV_MAGIC, 0)
#define SSD_BLKDEV_GET_LBN		_IOR(SSD_BLKDEV_MAGIC, 1, unsigned long)
#define SSD_BLKDEV_SET_PPN		_IOW(SSD_BLKDEV_MAGIC, 2, unsigned long)

struct sector_request_map {
	unsigned long lba;
	unsigned long ppn;
	int dir;
	unsigned int num_sectors;
};

/* Below data should be correlated with USER_SRC/ftl/ftl.h */
#define NR_BLOCKS 1024
#define NR_PAGES_PER_BLOCK 1024
#define NR_PAGES (NR_BLOCKS * NR_PAGES_PER_BLOCK)

#endif /* SSD_BLKDEV_IOCTL_H_ */
