FTLs := dftl.o direct_ftl.o user_ftl.o
CFLAGS := -g
ALL := variables ftl_main

all: $(ALL)

variables: variables.o
	gcc -o variables variables.o $(CFLAGS)
	
ftl_main: ftl_main.o $(FTLs)
	gcc -o ftl_main ftl_main.o $(FTLs) $(CFLAGS)
	
ftl_main.o: ftl_main.c ftl.h
	gcc -c ftl_main.c $(CFLAGS)
	
dftl.o: dftl.c ftl.h
	gcc -c dftl.c $(CFLAGS)
	
direct_ftl.o: direct_ftl.c ftl.h
	gcc -c direct_ftl.c $(CFLAGS)
	
user_ftl.o: user_ftl.c ftl.h
	gcc -c user_ftl.c $(CFLAGS)	
	
clean:
	rm -f *.o $(ALL)
