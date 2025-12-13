// 全域圖表變數
let adminTrafficChart = null;
let adminTxChart = null;

// 初始化
function initAdminDashboard() {

    console.log("Admin Dashboard 初始化");

    // 設定預設日期 (今日 與 7天前)
    const today = new Date();
    const lastWeek = new Date();
    lastWeek.setDate(today.getDate() - 6);

    // 檢查元素存在才賦值，避免 JS 報錯
    const startInput = document.getElementById("adminStartDate");
    const endInput = document.getElementById("adminEndDate");

    if (startInput && endInput) {
        startInput.valueAsDate = lastWeek;
        endInput.valueAsDate = today;

        startInput.addEventListener("change", loadAdminAnalytics);
        endInput.addEventListener("change", loadAdminAnalytics);
    }

    const filterBtn = document.getElementById("adminFilterBtn");
    if (filterBtn) {
        filterBtn.addEventListener("click", resetDateRange);
    }

    requestAnimationFrame(() => {
        loadAdminAnalytics();
        initAdminLoginLogs();
    });
}

// 載入分析 API
async function loadAdminAnalytics() {
    const startEl = document.getElementById("adminStartDate");
    const endEl = document.getElementById("adminEndDate");

    if (!startEl || !endEl) return;

    const start = startEl.value;
    const end = endEl.value;

    let url = `/api/admin/dashboard-analytics?`;
    if (start) url += `startDate=${start}&`;
    if (end) url += `endDate=${end}`;

    console.log("呼叫後端:", url);

    try {
        const resp = await fetch(url);
        if (!resp.ok) throw new Error("API Error");
        const data = await resp.json();

        console.log("後端回傳:", data);

        // 數據防呆處理
        const views = data.homepageViews ?? 0;
        const traffic = data.traffic ?? { labels: [], data: [] };
        const transactions = data.transactions ?? { labels: [], data: [] };
        // 假設後端也回傳了 totalTransactions (如果沒有就自己算 data 加總)
        const totalTx = data.totalTransactions ?? transactions.data.reduce((a, b) => a + b, 0);
        const successRate = data.successRate ?? 0;

        // 更新 KPI 卡片
        // 使用動畫數字效果 (可選) 或直接賦值
        updateText("kpiHomepageViews", views.toLocaleString());
        updateText("kpiTransactions", totalTx.toLocaleString());

        if (document.getElementById("kpiSuccessRate")) {
            const percentage = (successRate * 100).toFixed(1) + "%";
            document.getElementById("kpiSuccessRate").textContent = percentage;
        }

        // 渲染圖表
        renderAdminTrafficChart(traffic);
        renderAdminTxChart(transactions);

    } catch (error) {
        console.error("載入 Dashboard 數據失敗", error);
    }
}

function updateText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}

// 共用圖表設定 (美化用)
const commonChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
        legend: {
            display: true,
            position: 'top',
            labels: {
                usePointStyle: true,
                padding: 20,
                font: { family: "'Nunito', sans-serif" }
            }
        },
        tooltip: {
            backgroundColor: "rgb(255,255,255)",
            bodyColor: "#858796",
            titleColor: "#6e707e",
            borderColor: '#dddfeb',
            borderWidth: 1,
            padding: 10,
            displayColors: false
        }
    },
    scales: {
        x: {
            grid: { display: false, drawBorder: false },
            ticks: { maxTicksLimit: 7 }
        },
        y: {
            grid: { color: "rgb(234, 236, 244)", borderDash: [2], drawBorder: false },
            beginAtZero: true
        }
    }
};

// 日流量折線圖
function renderAdminTrafficChart(trafficData) {
    const ctx = document.getElementById("adminTrafficChart");
    if (!ctx) return;

    if (adminTrafficChart) adminTrafficChart.destroy();

    adminTrafficChart = new Chart(ctx, {
        type: "line",
        data: {
            labels: trafficData.labels,
            datasets: [{
                label: "每日總流量",
                data: trafficData.data,
                borderWidth: 3,
                borderColor: "#4e73df", // Primary Blue
                backgroundColor: "rgba(78, 115, 223, 0.05)",
                pointRadius: 3,
                pointBackgroundColor: "#4e73df",
                pointHoverRadius: 5,
                fill: true,
                tension: 0.4 // 平滑曲線
            }]
        },
        options: commonChartOptions
    });
}

// 銷售額折線圖
function renderAdminTxChart(txData) {
    const ctx = document.getElementById("adminTxChart");
    if (!ctx) return;

    if (adminTxChart) adminTxChart.destroy();

    adminTxChart = new Chart(ctx, {
        type: "line", // 或 bar
        data: {
            labels: txData.labels,
            datasets: [{
                label: "每日成功交易筆數",
                data: txData.data,
                borderWidth: 3,
                borderColor: "#1cc88a", // Success Green (改用綠色表示交易成功)
                backgroundColor: "rgba(28, 200, 138, 0.05)",
                pointRadius: 3,
                pointBackgroundColor: "#1cc88a",
                pointHoverRadius: 5,
                fill: true,
                tension: 0.4
            }]
        },
        options: commonChartOptions
    });
}

// 登入紀錄參數
let loginLogPage = 0;
let loginLogSize = 10;
let loginLogKeyword = "";

// 初始化登入紀錄
function initAdminLoginLogs() {
    const searchBtn = document.getElementById("loginLogSearchBtn");
    if (searchBtn) {
        searchBtn.addEventListener("click", () => {
            const input = document.getElementById("loginLogKeyword");
            loginLogKeyword = input ? input.value.trim() : "";
            loginLogPage = 0;
            loadLoginLogs();
        });
    }

    const prevBtn = document.getElementById("loginLogPrev");
    if (prevBtn) {
        prevBtn.addEventListener("click", () => {
            if (loginLogPage > 0) {
                loginLogPage--;
                loadLoginLogs();
            }
        });
    }

    const nextBtn = document.getElementById("loginLogNext");
    if (nextBtn) {
        nextBtn.addEventListener("click", () => {
            loginLogPage++;
            loadLoginLogs();
        });
    }

    loadLoginLogs();
}

// 呼叫 API
async function loadLoginLogs() {
    const url = `/api/admin/login-logs?keyword=${loginLogKeyword}&page=${loginLogPage}&size=${loginLogSize}`;

    try {
        const resp = await fetch(url);
        if (!resp.ok) return; // 簡單錯誤處理
        const page = await resp.json();

        console.log("登入紀錄:", page);

        renderLoginLogTable(page.content);

        // 更新分頁資訊
        const infoEl = document.getElementById("loginLogPageInfo");
        if (infoEl) infoEl.textContent = `${page.number + 1} / ${page.totalPages}`;

        // 按鈕狀態
        const nextBtn = document.getElementById("loginLogNext");
        const prevBtn = document.getElementById("loginLogPrev");
        if (nextBtn) nextBtn.disabled = page.last;
        if (prevBtn) prevBtn.disabled = page.first;

    } catch (e) {
        console.error("Log fetch failed", e);
    }
}

// 渲染表格 (增加 Badge 樣式)
function renderLoginLogTable(list) {
    const tbody = document.getElementById("loginLogTableBody");
    if (!tbody) return;
    tbody.innerHTML = "";

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 20px;">查無資料</td></tr>`;
        return;
    }

    list.forEach(log => {
        const tr = document.createElement("tr");

        // 狀態 Badge 樣式判斷
        const isSuccess = log.status && log.status.toUpperCase() === 'SUCCESS';
        const badgeClass = isSuccess ? 'status-success' : 'status-fail';
        const statusText = isSuccess ? '成功' : '失敗';

        tr.innerHTML = `
            <td><strong>${log.user?.account || "Unknown"}</strong></td>
            <td>${formatDateTime(log.loginTime)}</td>
            <td>${log.ipAddress}</td>
            <td>${parseUserAgent(log.userAgent)}</td>
            <td><span class="status-badge ${badgeClass}">${statusText}</span></td>
        `;

        tbody.appendChild(tr);
    });
}

// 重置日期按鈕
function resetDateRange() {
    const startInput = document.getElementById("adminStartDate");
    const endInput = document.getElementById("adminEndDate");

    if (!startInput || !endInput) return;

    const today = new Date();
    const lastWeek = new Date();
    lastWeek.setDate(today.getDate() - 6);

    startInput.valueAsDate = lastWeek;
    endInput.valueAsDate = today;

    loadAdminAnalytics(); // 重置後自動更新圖表
}


// 轉換日期格式
function formatDateTime(str) {
    if (!str) return "-";
    const d = new Date(str);
    // 補零處理
    const pad = (n) => n < 10 ? '0' + n : n;
    return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// 解析 User-Agent (簡化顯示)
function parseUserAgent(ua) {
    if (!ua) return "-";

    let os = "OS";
    if (ua.includes("Windows")) os = '<i class="fab fa-windows"></i> Win';
    else if (ua.includes("Mac OS")) os = '<i class="fab fa-apple"></i> Mac';
    else if (ua.includes("Android")) os = '<i class="fab fa-android"></i> Android';
    else if (ua.includes("iPhone") || ua.includes("iPad")) os = '<i class="fab fa-apple"></i> iOS';
    else if (ua.includes("Linux")) os = '<i class="fab fa-linux"></i> Linux';

    let browser = "Browser";
    if (ua.includes("Chrome")) browser = "Chrome";
    else if (ua.includes("Safari") && !ua.includes("Chrome")) browser = "Safari";
    else if (ua.includes("Firefox")) browser = "Firefox";
    else if (ua.includes("Edg")) browser = "Edge";

    return `${os} / ${browser}`;
}