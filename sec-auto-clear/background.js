// Sec Auto Clear - background service worker (MV3)
// Nhẹ, ổn định, tự lưu vào Chrome profile (không phụ thuộc file gốc sau khi Pack)
// Hooks đúng theo docs: https://developer.chrome.com/docs/extensions/reference/api/*

const SETTINGS_KEY = "secSettings";

function sameJSON(a, b) {
  try { return JSON.stringify(a) === JSON.stringify(b); } catch { return false; }
}

async function getSettings() {
  let syncData = {};
  let localData = {};
  try { syncData = await chrome.storage.sync.get([SETTINGS_KEY]); } catch (e) {}
  try { localData = await chrome.storage.local.get([SETTINGS_KEY]); } catch (e) {}
  let src = syncData[SETTINGS_KEY] || localData[SETTINGS_KEY];
  if (!src) {
    // seed cả 2 nơi
    try { await chrome.storage.sync.set({ [SETTINGS_KEY]: DEFAULT_SETTINGS }); } catch (e) {}
    try { await chrome.storage.local.set({ [SETTINGS_KEY]: DEFAULT_SETTINGS }); } catch (e) {}
    return { ...DEFAULT_SETTINGS };
  }
  const merged = {
    ...DEFAULT_SETTINGS,
    ...src,
    dataTypes: { ...DEFAULT_SETTINGS.dataTypes, ...(src.dataTypes || {}) }
  };
  // Nếu sync thiếu nhưng local có (hoặc ngược lại) thì đồng bộ lại để tự hồi phục
  if (!syncData[SETTINGS_KEY] && localData[SETTINGS_KEY] && !sameJSON(syncData[SETTINGS_KEY], merged)) {
    try { await chrome.storage.sync.set({ [SETTINGS_KEY]: merged }); } catch {}
  }
  if (!localData[SETTINGS_KEY] && syncData[SETTINGS_KEY] && !sameJSON(localData[SETTINGS_KEY], merged)) {
    try { await chrome.storage.local.set({ [SETTINGS_KEY]: merged }); } catch {}
  }
  return merged;
}

async function saveSettings(patch) {
  const current = await getSettings();
  const merged = { ...current, ...patch };
  if (patch.dataTypes) {
    merged.dataTypes = { ...current.dataTypes, ...patch.dataTypes };
  }
  // Không thay đổi gì thì bỏ qua để tránh ghi storage thừa (quota sync giới hạn số lần ghi)
  if (sameJSON(current, merged)) return merged;
  // Lưu vào CẢ hai để bền nhất: sync để theo tài khoản, local để fallback offline/quota
  try { await chrome.storage.sync.set({ [SETTINGS_KEY]: merged }); } catch (e) {
    console.warn("[Sec Auto Clear] sync save fail (quota/offline?)", e);
  }
  try { await chrome.storage.local.set({ [SETTINGS_KEY]: merged }); } catch (e) {
    console.warn("[Sec Auto Clear] local save fail", e);
  }
  return merged;
}

// Đồng bộ khi storage thay đổi từ nơi khác (ví dụ popup ghi sync thì local cũng cập nhật)
try {
  chrome.storage.onChanged.addListener((changes, area) => {
    if (changes[SETTINGS_KEY]) {
      const val = changes[SETTINGS_KEY].newValue;
      if (!val) return;
      if (area === "sync") {
        // chỉ ghi local nếu chưa giống (tránh ghi thừa)
        chrome.storage.local.get([SETTINGS_KEY]).then((r) => {
          if (!sameJSON(r[SETTINGS_KEY], val)) {
            chrome.storage.local.set({ [SETTINGS_KEY]: val }).catch(() => {});
          }
        }).catch(() => {});
      } else if (area === "local") {
        // tránh vòng lặp: chỉ sync nếu sync đang khác
        chrome.storage.sync.get([SETTINGS_KEY]).then((r) => {
          if (!sameJSON(r[SETTINGS_KEY], val)) {
            chrome.storage.sync.set({ [SETTINGS_KEY]: val }).catch(() => {});
          }
        }).catch(() => {});
      }
    }
  });
} catch {}

const ALARM_NAME = "sec-auto-clear-alarm";
const DEFAULT_SETTINGS = {
  enabled: true,
  interval: "60", // "10" | "60" | "1440" | "onClose"
  dataTypes: {
    cache: true,
    history: true,
    downloads: true,
    localStorage: true,
    formData: true,
    cookies: false,
    indexedDB: false,
    cacheStorage: true,
    serviceWorkers: false
  },
  notifyOnClear: false,
  lastCleared: 0,
  nextClear: 0
};

function buildBrowsingDataOptions(dataTypes) {
  return {
    appcache: !!dataTypes.cache,
    cache: !!dataTypes.cache,
    cacheStorage: !!dataTypes.cacheStorage,
    cookies: !!dataTypes.cookies,
    downloads: !!dataTypes.downloads,
    fileSystems: !!dataTypes.localStorage,
    formData: !!dataTypes.formData,
    history: !!dataTypes.history,
    indexedDB: !!dataTypes.indexedDB,
    localStorage: !!dataTypes.localStorage,
    passwords: false,
    serviceWorkers: !!dataTypes.serviceWorkers,
    webSQL: !!dataTypes.localStorage
  };
}

let isClearing = false;

async function performClear(reason = "alarm", alarmInfo = null) {
  if (isClearing) return { skipped: true, reason: "already_clearing" };
  const settings = await getSettings();
  if (!settings.enabled && reason !== "manual") {
    return { skipped: true, reason: "disabled" };
  }
  const types = buildBrowsingDataOptions(settings.dataTypes);
  const hasAny = Object.values(types).some(Boolean);
  if (!hasAny) return { skipped: true, reason: "no_type" };

  isClearing = true;
  const start = Date.now();
  try {
    await chrome.browsingData.remove(
      {
        since: 0,
        originTypes: { unprotectedWeb: true, protectedWeb: false, extension: false }
      },
      types
    );
    const now = Date.now();
    // Tính "lần tới" KHỚP với lịch alarm thật của Chrome:
    // - alarm: dùng scheduledTime của chính alarm (đã được Chrome đẩy sang chu kỳ kế tiếp)
    // - manual: GIỮ nguyên lịch alarm, không đẩy hạn dọn ra xa
    // - startup-fallback/onClose: giữ 0 hoặc theo chu kỳ nếu là định kỳ
    let next = 0;
    if (settings.interval !== "onClose") {
      const mins = parseInt(settings.interval, 10) || 0;
      if (mins > 0) {
        if (reason === "alarm" && alarmInfo && alarmInfo.scheduledTime) {
          next = alarmInfo.scheduledTime + mins * 60 * 1000;
        } else if (reason === "manual") {
          try {
            const a = await chrome.alarms.get(ALARM_NAME);
            next = a && a.scheduledTime ? a.scheduledTime : now + mins * 60 * 1000;
          } catch { next = now + mins * 60 * 1000; }
        } else {
          next = now + mins * 60 * 1000;
        }
      }
    }
    await saveSettings({ lastCleared: now, nextClear: next });
    console.log(`[Sec Auto Clear] Cleared (${reason}) in ${Date.now() - start}ms`, types);
    if (settings.notifyOnClear) {
      try {
        const hasPerm = await chrome.permissions.contains({ permissions: ["notifications"] });
        if (hasPerm) {
          await chrome.notifications.create({
            type: "basic",
            iconUrl: "icons/icon128.png",
            title: "Sec Auto Clear",
            message: `Đã dọn dẹp lúc ${new Date(now).toLocaleString("vi-VN")}`,
            priority: 0
          });
        }
      } catch {}
    }
    return { success: true, clearedAt: now, types };
  } catch (err) {
    console.error("[Sec Auto Clear] Clear failed:", err);
    return { success: false, error: String(err) };
  } finally {
    isClearing = false;
  }
}

// ===== Alarm reconciling =====
async function reconcileAlarm() {
  const settings = await getSettings();
  let existing = null;
  try { existing = await chrome.alarms.get(ALARM_NAME); } catch {}
  if (!settings.enabled || settings.interval === "onClose") {
    if (existing) await chrome.alarms.clear(ALARM_NAME);
    await saveSettings({ nextClear: 0 });
    return;
  }
  const mins = parseInt(settings.interval, 10);
  if (isNaN(mins) || mins <= 0) return;
  if (existing && existing.periodInMinutes === mins) {
    if (!settings.nextClear || settings.nextClear < Date.now()) {
      const next = existing.scheduledTime || (Date.now() + mins * 60 * 1000);
      await saveSettings({ nextClear: next });
    }
    return;
  }
  await chrome.alarms.clear(ALARM_NAME);
  await chrome.alarms.create(ALARM_NAME, { periodInMinutes: mins });
  const next = Date.now() + mins * 60 * 1000;
  await saveSettings({ nextClear: next });
  console.log(`[Sec Auto Clear] Alarm every ${mins} min, next ${new Date(next).toLocaleString()}`);
}

reconcileAlarm().catch(console.error);

// ===== Event listeners - top-level =====
chrome.runtime.onInstalled.addListener(async (details) => {
  console.log("[Sec Auto Clear] onInstalled", details.reason);
  await getSettings();
  await reconcileAlarm();
  if (details.reason === "install") await saveSettings({ lastCleared: 0 });
});

chrome.runtime.onStartup.addListener(async () => {
  await reconcileAlarm();
  const s = await getSettings();
  if (s.enabled && s.interval === "onClose") {
    const sinceLast = Date.now() - (s.lastCleared || 0);
    if (!s.lastCleared || sinceLast > 5 * 60 * 1000) {
      setTimeout(() => performClear("startup-fallback"), 2000);
    }
  }
});

chrome.alarms.onAlarm.addListener(async (alarm) => {
  if (alarm.name !== ALARM_NAME) return;
  // nextClear được tính lại bên trong performClear từ alarm.scheduledTime
  await performClear("alarm", alarm);
});

chrome.windows.onRemoved.addListener(async () => {
  const settings = await getSettings();
  if (settings.interval !== "onClose" || !settings.enabled) return;
  try {
    const windows = await chrome.windows.getAll();
    if (windows.length === 0) {
      setTimeout(() => performClear("onClose").catch(console.error), 300);
    }
  } catch {
    await performClear("onClose").catch(console.error);
  }
});

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  (async () => {
    try {
      if (msg.type === "GET_SETTINGS") {
        sendResponse({ ok: true, data: await getSettings() });
      } else if (msg.type === "SAVE_SETTINGS") {
        const updated = await saveSettings(msg.payload || {});
        await reconcileAlarm();
        if (updated.notifyOnClear) {
          try {
            const has = await chrome.permissions.contains({ permissions: ["notifications"] });
            if (!has) { sendResponse({ ok: true, data: updated, needNotifyPerm: true }); return; }
          } catch {}
        }
        sendResponse({ ok: true, data: updated });
      } else if (msg.type === "TRIGGER_CLEAR") {
        const res = await performClear("manual");
        sendResponse({ ok: !!res.success, data: res });
      } else if (msg.type === "GET_STATUS") {
        const s = await getSettings();
        let alarm = null;
        try { alarm = await chrome.alarms.get(ALARM_NAME); } catch {}
        sendResponse({ ok: true, data: { settings: s, alarm, isClearing } });
      } else if (msg.type === "REQUEST_NOTIFY_PERM") {
        const granted = await chrome.permissions.request({ permissions: ["notifications"] });
        sendResponse({ ok: true, granted });
      } else {
        sendResponse({ ok: false, error: "unknown_type" });
      }
    } catch (e) {
      sendResponse({ ok: false, error: String(e) });
    }
  })();
  return true;
});
