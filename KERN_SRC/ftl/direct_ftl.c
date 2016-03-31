#include "ftl.h"
#include <linux/delay.h>

void direct_ftl_init(void) {
  return;
}

u32 direct_ftl_read(u32 lpn) {
  udelay(FLASH_PAGE_READ_DELAY);
  return lpn;
}

u32 direct_ftl_write(u32 lpn) {
  udelay(FLASH_PAGE_WRITE_DELAY);
  return lpn;
}

struct ftl direct_ftl = {
  direct_ftl_init,
  direct_ftl_read,
  direct_ftl_write
};
