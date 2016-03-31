#include "ftl.h"
#include <linux/delay.h>

void user_ftl_init(void) {
  return;
}

u32 user_ftl_read(u32 lpn) {
  udelay(FLASH_PAGE_READ_DELAY);
  return lpn;
}

u32 user_ftl_write(u32 lpn) {
  udelay(FLASH_PAGE_WRITE_DELAY);
  return lpn;
}

struct ftl user_ftl = {
  user_ftl_init,
  user_ftl_read,
  user_ftl_write
};
