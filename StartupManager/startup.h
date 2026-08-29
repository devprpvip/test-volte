#pragma once
#include <windows.h>

#define MAX_STARTUP_ITEMS 256

typedef enum {
    SRC_HKCU_RUN = 0,
    SRC_HKLM_RUN,
    SRC_HKCU_RUNONCE,
    SRC_HKLM_RUNONCE,
    SRC_STARTUP_FOLDER_USER,
    SRC_STARTUP_FOLDER_COMMON,
    SRC_DISABLED_BACKUP
} StartupSource;

typedef struct {
    WCHAR name[128];
    WCHAR command[512];
    WCHAR location[256];
    StartupSource source;
    BOOL enabled;
    BOOL isHidden; // wrapped with start /min
} StartupItem;

extern StartupItem g_items[MAX_STARTUP_ITEMS];
extern int g_itemCount;

void ScanStartupItems(void);
BOOL DisableStartupItem(int index);
BOOL EnableStartupItem(int index);
BOOL DeleteStartupItem(int index);
BOOL SetHiddenMode(int index, BOOL hidden);
BOOL OpenItemLocation(int index);
const WCHAR* SourceToString(StartupSource src);
void GetStartupFolderPath(BOOL common, WCHAR* out, DWORD cch);
