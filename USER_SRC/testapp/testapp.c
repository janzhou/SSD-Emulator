#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <signal.h>

#include "../KERN_SRC/ssd_blkdev.h"

#define SSD_DEV_NODE	"/dev/ssd_ramdisk"

static struct sector_request_map request_map;

static unsigned long req_cnt;

int main()
{
	int fd;
	struct sigaction sig_action;
	sigset_t block_mask;

	fd = open(SSD_DEV_NODE, O_RDWR);
	if (fd < 0) {
		perror("Failed to open the device node");
		return errno;
	}

	ioctl(fd, SSD_BLKDEV_REGISTER_APP);


	while (1) {
		ioctl(fd, SSD_BLKDEV_GET_LBN, &request_map);
		printf("[%lu] Request LBA: %lu; Size: %u sectors; Dir: %d\n",
				++req_cnt, request_map.lba, request_map.num_sectors, request_map.dir);

		/* Make LBA to PPN conversion here */
		request_map.ppn = request_map.lba;

		ioctl(fd, SSD_BLKDEV_SET_PPN, &request_map);
	}

	close(fd);
	return 0;
}
