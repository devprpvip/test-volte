CC = x86_64-w64-mingw32-gcc
WINDRES = x86_64-w64-mingw32-windres
CFLAGS = -Os -s -municode -mwindows -Wall -Wextra -std=c11 -DUNICODE -D_UNICODE
LDFLAGS = -lcomctl32 -lshlwapi -lshell32
TARGET = StartupManager.exe
OBJS = main.o startup.o resource.o

all: $(TARGET) size

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "Build OK: $@"
	@ls -lh $@

main.o: main.c startup.h
	$(CC) $(CFLAGS) -c main.c -o main.o

startup.o: startup.c startup.h
	$(CC) $(CFLAGS) -c startup.c -o startup.o

resource.o: resource.rc app.manifest
	$(WINDRES) resource.rc -o resource.o

size:
	@echo "=== Kich thuoc file ==="
	@ls -lh $(TARGET) | awk '{print $$9, $$5}'
	@du -h $(TARGET)
	@echo "Muc tieu: < 5MB (5120KB). Hien tai:"
	@stat -c %s $(TARGET) | awk '{printf "%.2f KB (%.2f MB)\n", $$1/1024, $$1/1024/1024}'

clean:
	rm -f *.o *.exe

# Build 32-bit
32:
	i686-w64-mingw32-gcc $(CFLAGS) -o StartupManager-x86.exe main.c startup.c resource.rc $(LDFLAGS)
	ls -lh StartupManager-x86.exe

upx:
	upx --best --lzma $(TARGET) || echo "Cai upx de nen them: apt install upx-ucl"
	ls -lh $(TARGET)

.PHONY: all clean size 32 upx
