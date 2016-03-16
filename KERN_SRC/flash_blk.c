/*
===============================================================================
Driver Name		:		flash_blk
Author			:		RAGHAVENDRA
License			:		GPL
Description		:		LINUX DEVICE DRIVER PROJECT
===============================================================================
*/

#include <linux/module.h>
#include <linux/kernel.h>

MODULE_LICENSE("GPL");
MODULE_AUTHOR("RAGHAVENDRA");


static int __init flash_blk_init(void)
{
	return 0;
}

static void __exit flash_blk_exit(void)
{	

}

module_init(flash_blk_init);
module_exit(flash_blk_exit);

