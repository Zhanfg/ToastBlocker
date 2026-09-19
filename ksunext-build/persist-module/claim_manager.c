#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/syscall.h>
#include <unistd.h>

#define KSU_INSTALL_MAGIC1 0xDEADBEEFUL
#define CHANGE_MANAGER_UID 10006UL

int main(int argc, char **argv) {
    if (argc != 2) {
        fprintf(stderr, "usage: %s <appid>\n", argv[0]);
        return 2;
    }

    char *end = NULL;
    unsigned long appid = strtoul(argv[1], &end, 10);
    if (!end || *end != '\0' || appid <= 10000 || appid >= 20000) {
        fprintf(stderr, "invalid appid: %s\n", argv[1]);
        return 3;
    }

    uintptr_t ack = 0;
    uintptr_t expected = (uintptr_t)&ack;

    errno = 0;
    (void)syscall(__NR_reboot,
                  KSU_INSTALL_MAGIC1,
                  CHANGE_MANAGER_UID,
                  appid,
                  &ack);

    if (ack != expected) {
        fprintf(stderr,
                "CHANGE_MANAGER_UID not acknowledged (appid=%lu errno=%d ack=%#lx expected=%#lx)\n",
                appid, errno, (unsigned long)ack, (unsigned long)expected);
        return 4;
    }

    printf("manager appid -> %lu\n", appid);
    return 0;
}
