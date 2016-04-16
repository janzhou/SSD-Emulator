#ifndef SSD_BLKDEV_IOCTL_H_
#define SSD_BLKDEV_IOCTL_H_

#define SSD_BLKDEV_MAGIC	'x'

#define SSD_BLKDEV_REGISTER_APP	_IO(SSD_BLKDEV_MAGIC, 0)
#define SSD_BLKDEV_GET_REQ_SIZE	_IOR(SSD_BLKDEV_MAGIC, 1, unsigned long)
#define SSD_BLKDEV_GET_LBN		_IOR(SSD_BLKDEV_MAGIC, 2, unsigned long)
#define SSD_BLKDEV_SET_PPN		_IOW(SSD_BLKDEV_MAGIC, 3, unsigned long)

#define SSD_REQUEST_SIZE	8

enum ssd_transfer_direction {
	SSD_DIR_READ,
	SSD_DIR_WRITE
};

/*
 * ssd_page_map: Holds the mapping information. Used to
 * communicate with the user-space.
 *
 * lpn: Logical Page Number. Calculated by the kernel
 * ppn: Physical Page Number. Given by the user, based on a map of lpn
 * new_ppn: New physical page number. Offered by the user only for writes
 * dir: Direction: 1-Write; 0-Read
 */
struct ssd_page_map {
	unsigned long lpn;
	unsigned long ppn;
	unsigned long new_ppn;
	enum ssd_transfer_direction dir;
};

/*
 * sector_request_map: Holds the mapping information plus additional
 * information to process the I/O
 *
 * page_map: The page map information obtained from the user
 * start_sector: The starting sector number (as per the kernel)
 * num_sectors: Number of sectors to read/write
 * request_buff: Pointer to kernel's read/write buffer
 */
struct sector_request_map {
	struct ssd_page_map page_map;
	unsigned long start_sector;
	unsigned long num_sectors;
	void *request_buff;
};


#define SSD_NR_BLOCKS (unsigned long) 128
#define SSD_NR_PAGES_PER_BLOCK (unsigned long) 1024
#define SSD_NR_PAGES (SSD_NR_BLOCKS * SSD_NR_PAGES_PER_BLOCK)

#define SSD_PAGE_SIZE	(unsigned long) 4096
#define SSD_SECTOR_SIZE	 (unsigned long) 512
#define SSD_NR_SECTORS_PER_PAGE		(SSD_PAGE_SIZE / SSD_SECTOR_SIZE)

#define SSD_TOTAL_SIZE	(SSD_NR_BLOCKS * SSD_NR_PAGES_PER_BLOCK * SSD_NR_SECTORS_PER_PAGE * SSD_SECTOR_SIZE)

#define SSD_MAP_TABLE_SIZE (SSD_NR_PAGES * SSD_NR_SECTORS_PER_PAGE)

#endif /* SSD_BLKDEV_IOCTL_H_ */
