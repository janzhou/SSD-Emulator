#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <signal.h>

#include "ftl.h"
#include "../../KERN_SRC/ssd_blkdev.h"

#define SSD_DEV_NODE	"/dev/ssd_ramdisk"

static struct sector_request_map request_map;
static struct ftl *ftl = &default_ftl;

static unsigned long req_cnt;

int main()
{
	int fd, i;
	struct sigaction sig_action;
	sigset_t block_mask;

	fd = open(SSD_DEV_NODE, O_RDWR);
	if (fd < 0) {
		perror("Failed to open the device node");
		return errno;
	}

	ioctl(fd, SSD_BLKDEV_REGISTER_APP);
	ftl->init();

	printf("Successfully registered the application with SSD RAMDISK driver..\n");

	while (1) {
		ioctl(fd, SSD_BLKDEV_GET_LBN, &request_map);

		for (i = 0; i < request_map.num_sectors; i++) {

			if (request_map.dir == READ)
				request_map.psn[i] = ftl->read(request_map.start_lba + i);
			else
				request_map.psn[i] = ftl->write(request_map.start_lba + i);

			printf("[%lu] Request LBA: %lu; PPN: %lu; Size: %u sectors; Dir: %d\n",
					++req_cnt, request_map.start_lba + i, request_map.psn[i],
					request_map.num_sectors, request_map.dir);

//				request_map.ppn = request_map.lba;
		}

		ioctl(fd, SSD_BLKDEV_SET_PPN, &request_map);
	}

	close(fd);
	return 0;
}
