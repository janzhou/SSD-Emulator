FTLs := dftl.o direct_ftl.o user_ftl.o
CFLAGS := -g

ftl_main: $(FTLs)
	gcc -o ftl_main ftl_main.c $(FTLs) $(CFLAGS)
	
dftl.o:
	gcc -c dftl.c $(CFLAGS)
	
direct_ftl.o:
	gcc -c direct_ftl.c $(CFLAGS)
	
user_ftl.o:
	gcc -c user_ftl.c $(CFLAGS)	
	
clean:
	rm -f *.o ftl_main