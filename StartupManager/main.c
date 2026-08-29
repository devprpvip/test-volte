#define UNICODE
#define _UNICODE
#include <windows.h>
#include <commctrl.h>
#include <strsafe.h>
#include "startup.h"

#pragma comment(lib, "comctl32.lib")

#define IDC_LISTVIEW 1001
#define IDC_BTN_REFRESH 1002
#define IDC_BTN_DISABLE 1003
#define IDC_BTN_ENABLE 1004
#define IDC_BTN_HIDDEN 1005
#define IDC_BTN_DELETE 1006
#define IDC_BTN_OPENLOC 1007
#define IDC_BTN_EXIT 1008
#define IDC_STATIC_INFO 1009

HINSTANCE g_hInst;
HWND g_hList, g_hInfo;
HFONT g_hFont;
WCHAR g_szClassName[] = L"StartupManagerClass";

void UpdateInfo(void) {
    WCHAR buf[256];
    int enabled=0, disabled=0;
    for(int i=0;i<g_itemCount;i++){
        if(g_items[i].enabled) enabled++; else disabled++;
    }
    StringCchPrintfW(buf, 256, L"Tìm thấy %d mục  |  Đang bật: %d  |  Đã tắt: %d  |  Chọn 1 mục rồi bấm nút để thao tác.", g_itemCount, enabled, disabled);
    SetWindowTextW(g_hInfo, buf);
}

int GetSelectedIndex(void){
    int idx = ListView_GetNextItem(g_hList, -1, LVNI_SELECTED);
    return idx;
}

void RefreshList(void){
    ScanStartupItems();
    ListView_DeleteAllItems(g_hList);
    for(int i=0;i<g_itemCount;i++){
        StartupItem* it=&g_items[i];
        LVITEMW lv={0};
        lv.mask=LVIF_TEXT | LVIF_PARAM;
        lv.iItem=i;
        lv.lParam=i;
        lv.pszText=it->name;
        int row = ListView_InsertItem(g_hList, &lv);
        ListView_SetItemText(g_hList, row, 1, it->command);
        ListView_SetItemText(g_hList, row, 2, it->location);
        WCHAR status[64];
        if(!it->enabled) StringCchCopyW(status,64,L"Đã tắt");
        else if(it->isHidden) StringCchCopyW(status,64,L"Ẩn (minimized)");
        else StringCchCopyW(status,64,L"Đang bật");
        ListView_SetItemText(g_hList, row, 3, status);
        WCHAR src[64];
        StringCchCopyW(src,64, SourceToString(it->source));
        ListView_SetItemText(g_hList, row, 4, src);
    }
    UpdateInfo();
}

LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam){
    switch(msg){
        case WM_CREATE: {
            INITCOMMONCONTROLSEX icex={sizeof(icex), ICC_LISTVIEW_CLASSES};
            InitCommonControlsEx(&icex);

            // Font
            NONCLIENTMETRICSW ncm={sizeof(ncm)};
            SystemParametersInfoW(SPI_GETNONCLIENTMETRICS, sizeof(ncm), &ncm, 0);
            // Use Segoe UI 9pt for better Vietnamese
            HFONT hFont = CreateFontW(-12,0,0,0,FW_NORMAL,FALSE,FALSE,FALSE,DEFAULT_CHARSET,OUT_DEFAULT_PRECIS,CLIP_DEFAULT_PRECIS,DEFAULT_QUALITY,DEFAULT_PITCH|FF_DONTCARE, L"Segoe UI");
            if(hFont) g_hFont=hFont;
            else g_hFont = CreateFontIndirectW(&ncm.lfMessageFont);

            // ListView
            g_hList = CreateWindowExW(WS_EX_CLIENTEDGE, WC_LISTVIEWW, L"",
                WS_CHILD|WS_VISIBLE|LVS_REPORT|LVS_SINGLESEL|LVS_SHOWSELALWAYS,
                10,10,900,360, hwnd, (HMENU)IDC_LISTVIEW, g_hInst, NULL);
            SendMessageW(g_hList, WM_SETFONT, (WPARAM)g_hFont, TRUE);
            ListView_SetExtendedListViewStyle(g_hList, LVS_EX_FULLROWSELECT|LVS_EX_GRIDLINES|LVS_EX_DOUBLEBUFFER);

            LVCOLUMNW col={0};
            col.mask=LVCF_TEXT|LVCF_WIDTH|LVCF_FMT;
            col.fmt=LVCFMT_LEFT;
            col.cx=160; col.pszText=L"Tên ứng dụng"; ListView_InsertColumn(g_hList,0,&col);
            col.cx=340; col.pszText=L"Lệnh khởi động"; ListView_InsertColumn(g_hList,1,&col);
            col.cx=160; col.pszText=L"Vị trí"; ListView_InsertColumn(g_hList,2,&col);
            col.cx=110; col.pszText=L"Trạng thái"; ListView_InsertColumn(g_hList,3,&col);
            col.cx=130; col.pszText=L"Nguồn"; ListView_InsertColumn(g_hList,4,&col);

            // Buttons row 1
            int bx=10, by=380, bw=120, bh=28, gap=8;
            HWND hBtn;
            hBtn=CreateWindowW(L"BUTTON", L"Làm mới", WS_CHILD|WS_VISIBLE|BS_PUSHBUTTON, bx, by, bw, bh, hwnd, (HMENU)IDC_BTN_REFRESH, g_hInst, NULL); SendMessageW(hBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE); bx+=bw+gap;
            hBtn=CreateWindowW(L"BUTTON", L"Tắt", WS_CHILD|WS_VISIBLE|BS_PUSHBUTTON, bx, by, bw, bh, hwnd, (HMENU)IDC_BTN_DISABLE, g_hInst, NULL); SendMessageW(hBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE); bx+=bw+gap;
            hBtn=CreateWindowW(L"BUTTON", L"Bật lại", WS_CHILD|WS_VISIBLE|BS_PUSHBUTTON, bx, by, bw, bh, hwnd, (HMENU)IDC_BTN_ENABLE, g_hInst, NULL); SendMessageW(hBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE); bx+=bw+gap;
            hBtn=CreateWindowW(L"BUTTON", L"Chạy ẩn/hiện", WS_CHILD|WS_VISIBLE|BS_PUSHBUTTON, bx, by, 130, bh, hwnd, (HMENU)IDC_BTN_HIDDEN, g_hInst, NULL); SendMessageW(hBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE); bx+=130+gap;
            hBtn=CreateWindowW(L"BUTTON", L"Xóa", WS_CHILD|WS_VISIBLE|BS_PUSHBUTTON, bx, by, 80, bh, hwnd, (HMENU)IDC_BTN_DELETE, g_hInst, NULL); SendMessageW(hBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE); bx+=80+gap;
            hBtn=CreateWindowW(L"BUTTON", L"Mở vị trí", WS_CHILD|WS_VISIBLE|BS_PUSHBUTTON, bx, by, 110, bh, hwnd, (HMENU)IDC_BTN_OPENLOC, g_hInst, NULL); SendMessageW(hBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE); 

            // second row info + exit
            g_hInfo = CreateWindowW(L"STATIC", L"", WS_CHILD|WS_VISIBLE|SS_LEFT, 10, 418, 780, 20, hwnd, (HMENU)IDC_STATIC_INFO, g_hInst, NULL);
            SendMessageW(g_hInfo, WM_SETFONT, (WPARAM)g_hFont, TRUE);
            hBtn=CreateWindowW(L"BUTTON", L"Thoát", WS_CHILD|WS_VISIBLE|BS_PUSHBUTTON, 810, 415, 100, 28, hwnd, (HMENU)IDC_BTN_EXIT, g_hInst, NULL);
            SendMessageW(hBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

            RefreshList();
            break;
        }
        case WM_SIZE: {
            int w=LOWORD(lParam), h=HIWORD(lParam);
            if(g_hList){
                MoveWindow(g_hList, 10,10, w-20, h-100, TRUE);
                // adjust columns proportionally?
            }
            // reposition buttons
            // find buttons by ID and move
            HWND b;
            int by = h - 70;
            b=GetDlgItem(hwnd, IDC_BTN_REFRESH); if(b) MoveWindow(b,10,by,120,28,TRUE);
            b=GetDlgItem(hwnd, IDC_BTN_DISABLE); if(b) MoveWindow(b,138,by,120,28,TRUE);
            b=GetDlgItem(hwnd, IDC_BTN_ENABLE); if(b) MoveWindow(b,266,by,120,28,TRUE);
            b=GetDlgItem(hwnd, IDC_BTN_HIDDEN); if(b) MoveWindow(b,394,by,130,28,TRUE);
            b=GetDlgItem(hwnd, IDC_BTN_DELETE); if(b) MoveWindow(b,532,by,80,28,TRUE);
            b=GetDlgItem(hwnd, IDC_BTN_OPENLOC); if(b) MoveWindow(b,620,by,110,28,TRUE);
            b=GetDlgItem(hwnd, IDC_STATIC_INFO); if(b) MoveWindow(b,10,by+35,w-130,20,TRUE);
            b=GetDlgItem(hwnd, IDC_BTN_EXIT); if(b) MoveWindow(b,w-110,by+32,100,28,TRUE);
            break;
        }
        case WM_COMMAND: {
            int id=LOWORD(wParam);
            int sel=GetSelectedIndex();
            switch(id){
                case IDC_BTN_REFRESH:
                    RefreshList();
                    break;
                case IDC_BTN_DISABLE: {
                    if(sel<0){MessageBoxW(hwnd,L"Vui lòng chọn 1 ứng dụng trong danh sách.",L"Chưa chọn",MB_OK|MB_ICONWARNING); break;}
                    if(!g_items[sel].enabled){MessageBoxW(hwnd,L"Mục này đã bị tắt rồi.",L"Thông báo",MB_OK|MB_ICONINFORMATION); break;}
                    WCHAR msg[512];
                    StringCchPrintfW(msg,512,L"Bạn muốn TẮT khởi động cùng Windows cho:\n\n\"%s\"\n%s\n\nCó thể BẬT lại sau bằng nút \"Bật lại\".", g_items[sel].name, g_items[sel].command);
                    if(MessageBoxW(hwnd,msg,L"Xác nhận TẮT",MB_YESNO|MB_ICONQUESTION)==IDYES){
                        if(DisableStartupItem(sel)){
                            MessageBoxW(hwnd,L"Đã tắt thành công!\nỨng dụng sẽ không chạy cùng Windows nữa.\nDữ liệu đã được backup để có thể bật lại.",L"Thành công",MB_OK|MB_ICONINFORMATION);
                            RefreshList();
                        } else {
                            DWORD err=GetLastError();
                            WCHAR errMsg[256];
                            StringCchPrintfW(errMsg,256,L"Không thể tắt. Lỗi: %lu\n\nNếu là HKLM\\Run cần chạy Run as Administrator.", err);
                            MessageBoxW(hwnd,errMsg,L"Lỗi",MB_OK|MB_ICONERROR);
                        }
                    }
                    break;
                }
                case IDC_BTN_ENABLE: {
                    if(sel<0){MessageBoxW(hwnd,L"Vui lòng chọn 1 mục ĐÃ TẮT (có trạng thái \"Đã tắt\").",L"Chưa chọn",MB_OK|MB_ICONWARNING); break;}
                    if(g_items[sel].enabled){MessageBoxW(hwnd,L"Mục này đang bật, không cần bật lại.",L"Thông báo",MB_OK|MB_ICONINFORMATION); break;}
                    if(g_items[sel].source!=SRC_DISABLED_BACKUP){MessageBoxW(hwnd,L"Chỉ có thể bật lại các mục đã tắt bằng tool này (ở mục \"Đã tắt (Backup)\").",L"Thông báo",MB_OK|MB_ICONINFORMATION); break;}
                    WCHAR msg[512];
                    StringCchPrintfW(msg,512,L"Bật lại khởi động cùng Windows cho:\n\n\"%s\"\n%s ?", g_items[sel].name, g_items[sel].command);
                    if(MessageBoxW(hwnd,msg,L"Xác nhận BẬT",MB_YESNO|MB_ICONQUESTION)==IDYES){
                        if(EnableStartupItem(sel)){
                            MessageBoxW(hwnd,L"Đã bật lại thành công!",L"Thành công",MB_OK|MB_ICONINFORMATION);
                            RefreshList();
                        } else {
                            MessageBoxW(hwnd,L"Không thể bật lại. Thử chạy với quyền Admin.",L"Lỗi",MB_OK|MB_ICONERROR);
                        }
                    }
                    break;
                }
                case IDC_BTN_HIDDEN: {
                    if(sel<0){MessageBoxW(hwnd,L"Vui lòng chọn 1 ứng dụng HKCU\\Run hoặc HKLM\\Run.",L"Chưa chọn",MB_OK|MB_ICONWARNING); break;}
                    StartupItem* it=&g_items[sel];
                    if(it->source!=SRC_HKCU_RUN && it->source!=SRC_HKLM_RUN){
                        MessageBoxW(hwnd,L"Chức năng \"Chạy ẩn\" chỉ hỗ trợ Registry Run (HKCU/HKLM).\nVới Startup Folder, hãy chuột phải file .lnk > Properties > Run: Minimized.",L"Thông báo",MB_OK|MB_ICONINFORMATION);
                        break;
                    }
                    BOOL toHidden = !it->isHidden;
                    WCHAR msg[600];
                    if(toHidden){
                        StringCchPrintfW(msg,600,L"Đổi \"%s\" sang chế độ CHẠY ẨN (minimized)?\n\nLệnh hiện tại:\n%s\n\nSau khi đổi sẽ thành:\ncmd.exe /c start /min \"\" <lệnh gốc>\n\nỨng dụng sẽ khởi động ở chế độ thu nhỏ, không hiện cửa sổ.", it->name, it->command);
                    } else {
                        StringCchPrintfW(msg,600,L"Đổi \"%s\" về chế độ CHẠY BÌNH THƯỜNG?\n\nLệnh hiện tại (ẩn):\n%s\n\nSẽ khôi phục về lệnh gốc.", it->name, it->command);
                    }
                    if(MessageBoxW(hwnd,msg,toHidden?L"Chạy ẩn":L"Chạy hiện",MB_YESNO|MB_ICONQUESTION)==IDYES){
                        if(SetHiddenMode(sel,toHidden)){
                            MessageBoxW(hwnd,toHidden?L"Đã chuyển sang chạy ẩn!":L"Đã chuyển về chạy bình thường!",L"Thành công",MB_OK|MB_ICONINFORMATION);
                            RefreshList();
                            // reselect
                            ListView_SetItemState(g_hList, sel, LVIS_SELECTED|LVIS_FOCUSED, LVIS_SELECTED|LVIS_FOCUSED);
                        } else {
                            MessageBoxW(hwnd,L"Không thể đổi chế độ. Cần quyền Admin với HKLM.",L"Lỗi",MB_OK|MB_ICONERROR);
                        }
                    }
                    break;
                }
                case IDC_BTN_DELETE: {
                    if(sel<0){MessageBoxW(hwnd,L"Vui lòng chọn 1 ứng dụng để xóa.",L"Chưa chọn",MB_OK|MB_ICONWARNING); break;}
                    WCHAR msg[512];
                    StringCchPrintfW(msg,512,L"CẢNH BÁO: Xóa vĩnh viễn?\n\n\"%s\"\n%s\nVị trí: %s\n\nHành động này KHÔNG backup và không thể hoàn tác!\nBạn có chắc?", g_items[sel].name, g_items[sel].command, g_items[sel].location);
                    if(MessageBoxW(hwnd,msg,L"Xác nhận XÓA",MB_YESNO|MB_ICONWARNING|MB_DEFBUTTON2)==IDYES){
                        if(DeleteStartupItem(sel)){
                            MessageBoxW(hwnd,L"Đã xóa.",L"Thành công",MB_OK|MB_ICONINFORMATION);
                            RefreshList();
                        } else {
                            MessageBoxW(hwnd,L"Không thể xóa. Cần quyền Admin.",L"Lỗi",MB_OK|MB_ICONERROR);
                        }
                    }
                    break;
                }
                case IDC_BTN_OPENLOC: {
                    if(sel<0){MessageBoxW(hwnd,L"Vui lòng chọn 1 ứng dụng.",L"Chưa chọn",MB_OK|MB_ICONWARNING); break;}
                    if(!OpenItemLocation(sel)){
                        MessageBoxW(hwnd,L"Không mở được vị trí.",L"Lỗi",MB_OK|MB_ICONERROR);
                    }
                    break;
                }
                case IDC_BTN_EXIT:
                    DestroyWindow(hwnd);
                    break;
            }
            break;
        }
        case WM_NOTIFY: {
            LPNMHDR pnm=(LPNMHDR)lParam;
            if(pnm->idFrom==IDC_LISTVIEW && pnm->code==NM_DBLCLK){
                int sel=GetSelectedIndex();
                if(sel>=0){
                    WCHAR detail[1024];
                    StringCchPrintfW(detail,1024,L"Tên: %s\nLệnh: %s\nVị trí: %s\nNguồn: %s\nTrạng thái: %s",
                        g_items[sel].name, g_items[sel].command, g_items[sel].location, SourceToString(g_items[sel].source), g_items[sel].enabled?(g_items[sel].isHidden?L"Ẩn":L"Đang bật"):L"Đã tắt");
                    MessageBoxW(hwnd,detail, L"Chi tiết", MB_OK|MB_ICONINFORMATION);
                }
            }
            break;
        }
        case WM_DESTROY:
            if(g_hFont) DeleteObject(g_hFont);
            PostQuitMessage(0);
            break;
        default: return DefWindowProcW(hwnd,msg,wParam,lParam);
    }
    return 0;
}

int WINAPI wWinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, PWSTR pCmdLine, int nCmdShow){
    g_hInst=hInstance;
    WNDCLASSW wc={0};
    wc.lpfnWndProc=WndProc;
    wc.hInstance=hInstance;
    wc.lpszClassName=g_szClassName;
    wc.hbrBackground=(HBRUSH)(COLOR_WINDOW+1);
    wc.hCursor=LoadCursor(NULL, IDC_ARROW);
    wc.hIcon=LoadIcon(NULL, IDI_APPLICATION);
    RegisterClassW(&wc);

    // Enable DPI aware
    HMODULE hUser32 = GetModuleHandleW(L"user32.dll");
    if(hUser32){
        typedef BOOL (WINAPI *SetDPIAwareFunc)(void);
        SetDPIAwareFunc f = (SetDPIAwareFunc)GetProcAddress(hUser32, "SetProcessDPIAware");
        if(f) f();
    }

    HWND hwnd=CreateWindowExW(0, g_szClassName, L"Startup Manager - Quản lý khởi động Windows (Portable <5MB)",
        WS_OVERLAPPEDWINDOW, CW_USEDEFAULT,CW_USEDEFAULT, 960,520, NULL,NULL,hInstance,NULL);
    ShowWindow(hwnd, nCmdShow);
    UpdateWindow(hwnd);

    MSG msg;
    while(GetMessageW(&msg,NULL,0,0)){
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
    return (int)msg.wParam;
}
