#include "ftl.h"
#include <unistd.h>

void user_ftl_init(void) {
  return;
}

uint32_t user_ftl_read(uint32_t lpn) {
  usleep(FLASH_PAGE_READ_DELAY);
  return lpn;
}

uint32_t user_ftl_write(uint32_t lpn) {
  usleep(FLASH_PAGE_WRITE_DELAY);
  return lpn;
}

struct ftl user_ftl = {
  user_ftl_init,
  user_ftl_read,
  user_ftl_write
};
