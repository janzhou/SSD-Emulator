
obj-m		:= ssd_blkdev.o 
KERN_SRC 	:=/home/cass/Downloads/linux-4.3.3/
PWD			:= $(shell pwd)

modules:
	make -C $(KERN_SRC) M=$(PWD) modules

install:
	make -C $(KERN_SRC) M=$(PWD) modules_install
	depmod -a

clean:
	make -C $(KERN_SRC) M=$(PWD) clean
