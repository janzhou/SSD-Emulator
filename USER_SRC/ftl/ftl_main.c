#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <signal.h>

#include "ftl.h"
#include "../../KERN_SRC/ssd_blkdev.h"

#define SSD_DEV_NODE	"/dev/ssd_ramdisk"

static unsigned long req_size;
static struct ssd_request_map *request_map;
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

		ioctl(fd, SSD_BLKDEV_GET_REQ_SIZE, &req_size);

		printf("request_size = %lu\n", req_size);

		request_map = calloc(req_size, sizeof(*request_map));
		ioctl(fd, SSD_BLKDEV_GET_LBN, request_map);

		for (j = 0; j < req_size; j++) {
			struct ssd_request_map *req_map = &request_map[j];
			struct ssd_page_map *page_map = &req_map->page_map;

			if (page_map->dir == SSD_DIR_READ)
				page_map->ppn = ftl->read(page_map->lpn);
			else {
				page_map->ppn = ftl->read(page_map->lpn);
				page_map->new_ppn = ftl->write(page_map->lpn);
			}

//			printf("[%lu] Request LBA: %lu; PPN: %lu; Size: %u sectors; Dir: %d\n",
//					++req_cnt, req_map->start_lba + i, req_map->psn[i],
//					req_map->num_sectors, req_map->dir);

//				request_map.ppn = request_map.lba;
		}

		ioctl(fd, SSD_BLKDEV_SET_PPN, request_map);

		free(request_map);
	}

	close(fd);
	return 0;
}
