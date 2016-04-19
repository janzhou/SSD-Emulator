#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <signal.h>

#include "ftl.h"
#include "../kernel/ssd_blkdev.h"

#define SSD_DEV_NODE	"/dev/ssd_ramdisk"

static unsigned long req_size;
static struct sector_request_map *request_map;
static struct ftl *ftl = &default_ftl;

static unsigned long req_cnt;

int main()
{
	int fd, i, j;
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
		struct sector_request_map *req_map;
		ioctl(fd, SSD_BLKDEV_GET_REQ_SIZE, &req_size);

//		printf("request_size = %lu\n", req_size);

		request_map = calloc(req_size, sizeof(*request_map));
		ioctl(fd, SSD_BLKDEV_GET_LBN, request_map);

		for (j = 0; j < req_size; j++) {
			req_map = &request_map[j];

			for (i = 0; i < req_map->num_sectors; i++) {

				if (req_map->dir == READ)
					req_map->psn[i] = ftl->read(req_map->start_lba + i);
				else
					req_map->psn[i] = ftl->write(req_map->start_lba + i);

//				printf("[%lu] Request LBA: %lu; PPN: %lu; Size: %u sectors; Dir: %d\n",
//						++req_cnt, req_map->start_lba + i, req_map->psn[i],
//						req_map->num_sectors, req_map->dir);

	//				request_map.ppn = request_map.lba;
			}
		}

		ioctl(fd, SSD_BLKDEV_SET_PPN, request_map);

		free(request_map);
	}

	close(fd);
	return 0;
}
