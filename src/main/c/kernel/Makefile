
obj-m		:= ssd_blkdev.o
KERN_SRC	:= /lib/modules/$(shell uname -r)/build/
PWD			:= $(shell pwd)

-include Makefile.conf

modules:
	make -C $(KERN_SRC) M=$(PWD) modules	

install:
	make -C $(KERN_SRC) M=$(PWD) modules_install
	depmod -a

clean:
	make -C $(KERN_SRC) M=$(PWD) clean
