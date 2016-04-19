#include <unistd.h>
#include "ftl.h"
#include "list.h"

typedef struct {
	uint32_t lpn[SSD_NR_PAGES_PER_BLOCK];
	uint32_t id;
	uint32_t clean;
	uint32_t dirty;
	struct list_head list;
} dftl_block;

dftl_block dftl_blocks[SSD_NR_BLOCKS];

LIST_HEAD(free_block);
LIST_HEAD(gc_block);

struct {
	uint32_t psn;
	uint32_t block;
	bool cached;
	bool dirty;
} dftl_table[SSD_MAP_TABLE_SIZE];

struct {
	uint32_t lpn;
} dftl_cache[CACHE_SIZE];

uint32_t head = 0;
uint32_t tail = 0;

uint32_t next(uint32_t point)
{
	uint32_t next = point + 1;

	if (next >= CACHE_SIZE) {
		next = 0;
	}

	return next;
}

void cache(uint32_t lpn)
{
	uint32_t next_head = next(head);

	dftl_table[lpn].cached = true;

	if (next_head == tail) {
		uint32_t evict_lpn = dftl_cache[tail].lpn;
		dftl_table[evict_lpn].cached = false;
		if (dftl_table[evict_lpn].dirty) {
			usleep(FLASH_PAGE_READ_DELAY);
			usleep(FLASH_PAGE_WRITE_DELAY);
			dftl_table[evict_lpn].dirty = false;
		}
		tail = next(tail);
	}

	dftl_cache[head].lpn = lpn;
	head = next_head;
}

void dftl_init(void)
{
	uint32_t i = 0;

	for (i = 0; i < SSD_MAP_TABLE_SIZE; i++) {
		dftl_table[i].psn = SSD_MAP_TABLE_SIZE;
		dftl_table[i].cached = false;
		dftl_table[i].dirty = false;
	}

	for (i = 0; i < SSD_NR_BLOCKS; i++) {
		list_add_tail(&(dftl_blocks[i].list), &(free_block));
		dftl_blocks[i].id = i;
		dftl_blocks[i].clean = SSD_NR_PAGES_PER_BLOCK;
		dftl_blocks[i].dirty = 0;
	}

	return;
}

uint32_t dftl_read(uint32_t lpn)
{
//	if (!dftl_table[lpn].cached) {
//		usleep(FLASH_PAGE_READ_DELAY);
//		cache(lpn);
//	}

	return dftl_table[lpn].psn;
}

void dftl_gc(void)
{

}

dftl_block * dftl_free_block(void)
{
	dftl_block *block = NULL;

	while (block == NULL) {
		if (list_empty(&free_block)) {
			dftl_gc();
		}

		if (!list_empty(&free_block)) {
			block = list_first_entry(&free_block, dftl_block, list);
		}

		if (block != NULL && block->clean == 0) {
			list_del(&block->list);
			block = NULL;
		}
	}
	return block;
}

uint32_t dftl_write(uint32_t lpn)
{
	uint32_t psn = 0;
	dftl_block * block;

//	if (!dftl_table[lpn].cached) {
//		usleep(FLASH_PAGE_READ_DELAY);
//		cache(lpn);
//	}

	if (!(dftl_table[lpn].psn >= SSD_MAP_TABLE_SIZE)) {
		int block_id = dftl_table[lpn].psn / SSD_NR_PAGES_PER_BLOCK;
		dftl_blocks[block_id].dirty += 1;
	}

	block = dftl_free_block();

	psn = block->id * SSD_NR_PAGES_PER_BLOCK + SSD_NR_PAGES_PER_BLOCK - block->clean;
	block->clean -= 1;
	dftl_table[lpn].psn = psn;
	dftl_table[lpn].block = block->id;
	dftl_table[lpn].cached = true;
	dftl_table[lpn].dirty = true;

	return psn;
}

struct ftl dftl = { dftl_init, dftl_read, dftl_write };
