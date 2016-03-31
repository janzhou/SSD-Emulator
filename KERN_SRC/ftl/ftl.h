#ifndef SSD_FTL
#define SSD_FTL

#include <linux/types.h>

#define TABLE_SIZE 1024
#define CACHE_SIZE 128

#define BLOCK_NUMBER 1024
#define PAGE_PER_BLOCK 512
#define PAGE_NUMBER ( BLOCK_NUMBER * PAGE_PER_BLOCK )

#define FLASH_PAGE_READ_DELAY   25
#define FLASH_PAGE_WRITE_DELAY  300
#define FLASH_BLOCK_ERASE_DELAY 2000

struct ftl {
  void (* init)(void); // init
  u32 (* read)(u32 lpn); // return ppn
  u32 (* write)(u32 lpn); // return ppn
};

extern struct ftl dftl;
extern struct ftl direct_ftl;
extern struct ftl user_ftl;

#define default_ftl user_ftl

#endif
