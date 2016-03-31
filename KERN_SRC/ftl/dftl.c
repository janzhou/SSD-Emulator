#include "ftl.h"

#include <linux/delay.h>
#include <linux/list.h>

typedef struct {
  u32 lpn[PAGE_PER_BLOCK];
  u32 id;
  u32 clean;
  u32 dirty;
  struct list_head list;
} dftl_block;
dftl_block dftl_blocks[BLOCK_NUMBER];

LIST_HEAD(free_block);
LIST_HEAD(gc_block);

struct {
  u32 ppn;
  u32 block;
  bool cached;
  bool dirty;
} dftl_table[TABLE_SIZE];

struct {
  u32 lpn;
} dftl_cache[CACHE_SIZE];

u32 head = 0;
u32 tail = 0;

u32 next(u32 point) {
  u32 next = point + 1;
  if ( next >= CACHE_SIZE ) {
    next = 0;
  }
  return next;
}

void cache(u32 lpn) {
  u32 next_head = next(head);

  dftl_table[lpn].cached = true;

  if ( next_head == tail ) {
    u32 evict_lpn = dftl_cache[tail].lpn;
    dftl_table[evict_lpn].cached = false;
    if ( dftl_table[evict_lpn].dirty ) {
      udelay(FLASH_PAGE_READ_DELAY);
      udelay(FLASH_PAGE_WRITE_DELAY);
      dftl_table[evict_lpn].dirty = false;
    }
    tail = next(tail);
  }

  dftl_cache[head].lpn = lpn;
  head = next_head;
}

void dftl_init(void) {
  int i = 0;
  for(i = 0; i < TABLE_SIZE; i++) {
    dftl_table[i].ppn = PAGE_NUMBER;
    dftl_table[i].cached = false;
    dftl_table[i].dirty = false;
  }

  for(i = 0; i < BLOCK_NUMBER; i++) {
    list_add(&(dftl_blocks[i].list), &(free_block));
    dftl_blocks[i].id = i;
    dftl_blocks[i].clean = PAGE_PER_BLOCK;
    dftl_blocks[i].dirty = 0;
  }
  return;
}

u32 dftl_read(u32 lpn) {
  if ( !dftl_table[lpn].cached ) {
    udelay(FLASH_PAGE_READ_DELAY);
    cache(lpn);
  }

  return dftl_table[lpn].ppn;
}

void dftl_gc(void) {

}

dftl_block * dftl_free_block(void) {
  dftl_block * block = NULL;
  while(block == NULL){
    if( list_empty(&free_block) ) {
      dftl_gc();
    }

    if( !list_empty(&free_block) ){
      block = list_first_entry(&free_block, dftl_block, list);
    }
    if( block != NULL && block->clean == 0 ) {
      list_del(&block->list);
      block = NULL;
    }
  }
  return block;
}

u32 dftl_write(u32 lpn) {
  u32 ppn = 0;
  dftl_block * block;

  if( !dftl_table[lpn].cached ) {
    udelay(FLASH_PAGE_READ_DELAY);
    cache(lpn);
  }

  if( !( dftl_table[lpn].ppn >= PAGE_NUMBER ) )  {
    int block_id = dftl_table[lpn].ppn / PAGE_PER_BLOCK;
    dftl_blocks[block_id].dirty += 1;
  }

  block = dftl_free_block();

  ppn = block->id * PAGE_PER_BLOCK + PAGE_PER_BLOCK - block->clean;
  block->clean -= 1;
  dftl_table[lpn].ppn = ppn;
  dftl_table[lpn].block = block->id;
  dftl_table[lpn].cached = true;
  dftl_table[lpn].dirty = true;

  return ppn;
}

struct ftl dftl = {
  dftl_init,
  dftl_read,
  dftl_write
};
