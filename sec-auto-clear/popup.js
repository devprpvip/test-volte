// popup.js - logic UI nhẹ, không framework
const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

let currentSettings = null;
let countdownTimer = null;

function formatTime(ts) {
  if (!ts) return "—";
  const d = new Date(ts);
  return d.toLocaleString("vi-VN", { hour12: false });
}

function formatCountdown(next) {
  if (!next) return "";
  const diff = next - Date.now();
  if (diff <= 0) return "Sắp dọn…";
  const m = Math.floor(diff / 60000);
  const s = Math.floor((diff % 60000) / 1000);
  if (m >= 1440) {
    const d = Math.floor(m / 1440);
    const h = Math.floor((m % 1440) / 60);
    return `Còn ~${d} ngày ${h} giờ`;
  }
  if (m >= 60) {
    const h = Math.floor(m / 60);
    const rm = m % 60;
    return `Còn ~${h} giờ ${rm} phút`;
  }
  return `Còn ${m} phút ${String(s).padStart(2, "0")} giây`;
}

function showToast(msg, type = "success", ms = 2500) {
  const el = $("#toast");
  el.textContent = msg;
  el.className = `toast ${type}`;
  el.classList.remove("hidden");
  setTimeout(() => el.classList.add("hidden"), ms);
}

async function send(type, payload) {
  return new Promise((resolve) => {
    chrome.runtime.sendMessage({ type, payload }, (res) => {
      if (chrome.runtime.lastError) {
        resolve({ ok: false, error: chrome.runtime.lastError.message });
      } else {
        resolve(res);
      }
    });
  });
}

async function load() {
  if (load._inFlight) return;
  load._inFlight = true;
  try {
    const res = await send("GET_STATUS");
    if (!res || !res.ok) {
      showToast("Không tải được cài đặt", "error");
      return;
    }
    const { settings, alarm } = res.data;
    currentSettings = settings;

    // bind UI
    $("#enabled").checked = !!settings.enabled;
    $("#interval").value = settings.interval || "60";
    $("#notifyOnClear").checked = !!settings.notifyOnClear;

    $$("[data-key]").forEach((cb) => {
      const k = cb.dataset.key;
      cb.checked = !!settings.dataTypes[k];
    });

    $("#lastCleared").textContent = settings.lastCleared ? formatTime(settings.lastCleared) : "Chưa dọn lần nào";
    if (settings.interval === "onClose") {
      $("#nextClear").textContent = "Khi đóng Chrome";
      $("#countdown").textContent = settings.enabled ? "Sẽ dọn khi bạn đóng cửa sổ cuối cùng" : "Đã tắt";
    } else {
      // ưu tiên scheduledTime của alarm thật, fallback nextClear đã lưu
      let next = settings.nextClear;
      if (alarm && alarm.scheduledTime) next = alarm.scheduledTime;
      $("#nextClear").textContent = next ? formatTime(next) : "—";
      if (!settings.enabled) {
        $("#countdown").textContent = "Đã tắt";
      } else {
        startCountdown(next);
      }
    }

    updateEnabledState();
  } finally {
    load._inFlight = false;
  }
}

function startCountdown(nextTs) {
  clearInterval(countdownTimer);
  if (!nextTs || $("#interval").value === "onClose") {
    $("#countdown").textContent = "";
    return;
  }
  let reloadScheduled = false; // chống spam setTimeout(load) mỗi tick khi countdown về 0
  const tick = () => {
    $("#countdown").textContent = formatCountdown(nextTs);
    if (nextTs - Date.now() <= 0) {
      clearInterval(countdownTimer);
      if (!reloadScheduled) {
        reloadScheduled = true;
        // reload sau 1.5s để cập nhật lastCleared nếu vừa dọn
        setTimeout(load, 1500);
      }
    }
  };
  tick();
  countdownTimer = setInterval(tick, 1000);
}

function updateEnabledState() {
  const on = $("#enabled").checked;
  $("#interval").disabled = !on;
  $$("[data-key]").forEach((e) => (e.disabled = !on));
  $("#notifyOnClear").disabled = !on;
  $("#clearNow").disabled = false; // luôn cho phép dọn thủ công
  document.body.style.opacity = on ? "1" : "0.9";
}

function collectForm() {
  const dataTypes = {};
  $$("[data-key]").forEach((cb) => (dataTypes[cb.dataset.key] = cb.checked));
  return {
    enabled: $("#enabled").checked,
    interval: $("#interval").value,
    notifyOnClear: $("#notifyOnClear").checked,
    dataTypes
  };
}

async function handleSave(showMsg = true) {
  const payload = collectForm();
  // nếu bật notify, cần xin quyền notifications (optional_permissions) bằng user gesture
  if (payload.notifyOnClear) {
    try {
      const has = await chrome.permissions.contains({ permissions: ["notifications"] });
      if (!has) {
        const granted = await new Promise((resolve) => {
          chrome.permissions.request({ permissions: ["notifications"] }, (g) => resolve(g));
        });
        if (!granted) {
          payload.notifyOnClear = false;
          $("#notifyOnClear").checked = false;
          showToast("Bạn đã từ chối quyền thông báo", "info");
        }
      }
    } catch {}
  }
  const res = await send("SAVE_SETTINGS", payload);
  if (res && res.ok) {
    currentSettings = res.data;
    if (res.needNotifyPerm) {
      // background báo cần perm nhưng ta đã handle ở trên
    }
    await load();
    if (showMsg) showToast("Đã lưu cài đặt ✓", "success");
  } else {
    showToast("Lưu thất bại: " + (res?.error || "unknown"), "error");
  }
}

async function handleClearNow() {
  const btn = $("#clearNow");
  const old = btn.textContent;
  btn.textContent = "Đang dọn...";
  btn.disabled = true;
  const res = await send("TRIGGER_CLEAR");
  btn.textContent = old;
  btn.disabled = false;
  if (res && res.ok && res.data && res.data.success) {
    showToast("Đã dọn sạch ✓", "success");
    await load();
  } else if (res && res.data && res.data.skipped) {
    showToast("Bỏ qua: " + (res.data.reason || ""), "info");
  } else {
    showToast("Dọn thất bại: " + (res?.error || res?.data?.error || "unknown"), "error");
  }
}

// Events
document.addEventListener("DOMContentLoaded", () => {
  load();

  $("#enabled").addEventListener("change", () => {
    updateEnabledState();
    handleSave(false);
  });
  $("#interval").addEventListener("change", () => handleSave(false));
  $("#notifyOnClear").addEventListener("change", () => handleSave(false));
  $$("[data-key]").forEach((cb) => cb.addEventListener("change", () => handleSave(false)));

  $("#save").addEventListener("click", () => handleSave(true));
  $("#clearNow").addEventListener("click", handleClearNow);

  $("#openHelp").addEventListener("click", (e) => {
    e.preventDefault();
    $("#helpModal").classList.remove("hidden");
  });
  $("#closeHelp").addEventListener("click", () => $("#helpModal").classList.add("hidden"));
  $("#helpModal").addEventListener("click", (e) => {
    if (e.target.id === "helpModal") $("#helpModal").classList.add("hidden");
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") $("#helpModal").classList.add("hidden");
  });
});

// Auto refresh mỗi 30s khi popup đang mở để cập nhật countdown / nextClear
setInterval(() => {
  // chỉ refresh nếu tab/popup đang hiển thị và ở chế độ định kỳ đã bật
  if (document.visibilityState !== "visible") return;
  if (currentSettings && currentSettings.enabled && currentSettings.interval !== "onClose") load();
}, 30000);
