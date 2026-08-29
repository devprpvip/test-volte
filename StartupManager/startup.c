#include "startup.h"
#include <shlobj.h>
#include <shlwapi.h>
#include <strsafe.h>

#pragma comment(lib, "shlwapi.lib")

StartupItem g_items[MAX_STARTUP_ITEMS];
int g_itemCount = 0;

const WCHAR* SourceToString(StartupSource src) {
    switch(src){
        case SRC_HKCU_RUN: return L"HKCU\\Run";
        case SRC_HKLM_RUN: return L"HKLM\\Run";
        case SRC_HKCU_RUNONCE: return L"HKCU\\RunOnce";
        case SRC_HKLM_RUNONCE: return L"HKLM\\RunOnce";
        case SRC_STARTUP_FOLDER_USER: return L"Startup (User)";
        case SRC_STARTUP_FOLDER_COMMON: return L"Startup (Common)";
        case SRC_DISABLED_BACKUP: return L"Đã tắt (Backup)";
        default: return L"Unknown";
    }
}

void GetStartupFolderPath(BOOL common, WCHAR* out, DWORD cch) {
    if (common) SHGetSpecialFolderPathW(NULL, out, CSIDL_COMMON_STARTUP, FALSE);
    else SHGetSpecialFolderPathW(NULL, out, CSIDL_STARTUP, FALSE);
}

static BOOL IsHiddenCommand(const WCHAR* cmd) {
    // check if wrapped with start /min
    if (!cmd) return FALSE;
    WCHAR lower[512];
    lstrcpynW(lower, cmd, 512);
    CharLowerW(lower);
    return wcsstr(lower, L"start /min") != NULL || wcsstr(lower, L"/min") != NULL;
}

static void AddItem(const WCHAR* name, const WCHAR* cmd, const WCHAR* loc, StartupSource src, BOOL enabled) {
    if (g_itemCount >= MAX_STARTUP_ITEMS) return;
    StartupItem* it = &g_items[g_itemCount++];
    StringCchCopyW(it->name, 128, name);
    StringCchCopyW(it->command, 512, cmd);
    StringCchCopyW(it->location, 256, loc);
    it->source = src;
    it->enabled = enabled;
    it->isHidden = IsHiddenCommand(cmd);
}

static void ScanRegistryKey(HKEY root, const WCHAR* subkey, StartupSource src) {
    HKEY hKey;
    if (RegOpenKeyExW(root, subkey, 0, KEY_READ | KEY_WOW64_64KEY, &hKey) != ERROR_SUCCESS) {
        if (RegOpenKeyExW(root, subkey, 0, KEY_READ, &hKey) != ERROR_SUCCESS) return;
    }
    WCHAR valueName[16383];
    BYTE data[2048];
    DWORD idx = 0;
    while (1) {
        DWORD cchName = 16383;
        DWORD cbData = sizeof(data);
        DWORD type = 0;
        LONG r = RegEnumValueW(hKey, idx, valueName, &cchName, NULL, &type, data, &cbData);
        if (r == ERROR_NO_MORE_ITEMS) break;
        if (r == ERROR_SUCCESS) {
            WCHAR cmdStr[512] = L"";
            if (type == REG_SZ || type == REG_EXPAND_SZ) {
                StringCchCopyW(cmdStr, 512, (WCHAR*)data);
            } else if (type == REG_DWORD) {
                StringCchPrintfW(cmdStr, 512, L"DWORD:%lu", *(DWORD*)data);
            } else {
                StringCchCopyW(cmdStr, 512, L"(binary)");
            }
            WCHAR loc[256];
            StringCchPrintfW(loc, 256, L"%s", SourceToString(src));
            AddItem(valueName, cmdStr, loc, src, TRUE);
        }
        idx++;
    }
    RegCloseKey(hKey);

    // Also check StartupApproved to mark disabled (Win8+)
    // HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\Run
    if (src == SRC_HKCU_RUN || src == SRC_HKLM_RUN) {
        HKEY hApproved;
        const WCHAR* approvedPath = L"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run";
        // For HKLM, approved is under same but HKLM
        if (root == HKEY_CURRENT_USER) {
            if (RegOpenKeyExW(root, approvedPath, 0, KEY_READ, &hApproved) == ERROR_SUCCESS) {
                // update last added items status if needed
                // we need to check each value we just added
                for (int i = g_itemCount - (int)idx; i < g_itemCount; i++) {
                    BYTE b[12];
                    DWORD cb = sizeof(b);
                    DWORD tp;
                    if (RegQueryValueExW(hApproved, g_items[i].name, NULL, &tp, b, &cb) == ERROR_SUCCESS) {
                        if (cb >= 1 && b[0] == 0x03) {
                            g_items[i].enabled = FALSE;
                            StringCchCopyW(g_items[i].location, 256, L"HKCU\\Run (Disabled by Windows)");
                        }
                    }
                }
                RegCloseKey(hApproved);
            }
        }
    }
}

static void ScanStartupFolder(BOOL common) {
    WCHAR path[MAX_PATH];
    GetStartupFolderPath(common, path, MAX_PATH);
    WCHAR search[MAX_PATH];
    StringCchPrintfW(search, MAX_PATH, L"%s\\*", path);
    WIN32_FIND_DATAW fd;
    HANDLE hFind = FindFirstFileW(search, &fd);
    if (hFind == INVALID_HANDLE_VALUE) return;
    do {
        if (fd.cFileName[0]==L'.') continue;
        if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) continue;
        WCHAR full[MAX_PATH];
        StringCchPrintfW(full, MAX_PATH, L"%s\\%s", path, fd.cFileName);
        WCHAR loc[256];
        StringCchCopyW(loc, 256, common ? L"Startup (Common)" : L"Startup (User)");
        StartupSource src = common ? SRC_STARTUP_FOLDER_COMMON : SRC_STARTUP_FOLDER_USER;
        AddItem(fd.cFileName, full, loc, src, TRUE);
    } while(FindNextFileW(hFind, &fd));
    FindClose(hFind);
}

static void ScanDisabledBackup(void) {
    HKEY hKey;
    const WCHAR* backupPath = L"Software\\StartupManager_Disabled";
    if (RegOpenKeyExW(HKEY_CURRENT_USER, backupPath, 0, KEY_READ, &hKey) != ERROR_SUCCESS) return;
    // enumerate subkeys? Actually we store values directly? Let's use subkey per source
    // For simplicity, we stored in HKCU\Software\StartupManager_Disabled\Run
    HKEY hRun;
    if (RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\Run", 0, KEY_READ, &hRun) == ERROR_SUCCESS) {
        WCHAR valueName[16383];
        BYTE data[2048];
        DWORD idx=0;
        while(1){
            DWORD cchName = 16383;
            DWORD cbData = sizeof(data);
            DWORD type;
            LONG r = RegEnumValueW(hRun, idx, valueName, &cchName, NULL, &type, data, &cbData);
            if(r==ERROR_NO_MORE_ITEMS) break;
            if(r==ERROR_SUCCESS){
                WCHAR cmdStr[512]=L"";
                if(type==REG_SZ||type==REG_EXPAND_SZ) StringCchCopyW(cmdStr,512,(WCHAR*)data);
                AddItem(valueName, cmdStr, L"Đã tắt (Backup HKCU\\Run)", SRC_DISABLED_BACKUP, FALSE);
            }
            idx++;
        }
        RegCloseKey(hRun);
    }
    // Check HKLM backup
    HKEY hRunLM;
    if (RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\RunLM", 0, KEY_READ, &hRunLM) == ERROR_SUCCESS) {
        WCHAR valueName[16383];
        BYTE data[2048];
        DWORD idx=0;
        while(1){
            DWORD cchName = 16383;
            DWORD cbData = sizeof(data);
            DWORD type;
            LONG r = RegEnumValueW(hRunLM, idx, valueName, &cchName, NULL, &type, data, &cbData);
            if(r==ERROR_NO_MORE_ITEMS) break;
            if(r==ERROR_SUCCESS){
                WCHAR cmdStr[512]=L"";
                if(type==REG_SZ||type==REG_EXPAND_SZ) StringCchCopyW(cmdStr,512,(WCHAR*)data);
                AddItem(valueName, cmdStr, L"Đã tắt (Backup HKLM\\Run)", SRC_DISABLED_BACKUP, FALSE);
            }
            idx++;
        }
        RegCloseKey(hRunLM);
    }
    RegCloseKey(hKey);
}

void ScanStartupItems(void) {
    g_itemCount = 0;
    ScanRegistryKey(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run", SRC_HKCU_RUN);
    ScanRegistryKey(HKEY_LOCAL_MACHINE, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run", SRC_HKLM_RUN);
    ScanRegistryKey(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\RunOnce", SRC_HKCU_RUNONCE);
    ScanRegistryKey(HKEY_LOCAL_MACHINE, L"Software\\Microsoft\\Windows\\CurrentVersion\\RunOnce", SRC_HKLM_RUNONCE);
    ScanStartupFolder(FALSE);
    ScanStartupFolder(TRUE);
    ScanDisabledBackup();
}

// Helpers for registry backup
static BOOL EnsureBackupKey(const WCHAR* sub) {
    HKEY h;
    DWORD disp;
    LONG r = RegCreateKeyExW(HKEY_CURRENT_USER, sub, 0, NULL, 0, KEY_WRITE, NULL, &h, &disp);
    if(r==ERROR_SUCCESS){ RegCloseKey(h); return TRUE;}
    return FALSE;
}

BOOL DisableStartupItem(int index) {
    if(index<0||index>=g_itemCount) return FALSE;
    StartupItem* it=&g_items[index];
    if(!it->enabled) return FALSE;

    if(it->source==SRC_HKCU_RUN){
        HKEY hSrc, hDst;
        if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run",0, KEY_READ|KEY_WRITE, &hSrc)!=ERROR_SUCCESS) return FALSE;
        EnsureBackupKey(L"Software\\StartupManager_Disabled");
        EnsureBackupKey(L"Software\\StartupManager_Disabled\\Run");
        if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\Run",0, KEY_WRITE, &hDst)!=ERROR_SUCCESS){RegCloseKey(hSrc); return FALSE;}
        // read value
        WCHAR data[1024]; DWORD cb=sizeof(data); DWORD type;
        if(RegQueryValueExW(hSrc, it->name, NULL, &type, (BYTE*)data, &cb)==ERROR_SUCCESS){
            RegSetValueExW(hDst, it->name, 0, type, (BYTE*)data, cb);
            RegDeleteValueW(hSrc, it->name);
            // also set StartupApproved to disabled if exists
            HKEY hAppr;
            if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run",0, KEY_WRITE, &hAppr)==ERROR_SUCCESS){
                BYTE b[12]={0x03,0,0,0, 0,0,0,0, 0,0,0,0};
                // we have deleted, so delete approved entry too? Keep for backup?
                RegDeleteValueW(hAppr, it->name);
                RegCloseKey(hAppr);
            }
        }
        RegCloseKey(hSrc); RegCloseKey(hDst);
        return TRUE;
    } else if(it->source==SRC_HKLM_RUN){
        // Need admin
        HKEY hSrc, hDst;
        if(RegOpenKeyExW(HKEY_LOCAL_MACHINE, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run",0, KEY_READ|KEY_WRITE|KEY_WOW64_64KEY, &hSrc)!=ERROR_SUCCESS){
            if(RegOpenKeyExW(HKEY_LOCAL_MACHINE, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run",0, KEY_READ|KEY_WRITE, &hSrc)!=ERROR_SUCCESS) return FALSE;
        }
        EnsureBackupKey(L"Software\\StartupManager_Disabled\\RunLM");
        if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\RunLM",0, KEY_WRITE, &hDst)!=ERROR_SUCCESS){RegCloseKey(hSrc); return FALSE;}
        WCHAR data[1024]; DWORD cb=sizeof(data); DWORD type;
        if(RegQueryValueExW(hSrc, it->name, NULL, &type, (BYTE*)data, &cb)==ERROR_SUCCESS){
            RegSetValueExW(hDst, it->name, 0, type, (BYTE*)data, cb);
            LONG del = RegDeleteValueW(hSrc, it->name);
            RegCloseKey(hSrc); RegCloseKey(hDst);
            return del==ERROR_SUCCESS;
        }
        RegCloseKey(hSrc); RegCloseKey(hDst);
        return FALSE;
    } else if(it->source==SRC_STARTUP_FOLDER_USER || it->source==SRC_STARTUP_FOLDER_COMMON){
        // Move file to disabled folder: %APPDATA%\StartupManager_Disabled
        WCHAR folder[MAX_PATH];
        SHGetSpecialFolderPathW(NULL, folder, CSIDL_APPDATA, FALSE);
        WCHAR disabledDir[MAX_PATH];
        StringCchPrintfW(disabledDir, MAX_PATH, L"%s\\StartupManager_Disabled", folder);
        CreateDirectoryW(disabledDir, NULL);
        WCHAR srcPath[MAX_PATH];
        StringCchCopyW(srcPath, MAX_PATH, it->command);
        WCHAR fileName[MAX_PATH];
        WCHAR* p = wcsrchr(srcPath, L'\\');
        if(p) StringCchCopyW(fileName, MAX_PATH, p+1);
        else StringCchCopyW(fileName, MAX_PATH, it->name);
        WCHAR dstPath[MAX_PATH];
        StringCchPrintfW(dstPath, MAX_PATH, L"%s\\%s", disabledDir, fileName);
        if(MoveFileW(srcPath, dstPath)){
            // save mapping for restore? store in registry backup for folder
            EnsureBackupKey(L"Software\\StartupManager_Disabled\\StartupFolder");
            HKEY hMap;
            if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\StartupFolder",0, KEY_WRITE, &hMap)==ERROR_SUCCESS){
                RegSetValueExW(hMap, fileName, 0, REG_SZ, (BYTE*)srcPath, (lstrlenW(srcPath)+1)*sizeof(WCHAR));
                RegCloseKey(hMap);
            }
            return TRUE;
        }
        return FALSE;
    }
    return FALSE;
}

BOOL EnableStartupItem(int index) {
    if(index<0||index>=g_itemCount) return FALSE;
    StartupItem* it=&g_items[index];
    if(it->enabled) return FALSE;
    if(it->source!=SRC_DISABLED_BACKUP) return FALSE;

    // Try to restore: check if backup exists in Run
    HKEY hBackup, hDst;
    WCHAR data[1024]; DWORD cb=sizeof(data); DWORD type;
    // Check Run backup
    if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\Run",0, KEY_READ, &hBackup)==ERROR_SUCCESS){
        cb=sizeof(data);
        if(RegQueryValueExW(hBackup, it->name, NULL, &type, (BYTE*)data, &cb)==ERROR_SUCCESS){
            if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run",0, KEY_WRITE, &hDst)==ERROR_SUCCESS){
                RegSetValueExW(hDst, it->name, 0, type, (BYTE*)data, cb);
                RegCloseKey(hDst);
                RegDeleteValueW(hBackup, it->name);
                RegCloseKey(hBackup);
                // Also clear approved
                HKEY hAppr;
                if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run",0, KEY_WRITE, &hAppr)==ERROR_SUCCESS){
                    BYTE b[12]={0x02,0,0,0,0,0,0,0,0,0,0,0};
                    RegSetValueExW(hAppr, it->name, 0, REG_BINARY, b, sizeof(b));
                    RegCloseKey(hAppr);
                }
                return TRUE;
            }
        }
        RegCloseKey(hBackup);
    }
    // Check RunLM backup
    if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\RunLM",0, KEY_READ, &hBackup)==ERROR_SUCCESS){
        cb=sizeof(data);
        if(RegQueryValueExW(hBackup, it->name, NULL, &type, (BYTE*)data, &cb)==ERROR_SUCCESS){
            HKEY hLM;
            if(RegOpenKeyExW(HKEY_LOCAL_MACHINE, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run",0, KEY_WRITE|KEY_WOW64_64KEY, &hLM)==ERROR_SUCCESS ||
               RegOpenKeyExW(HKEY_LOCAL_MACHINE, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run",0, KEY_WRITE, &hLM)==ERROR_SUCCESS){
                RegSetValueExW(hLM, it->name, 0, type, (BYTE*)data, cb);
                RegCloseKey(hLM);
                RegDeleteValueW(hBackup, it->name);
                RegCloseKey(hBackup);
                return TRUE;
            }
        }
        RegCloseKey(hBackup);
    }
    // Check StartupFolder backup
    HKEY hMap;
    if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\StartupFolder",0, KEY_READ, &hMap)==ERROR_SUCCESS){
        WCHAR srcPath[MAX_PATH]; DWORD cb2=sizeof(srcPath); DWORD tp;
        if(RegQueryValueExW(hMap, it->name, NULL, &tp, (BYTE*)srcPath, &cb2)==ERROR_SUCCESS){
            WCHAR folder[MAX_PATH];
            SHGetSpecialFolderPathW(NULL, folder, CSIDL_APPDATA, FALSE);
            WCHAR disabledPath[MAX_PATH];
            StringCchPrintfW(disabledPath, MAX_PATH, L"%s\\StartupManager_Disabled\\%s", folder, it->name);
            if(MoveFileW(disabledPath, srcPath)){
                RegDeleteValueW(hMap, it->name);
                RegCloseKey(hMap);
                return TRUE;
            }
        }
        RegCloseKey(hMap);
    }
    // fallback for startup folder file name matching
    WCHAR folder[MAX_PATH];
    SHGetSpecialFolderPathW(NULL, folder, CSIDL_APPDATA, FALSE);
    WCHAR disabledPath[MAX_PATH];
    StringCchPrintfW(disabledPath, MAX_PATH, L"%s\\StartupManager_Disabled\\%s", folder, it->name);
    WCHAR userStartup[MAX_PATH];
    GetStartupFolderPath(FALSE, userStartup, MAX_PATH);
    WCHAR dstPath[MAX_PATH];
    StringCchPrintfW(dstPath, MAX_PATH, L"%s\\%s", userStartup, it->name);
    if(MoveFileW(disabledPath, dstPath)) return TRUE;

    return FALSE;
}

BOOL DeleteStartupItem(int index) {
    if(index<0||index>=g_itemCount) return FALSE;
    StartupItem* it=&g_items[index];
    if(it->source==SRC_HKCU_RUN){
        HKEY h;
        if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run",0, KEY_WRITE, &h)==ERROR_SUCCESS){
            LONG r=RegDeleteValueW(h, it->name);
            RegCloseKey(h);
            return r==ERROR_SUCCESS;
        }
    } else if(it->source==SRC_HKLM_RUN){
        HKEY h;
        if(RegOpenKeyExW(HKEY_LOCAL_MACHINE, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run",0, KEY_WRITE|KEY_WOW64_64KEY, &h)==ERROR_SUCCESS ||
           RegOpenKeyExW(HKEY_LOCAL_MACHINE, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run",0, KEY_WRITE, &h)==ERROR_SUCCESS){
            LONG r=RegDeleteValueW(h, it->name);
            RegCloseKey(h);
            return r==ERROR_SUCCESS;
        }
    } else if(it->source==SRC_STARTUP_FOLDER_USER || it->source==SRC_STARTUP_FOLDER_COMMON){
        return DeleteFileW(it->command);
    } else if(it->source==SRC_DISABLED_BACKUP){
        // delete from backup
        HKEY h;
        if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\Run",0, KEY_WRITE, &h)==ERROR_SUCCESS){
            if(RegDeleteValueW(h, it->name)==ERROR_SUCCESS){RegCloseKey(h); return TRUE;}
            RegCloseKey(h);
        }
        if(RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\StartupManager_Disabled\\RunLM",0, KEY_WRITE, &h)==ERROR_SUCCESS){
            if(RegDeleteValueW(h, it->name)==ERROR_SUCCESS){RegCloseKey(h); return TRUE;}
            RegCloseKey(h);
        }
        // try file
        WCHAR folder[MAX_PATH];
        SHGetSpecialFolderPathW(NULL, folder, CSIDL_APPDATA, FALSE);
        WCHAR p[MAX_PATH];
        StringCchPrintfW(p, MAX_PATH, L"%s\\StartupManager_Disabled\\%s", folder, it->name);
        return DeleteFileW(p);
    }
    return FALSE;
}

BOOL SetHiddenMode(int index, BOOL hidden) {
    if(index<0||index>=g_itemCount) return FALSE;
    StartupItem* it=&g_items[index];
    HKEY hKey = NULL;
    const WCHAR* regPath = NULL;
    HKEY root = 0;
    if(it->source==SRC_HKCU_RUN){ root=HKEY_CURRENT_USER; regPath=L"Software\\Microsoft\\Windows\\CurrentVersion\\Run"; }
    else if(it->source==SRC_HKLM_RUN){ root=HKEY_LOCAL_MACHINE; regPath=L"Software\\Microsoft\\Windows\\CurrentVersion\\Run"; }
    else return FALSE;

    if(RegOpenKeyExW(root, regPath,0, KEY_READ|KEY_WRITE | (root==HKEY_LOCAL_MACHINE?KEY_WOW64_64KEY:0), &hKey)!=ERROR_SUCCESS){
        if(RegOpenKeyExW(root, regPath,0, KEY_READ|KEY_WRITE, &hKey)!=ERROR_SUCCESS) return FALSE;
    }
    WCHAR cur[512]; DWORD cb=sizeof(cur); DWORD type;
    if(RegQueryValueExW(hKey, it->name, NULL, &type, (BYTE*)cur, &cb)!=ERROR_SUCCESS){RegCloseKey(hKey); return FALSE;}

    WCHAR newCmd[512];
    WCHAR lower[512]; lstrcpynW(lower, cur, 512); CharLowerW(lower);
    BOOL alreadyHidden = wcsstr(lower, L"start /min")!=NULL;

    if(hidden && !alreadyHidden){
        // Check if command is quoted
        // Wrap: cmd.exe /c start /min "" "original"
        // Remove outer quotes for wrapping? Keep as is inside inner quotes
        // If cmd contains .exe path with spaces, we need to preserve
        StringCchPrintfW(newCmd, 512, L"cmd.exe /c start /min \"\" %s", cur);
    } else if(!hidden && alreadyHidden){
        // Try to unwrap: find first occurrence of "" and take after
        WCHAR* p = wcsstr(cur, L"\"\"");
        if(p){
            p+=2;
            while(*p==L' '||*p==L'"') p++;
            // Actually after "" there is space then original command may be quoted
            // Simpler: search for start /min "" pattern case-insensitive using lower
            // Use original cur to find
            WCHAR* pos = wcsstr(lower, L"start /min");
            if(pos){
                int offset = (int)(pos - lower);
                WCHAR* origPos = cur + offset + lstrlenW(L"start /min");
                while(*origPos==L' '||*origPos==L'"') origPos++;
                StringCchCopyW(newCmd, 512, origPos);
                // remove trailing quote if present
                size_t len=lstrlenW(newCmd);
                if(len>0 && newCmd[len-1]==L'"') newCmd[len-1]=0;
            } else {
                StringCchCopyW(newCmd, 512, cur);
            }
        } else {
            // fallback: remove cmd.exe /c start /min "" prefix
            WCHAR* pos = wcsstr(lower, L"cmd.exe /c start /min");
            if(pos){
                int offset = (int)(pos - lower) + lstrlenW(L"cmd.exe /c start /min");
                WCHAR* origPos = cur + offset;
                while(*origPos==L' '||*origPos==L'"') origPos++;
                StringCchCopyW(newCmd, 512, origPos);
                size_t len=lstrlenW(newCmd);
                if(len>0 && newCmd[len-1]==L'"') newCmd[len-1]=0;
            } else {
                StringCchCopyW(newCmd, 512, cur);
            }
        }
    } else {
        RegCloseKey(hKey);
        return FALSE; // no change
    }

    LONG r = RegSetValueExW(hKey, it->name, 0, REG_SZ, (BYTE*)newCmd, (lstrlenW(newCmd)+1)*sizeof(WCHAR));
    RegCloseKey(hKey);
    if(r==ERROR_SUCCESS){
        StringCchCopyW(it->command, 512, newCmd);
        it->isHidden = hidden;
        return TRUE;
    }
    return FALSE;
}

BOOL OpenItemLocation(int index){
    if(index<0||index>=g_itemCount) return FALSE;
    StartupItem* it=&g_items[index];
    WCHAR path[MAX_PATH];
    if(it->source==SRC_STARTUP_FOLDER_USER || it->source==SRC_STARTUP_FOLDER_COMMON){
        WCHAR dir[MAX_PATH];
        StringCchCopyW(dir, MAX_PATH, it->command);
        WCHAR* p=wcsrchr(dir, L'\\');
        if(p) *p=0;
        ShellExecuteW(NULL, L"open", dir, NULL, NULL, SW_SHOW);
        return TRUE;
    } else {
        // extract exe path from command
        WCHAR cmd[512]; StringCchCopyW(cmd, 512, it->command);
        // remove cmd.exe /c start /min "" wrapper
        WCHAR lower[512]; lstrcpynW(lower, cmd, 512); CharLowerW(lower);
        WCHAR* unwrapped = cmd;
        WCHAR* pos = wcsstr(lower, L"start /min");
        if(pos){
            int off = (int)(pos - lower) + lstrlenW(L"start /min");
            unwrapped = cmd + off;
            while(*unwrapped==L' '||*unwrapped==L'"') unwrapped++;
        }
        // Now extract first quoted or first token
        WCHAR exePath[MAX_PATH]=L"";
        if(unwrapped[0]==L'"'){
            WCHAR* end = wcschr(unwrapped+1, L'"');
            if(end){ size_t len=end-(unwrapped+1); wcsncpy(exePath, unwrapped+1, len); exePath[len]=0; }
            else StringCchCopyW(exePath, MAX_PATH, unwrapped+1);
        } else {
            // token until space
            WCHAR* end = wcschr(unwrapped, L' ');
            if(end){ size_t len=end-unwrapped; wcsncpy(exePath, unwrapped, len); exePath[len]=0; }
            else StringCchCopyW(exePath, MAX_PATH, unwrapped);
        }
        // remove args
        // exePath may contain .exe + args, try to find .exe
        WCHAR* exeEnd = wcsstr(exePath, L".exe");
        if(exeEnd) *(exeEnd+4)=0;
        WCHAR dir[MAX_PATH];
        StringCchCopyW(dir, MAX_PATH, exePath);
        WCHAR* p=wcsrchr(dir, L'\\');
        if(p) *p=0;
        else StringCchCopyW(dir, MAX_PATH, exePath);
        if(GetFileAttributesW(exePath)!=INVALID_FILE_ATTRIBUTES || GetFileAttributesW(dir)!=INVALID_FILE_ATTRIBUTES){
            if(GetFileAttributesW(dir)!=INVALID_FILE_ATTRIBUTES){
                ShellExecuteW(NULL, L"open", dir, NULL, NULL, SW_SHOW);
            } else {
                ShellExecuteW(NULL, L"open", L"explorer.exe", dir, NULL, SW_SHOW);
            }
            return TRUE;
        }
        // fallback: open registry? no
        MessageBoxW(NULL, exePath, L"Đường dẫn", MB_OK);
        return FALSE;
    }
}
