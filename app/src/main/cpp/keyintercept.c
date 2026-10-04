/*
 * keyintercept —— 物理按键独占抓取器（独立可执行文件）
 * ---------------------------------------------------------------------------
 * 来源：0.6.x 发布包里 assets/keyintercept 的二进制（aarch64、未 strip）。
 *       本文件按其反汇编 + 符号表 + rodata 字符串还原，并用同一套 NDK 编译
 *       比对过（字符串/格式串一致、.text 仅差 32 字节），行为等价。
 *
 * 作用：以 shell/root 身份独占（EVIOCGRAB）实体按键设备，把 EV_KEY 事件
 *       经 TCP 发给 127.0.0.1:<port>，由 App 的 KeyInterceptService 处理。
 *       应用进程无法执行私有目录里的文件，所以由 Shizuku 把本文件拷到
 *       /data/local/tmp/keyintercept 再执行。
 *
 * 协议：每包 5 字节 —— [0x00, 0x00, code_lo, code_hi, pressed?1:0]
 *
 * 用法：keyintercept <port>
 *
 * 注：libkeyintercept.so 只是打包用的名字，本体是可执行文件（Android 只会把
 *     jniLibs/CMake 产物里的 *.so 放进 APK 的 lib/<abi>/，所以借这个后缀）。
 */
#include <arpa/inet.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/input.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/epoll.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <unistd.h>
#include <android/log.h>

#define TAG        "KeyIntercept"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define PORT_MIN    1
#define PORT_MAX    65535
#define SERVER_IP   "127.0.0.1"
#define INPUT_DIR   "/dev/input"
#define CONN_RETRY  10      /* 连接 App 的重试次数（每次间隔 1s） */
#define MAX_EVENTS  16
#define READ_EVENTS 64      /* 单次 read 最多容纳的 input_event 个数 */

/* 全局：与 App 的 TCP 连接、epoll 实例 */
static int sockfd = -1;
static int epoll_fd = -1;

int main(int argc, char **argv) {
    if (argc != 2) {
        LOGE("Usage: %s <port>", argv[0]);
        return 1;
    }

    int port = atoi(argv[1]);
    if (port < PORT_MIN || port > PORT_MAX) {
        LOGE("Invalid port");
        return 1;
    }

    /* ---- 1. 连接本机 App 的 TCP 服务端（重试 CONN_RETRY 次） ---- */
    struct sockaddr_in addr;
    int retry = CONN_RETRY;
    do {
        int fd = socket(AF_INET, SOCK_STREAM, 0);
        if (fd < 0) {
            LOGE("socket failed: %s", strerror(errno));
            sockfd = -1;
            sleep(1);
            continue;
        }
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_port   = htons((uint16_t) port);
        inet_pton(AF_INET, SERVER_IP, &addr.sin_addr);

        if (connect(fd, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
            LOGE("connect failed: %s", strerror(errno));
            close(fd);
            sockfd = -1;
            sleep(1);
            continue;
        }
        /* 非阻塞：send 遇到 EAGAIN 直接丢弃该事件，不阻塞事件循环 */
        int flags = fcntl(fd, F_GETFL, 0);
        if (flags >= 0) fcntl(fd, F_SETFL, flags | O_NONBLOCK);
        sockfd = fd;
        break;
    } while (--retry);

    if (sockfd < 0) {
        LOGE("Could not connect to server");
        return 1;
    }

    /* ---- 2. 建 epoll，遍历 /dev/input 抢下按键类设备 ---- */
    epoll_fd = epoll_create(64);
    if (epoll_fd < 0) {
        LOGE("epoll_create: %s", strerror(errno));
        close(sockfd);
        return 1;
    }

    DIR *dir = opendir(INPUT_DIR);
    if (!dir) {
        LOGE("opendir %s failed: %s", INPUT_DIR, strerror(errno));
        /* 原程序此处不返回，继续进入事件循环 */
    } else {
        struct dirent *de;
        char path[4096];
        while ((de = readdir(dir)) != NULL) {
            if (strncmp(de->d_name, "event", 5) != 0) continue;

            snprintf(path, sizeof(path), "%s/%s", INPUT_DIR, de->d_name);
            int fd = open(path, O_RDONLY | O_NONBLOCK);
            if (fd < 0) {
                LOGE("open %s: %s", path, strerror(errno));
                continue;
            }

            /* 能力位：要有 EV_KEY，且不能有 EV_ABS（= 键盘/按键，排除触摸屏） */
            uint64_t evbits[8] = {0};
            if (ioctl(fd, EVIOCGBIT(0, sizeof(evbits)), evbits) < 0) {
                LOGE("ioctl EVIOCGBIT(0) %s: %s", path, strerror(errno));
                close(fd);
                continue;
            }
            if (!(evbits[0] & (1ULL << EV_KEY)) || (evbits[0] & (1ULL << EV_ABS))) {
                close(fd);
                continue;
            }

            /* 独占设备：系统与其它进程再也收不到这些按键 */
            if (ioctl(fd, EVIOCGRAB, 1) < 0) {
                LOGE("EVIOCGRAB %s failed: %s", path, strerror(errno));
                close(fd);
                continue;
            }

            struct epoll_event ev;
            ev.events  = EPOLLIN | EPOLLET;   /* 边沿触发 */
            ev.data.fd = fd;
            if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, fd, &ev) < 0) {
                LOGE("epoll_ctl add %s: %s", path, strerror(errno));
                ioctl(fd, EVIOCGRAB, 0);      /* 失败则释放独占 */
                close(fd);
            }
        }
        closedir(dir);
    }

    /* ---- 3. 事件循环 ---- */
    struct epoll_event events[MAX_EVENTS];
    for (;;) {
        int n = epoll_wait(epoll_fd, events, MAX_EVENTS, -1);
        if (n < 0) {
            if (errno == EINTR) continue;     /* 被信号打断，继续等 */
            LOGE("epoll_wait: %s", strerror(errno));
            break;
        }

        for (int i = 0; i < n; i++) {
            if (!(events[i].events & EPOLLIN)) continue;
            int fd = events[i].data.fd;

            struct input_event iev[READ_EVENTS];
            ssize_t r = read(fd, iev, sizeof(iev));
            if (r < 0) {
                if (errno == EAGAIN) continue;
                LOGE("read error on fd %d: %s", fd, strerror(errno));
                epoll_ctl(epoll_fd, EPOLL_CTL_DEL, fd, NULL);
                close(fd);
                continue;
            }

            int count = (int) (r / sizeof(struct input_event));
            for (int k = 0; k < count; k++) {
                if (iev[k].type != EV_KEY) continue;

                /* 5 字节包：[0, 0, code_lo, code_hi, pressed] */
                uint8_t pkt[5];
                pkt[0] = 0;
                pkt[1] = 0;
                pkt[2] = iev[k].code & 0xFF;
                pkt[3] = (iev[k].code >> 8) & 0xFF;
                pkt[4] = iev[k].value ? 1 : 0;

                if (send(sockfd, pkt, sizeof(pkt), 0) < 0 && errno != EAGAIN) {
                    LOGE("send error: %s", strerror(errno));
                }
            }
        }
    }

    if (sockfd >= 0) close(sockfd);
    if (epoll_fd >= 0) close(epoll_fd);
    return 0;
}
