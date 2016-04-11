obj-m		:= ssd_blkdev.o 
KERN_SRC	:= /lib/modules/$(shell uname -r)/build/
USER_FTL_SRC:= ../USER_SRC/ftl
PWD			:= $(shell pwd)

modules:
	make -C $(KERN_SRC) M=$(PWD) modules
	make -C $(USER_FTL_SRC)

install:
	make -C $(KERN_SRC) M=$(PWD) modules_install
	depmod -a

clean:
	make -C $(KERN_SRC) M=$(PWD) clean
	make -C $(USER_FTL_SRC) clean
