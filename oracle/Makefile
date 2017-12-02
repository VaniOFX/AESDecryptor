CC           = gcc
CFLAGS       = -Wall

oracle: aes.o oracle.o
	$(CC) $(CFLAGS) aes.o oracle.o -o oracle

oracle.o: oracle.c
	$(CC) $(CFLAGS) -c oracle.c -o oracle.o

aes.o : aes.h aes.c
	$(CC) $(CFLAGS) -c aes.c -o aes.o

clean: 
	rm *.o
