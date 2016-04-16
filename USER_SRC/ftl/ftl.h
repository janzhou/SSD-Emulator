#ifndef SSD_FTL
#define SSD_FTL

#include <stdint.h>
#include "../../KERN_SRC/ssd_blkdev.h"

enum {
	false	= 0,
	true	= 1
};

typedef unsigned short bool;

#define CACHE_SIZE 128

//#define NR_BLOCKS 1024
//#define NR_PAGES_PER_BLOCK 1024
//#define NR_PAGES (NR_BLOCKS * NR_PAGES_PER_BLOCK)
//
//#define PAGE_SIZE	4096
//#define SECTOR_SIZE	 512
//#define NR_SECTORS_PER_PAGE		(PAGE_SIZE / SECTOR_SIZE)
//
//#define TABLE_SIZE (NR_PAGES * NR_SECTORS_PER_PAGE)

#define FLASH_PAGE_READ_DELAY   25
#define FLASH_PAGE_WRITE_DELAY  300
#define FLASH_BLOCK_ERASE_DELAY 2000

struct ftl {
  void (*init)(void); // init
  uint32_t (*read)(uint32_t lpn); // return ppn
  uint32_t (*write)(uint32_t lpn); // return ppn
};

extern struct ftl dftl;
extern struct ftl direct_ftl;
extern struct ftl user_ftl;

#define default_ftl user_ftl;

#endif
