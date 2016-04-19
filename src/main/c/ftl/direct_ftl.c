#include <unistd.h>
#include "ftl.h"

void direct_ftl_init(void) {
  return;
}

uint32_t direct_ftl_read(uint32_t lpn) {
  usleep(FLASH_PAGE_READ_DELAY);
  return lpn;
}

uint32_t direct_ftl_write(uint32_t lpn) {
  usleep(FLASH_PAGE_WRITE_DELAY);
  return lpn;
}

struct ftl direct_ftl = {
  direct_ftl_init,
  direct_ftl_read,
  direct_ftl_write
};
