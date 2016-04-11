/* Copyright 2009, 2010 Brendan Tauras */

/* run_test2.cpp is part of FlashSim. */

/* FlashSim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version. */

/* FlashSim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details. */

/* You should have received a copy of the GNU General Public License
 * along with FlashSim.  If not, see <http://www.gnu.org/licenses/>. */

/****************************************************************************/

#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <signal.h>

#include "ssd.h"
#include "../../KERN_SRC/ssd_blkdev.h"

#define SSD_DEV_NODE	"/dev/ssd_ramdisk"

struct sector_request_map request_map;

static unsigned long req_cnt;

using namespace ssd;

int main()
{
	load_config();
	print_config(NULL);

	int fd;
	Ssd *ssd = new Ssd();
	double cur_time = 1.0;
	double delta = BUS_DATA_DELAY - 2 > 0 ? BUS_DATA_DELAY - 2 : BUS_DATA_DELAY;
	enum event_type event_type;

	fd = open(SSD_DEV_NODE, O_RDWR);
	if (fd < 0) {
		perror("Failed to open the device node");
		return errno;
	}

	ioctl(fd, SSD_BLKDEV_REGISTER_APP);
	printf("Registered the application with the driver..\n");

	while (1) {
			ioctl(fd, SSD_BLKDEV_GET_LBN, &request_map);
//			printf("[%lu] Request LBA: %lu; Size: %u sectors; Dir: %d\n",
//					++req_cnt, request_map.lba, request_map.num_sectors, request_map.dir);

			event_type = request_map.dir ? WRITE : READ;
			ssd->event_arrive(event_type, request_map.lba, 1, cur_time);

			printf("[%lu]: LBA: %lu; PPN = %lu; Dir: %d\n", ++req_cnt, request_map.lba, request_map.ppn, request_map.dir);

//			request_map.ppn = request_map.lba;
			ioctl(fd, SSD_BLKDEV_SET_PPN, &request_map);

			cur_time += delta;
	}

	close(fd);

	delete ssd;
	return 0;
}
