obj-m += ssd.o
ssd-objs := ssd_blkdev.o ftl/direct_ftl.o ftl/dftl.o ftl/user_ftl.o
KERN_SRC	:= /lib/modules/$(shell uname -r)/build/
PWD			:= $(shell pwd)

modules:
	make -C $(KERN_SRC) M=$(PWD) modules

install:
	make -C $(KERN_SRC) M=$(PWD) modules_install
	depmod -a

clean:
	make -C $(KERN_SRC) M=$(PWD) clean
