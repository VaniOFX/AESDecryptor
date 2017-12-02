#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include "aes.h"

int main(int argc, char* argv[]) {
	char helpstr[] = "Use: oracle <filename> (see a2q.pdf)\nFile size should be 16n, where n >= 2\n";
	if (argc < 2) {
		printf("%s\n", helpstr);
		return -1;
	}

	//read input file to get iv and ciphertext
	FILE * f;
	f = fopen(argv[1], "rb");
	int len = 0;
	int c;
	if (f != NULL) {
		do {
			c = getc(f);
			len += 1;
		} while (c != EOF);
	}
	else {
		printf("%s\n", helpstr);
		return -1;
	}
	len -= 1;
	fclose(f);
	if (len < 32 || len % 16 != 0) {
		printf("%s\n", helpstr);
		return -1;
	}
	uint8_t iv[16];
	int textlen = len-16;
	uint8_t* ciphertext = (uint8_t*) malloc(textlen);
	len = 0;
	f = fopen(argv[1], "r");
	do {
		c = getc(f);
		if (len < 16) iv[len] = (uint8_t) c;
		else ciphertext[len-16] = (uint8_t) c;
		len += 1;

	} while (c != EOF);
	len -= 1;

	/*for (int i = 0; i < 16; i++) {
		printf("%d,", iv[i]);
	}
	printf("\n");
	for (int i = 0; i < len-16; i++) {
		printf("%d,", ciphertext[i]);
	}
	printf("\n");*/

	//hardcoded key:
	char key_string[17] = "COMP3632 testkey";
	uint8_t key[16];
	for (int i = 0; i < 16; i++) {
		key[i] = (uint8_t) key_string[i];
	}
	uint8_t* plaintext = (uint8_t*) malloc(textlen);
	AES_CBC_decrypt_buffer(plaintext, ciphertext, textlen, key, iv);
	int exp_padnum = (int)plaintext[textlen-1];
	int ok = 1;
	if (exp_padnum < 1 || exp_padnum > 16) ok = 0;
	for (int i = 0; i < exp_padnum; i++) {
		if (plaintext[textlen-1-i] != exp_padnum) ok = 0;
	}
	printf("%d", ok);
	/*for (int i = 0; i < len-16; i++) {
		printf("%c,", (char)plaintext[i]);
	}
	printf("\n");*/


	return 0;

}
