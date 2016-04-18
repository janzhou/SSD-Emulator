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

static struct sector_request_map *request_map;

#define xprintf(X) printf("%s: 0x%x\n", #X, X)

int main()
{
	xprintf(SSD_BLKDEV_REGISTER_APP);
	xprintf(SSD_BLKDEV_GET_REQ_SIZE);
	xprintf(SSD_BLKDEV_GET_LBN);
	xprintf(SSD_BLKDEV_SET_PPN);
	xprintf(SSD_BLKDEV_MOVE_PAGE);
	xprintf(sizeof(*request_map));
	xprintf(sizeof(struct sector_request_map));
}
