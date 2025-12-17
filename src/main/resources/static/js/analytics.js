// 全域變數
let chartTraffic = null;
let chartSales = null;
let chartPie = null;
let allEvents = [];
let visibleEvents = 10;

// 顏色
function getColorByIndex(i) {
    const colors = ["#ff9900", "#0077ff", "#33cc33", "#cc33ff", "#ff3333"];
    return colors[i % colors.length];
}

// 主函式：讀取分析
async function loadAnalytics() {

    // 取得勾選的活動
    const eventIds = [...document.querySelectorAll('.event-check:checked')]
        .map(e => e.value);

    // 取得模式:merge / compare
    const mode = document.querySelector('input[name="mode"]:checked')?.value || "merge";

    // 無活動顯示空資料（全部為 0)
    if (eventIds.length === 0) {
        // KPI 清空
        document.getElementById("kpiViews").innerText = 0;
        document.getElementById("kpiSales").innerText = 0;
        document.getElementById("kpiRevenue").innerText = 0;
        document.getElementById("kpiConversion").innerText = "0%";

        // 近七天日期
        const labels = [];
        const today = new Date();

        for (let i = 6; i >= 0; i--) {
            const d = new Date(today);
            d.setDate(today.getDate() - i);
            labels.push((d.getMonth() + 1) + "/" + d.getDate());
        }

        const zeros = [0, 0, 0, 0, 0, 0, 0];

        // 流量圖
        if (chartTraffic) chartTraffic.destroy();
        chartTraffic = new Chart(document.getElementById("trafficChart"), {
            type: "line",
            data: { labels, datasets: [{ label: "流量", data: zeros, borderColor: "#ccc" }] }
        });

        // 銷售圖
        if (chartSales) chartSales.destroy();
        chartSales = new Chart(document.getElementById("salesChart"), {
            type: "line",
            data: { labels, datasets: [{ label: "銷售收入", data: zeros, borderColor: "#ccc" }] }
        });

        // 圓餅圖
        if (chartPie) chartPie.destroy();
        renderPieChart(["無資料"], [1], ["#cccccc"]);
        loadOverview();
        return;
    }

    const startDate = document.getElementById("dateStart")?.value || "";
    const endDate = document.getElementById("dateEnd")?.value || "";

    let params = eventIds.map(id => `eventIds=${id}`).join("&");
    params += `&mode=${mode}`;

    if (startDate) params += `&startDate=${startDate}`;
    if (endDate) params += `&endDate=${endDate}`;

    // 呼叫後端 API
    let resp = await fetch(`/api/analytics?${params}`);
    let data = await resp.json();
    console.log("後端回傳資料:", data);

    // 比較模式(compare)
    if (mode === "compare") {

        // 流量圖（多條線）
        const datasetsTraffic = eventIds.map((id, index) => ({
            label: "活動 " + id,
            data: data.compare[id].traffic,
            borderWidth: 2,
            fill: false,
            borderColor: getColorByIndex(index)
        }));

        if (chartTraffic) chartTraffic.destroy();
        chartTraffic = new Chart(document.getElementById("trafficChart"), {
            type: "line",
            data: {
                labels: data.compare[eventIds[0]].labels,
                datasets: datasetsTraffic
            }
        });

        // ===== 銷售圖（多條線） =====
        const datasetsSales = eventIds.map((id, index) => ({
            label: "活動 " + id,
            data: data.compare[id].sales,
            borderWidth: 2,
            fill: false,
            borderColor: getColorByIndex(index)
        }));

        if (chartSales) chartSales.destroy();
        chartSales = new Chart(document.getElementById("salesChart"), {
            type: "line",
            data: {
                labels: data.compare[eventIds[0]].labels,
                datasets: datasetsSales
            }
        });

        // ===== 圓餅圖：目前先合併，不做多餅 =====
        if (chartPie) chartPie.destroy();
        renderPieChart(["比較模式無個別圓餅"], [1], ["#cccccc"]);

        // 不支援KPI顯示(比較模式)
        document.getElementById("kpiViews").innerText = "-";
        document.getElementById("kpiSales").innerText = "-";
        document.getElementById("kpiRevenue").innerText = "-";
        document.getElementById("kpiConversion").innerText = "-";
        return;
    }

    // KPI
    document.getElementById("kpiViews").innerText = data.kpi.todayViews;
    document.getElementById("kpiSales").innerText = data.kpi.todaySales;
    document.getElementById("kpiRevenue").innerText = data.kpi.weekRevenue;
    document.getElementById("kpiConversion").innerText =
        (data.kpi.conversionRate * 100).toFixed(1) + "%";

    // 流量 chart
    if (chartTraffic) chartTraffic.destroy();
    chartTraffic = new Chart(document.getElementById("trafficChart"), {
        type: "line",
        data: {
            labels: data.lineCharts.traffic.labels,
            datasets: [{
                label: "流量",
                data: data.lineCharts.traffic.data,
                borderColor: "#ff9900",
                borderWidth: 2,
                tension: 0.2,
                fill: false
            }]
        }
    });

    // 銷售 chart
    if (chartSales) chartSales.destroy();
    chartSales = new Chart(document.getElementById("salesChart"), {
        type: "line",
        data: {
            labels: data.lineCharts.sales.labels,
            datasets: [{
                label: "銷售數量",
                data: data.lineCharts.sales.data,
                borderColor: "#0077ff",
                borderWidth: 2,
                tension: 0.2,
                fill: false
            }]
        }
    });

    // 圓餅
    let labelsPie = data.ticketPie.labels;
    let dataPie = data.ticketPie.data;

    if (!labelsPie || !dataPie || dataPie.length === 0 || dataPie.every(v => v === 0)) {

        renderPieChart(["無銷售資料"], [1], ["#cccccc"]);
        return;
    }

    const dynamicColors = [
        "#ff6600", "#0099ff", "#66cc33", "#cc33ff",
        "#ff9933", "#6699ff", "#33cc99", "#ff3333"
    ];

    // 根據票種數量裁切顏色
    let pieColors = dynamicColors.slice(0, dataPie.length);

    if (chartPie) chartPie.destroy();
    renderPieChart(labelsPie, dataPie, pieColors);
}

// 載入活動列表
async function loadFilterEventList() {

    const listContainer = document.getElementById("eventFilterList");
    const loadMoreBtn = document.getElementById("loadMoreEventsBtn");
    const searchInput = document.getElementById("eventSearch");

    if (!listContainer) return;

    // 取得所有活動
    let resp = await fetch("/api/events/my");
    const text = await resp.text();
    allEvents = JSON.parse(text);

    // 排序:最新(id最大)→ 最舊
    allEvents.sort((a, b) => b.id - a.id);

    // 還原使用者勾選
    let savedEvents = JSON.parse(localStorage.getItem("analytics_selectedEvents") || "[]");

    // 如果完全沒有儲存過，預設選最新活動
    if (savedEvents.length === 0 && allEvents.length > 0) {
        const newestId = String(allEvents[0].id);
        savedEvents = [newestId];
        localStorage.setItem("analytics_selectedEvents", JSON.stringify(savedEvents));
    }

    setDefaultDateRange();
    // 初始化：顯示前 10 個
    visibleEvents = 10;

    // 搜尋（即時搜索）
    if (searchInput) {
        searchInput.addEventListener("input", () => renderEventList());
    }

    // 顯示更多
    if (loadMoreBtn) {
        loadMoreBtn.addEventListener("click", () => {
            visibleEvents += 10;
            renderEventList();
        });
    }

    // 第一次渲染
    renderEventList();
}

function renderEventList() {

    const listContainer = document.getElementById("eventFilterList");
    const loadMoreBtn = document.getElementById("loadMoreEventsBtn");
    const searchInput = document.getElementById("eventSearch");

    if (!listContainer) return;

    listContainer.innerHTML = "";

    // 從 localStorage 還原勾選
    const savedEvents = JSON.parse(localStorage.getItem("analytics_selectedEvents") || "[]");

    // 搜尋過濾
    const keyword = searchInput?.value.trim() || "";
    let filtered = allEvents.filter(ev =>
        ev.title.includes(keyword) || String(ev.id).includes(keyword)
    );

    // 只顯示visibleEvents個
    let toShow = filtered.slice(0, visibleEvents);

    // 產生 checkbox
    toShow.forEach(ev => {
        const checked = savedEvents.includes(String(ev.id));

        listContainer.insertAdjacentHTML("beforeend", `
            <label>
                <input type="checkbox" class="event-check" value="${ev.id}" ${checked ? "checked" : ""}>
                <span style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${ev.title}</span>
            </label>
        `);
    });

    // 顯示更多按鈕
    loadMoreBtn.style.display = (filtered.length > visibleEvents ? "block" : "none");

    // 重新綁定事件（保留你原本方法）
    initEventSelectionListener();
}

// Checkbox + mode 監聽
function initEventSelectionListener() {
    // 監聽活動 checkbox
    document.querySelectorAll(".event-check").forEach(cb => {
        cb.addEventListener("change", () => {
            // 保存勾選的活動 ID
            const ids = [...document.querySelectorAll('.event-check:checked')]
                .map(e => e.value);
            localStorage.setItem("analytics_selectedEvents", JSON.stringify(ids));
            loadAnalytics();
            loadOverview();
        });
    });

    // 監聽模式（合併 / 比對）
    document.querySelectorAll("input[name='mode']").forEach(r => {
        r.addEventListener("change", () => {
            // 保存模式
            localStorage.setItem("analytics_mode", r.value);
            loadAnalytics();
            loadOverview();
        });
    });
}

async function initTrafficAnalytics() {
    if (!document.getElementById("trafficChart")) return;

    // 要先載入活動列表 (產生 checkbox)
    await loadFilterEventList();

    // 從 localStorage 還原活動選擇
    const savedEvents = JSON.parse(localStorage.getItem("analytics_selectedEvents") || "[]");
    if (savedEvents.length > 0) {
        document.querySelectorAll(".event-check").forEach(cb => {
            cb.checked = savedEvents.includes(cb.value);
        });
    }

    // 還原日期
    const savedStart = localStorage.getItem("analytics_date_start");
    const savedEnd = localStorage.getItem("analytics_date_end");

    if (savedStart) document.getElementById("dateStart").value = savedStart;
    if (savedEnd) document.getElementById("dateEnd").value = savedEnd;


    // 還原模式 (merge / compare)
    const savedMode = localStorage.getItem("analytics_mode");
    if (savedMode) {
        const radio = document.querySelector(`input[name='mode'][value='${savedMode}']`);
        if (radio) radio.checked = true;
    }

    initEventSelectionListener();

    // 日期篩選
    document.getElementById("dateStart").addEventListener("change", () => {
        localStorage.setItem("analytics_date_start", document.getElementById("dateStart").value);
        loadAnalytics();
    });

    document.getElementById("dateEnd").addEventListener("change", () => {
        localStorage.setItem("analytics_date_end", document.getElementById("dateEnd").value);
        loadAnalytics();
    });

    // 重製日期
    document.getElementById("resetDateBtn").addEventListener("click", () => {
        setDefaultDateRange();
        loadAnalytics();
    });

    loadAnalytics();
    loadOverview();
}

function setDefaultDateRange() {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 6);

    document.getElementById("dateStart").value = start.toISOString().split("T")[0];
    document.getElementById("dateEnd").value = end.toISOString().split("T")[0];
}

// 圓餅圖
function renderPieChart(labels, data, colors) {
    if (chartPie) chartPie.destroy();

    chartPie = new Chart(document.getElementById("pieChart"), {
        type: "pie",
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: colors
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: "top",
                    labels: { boxWidth: 20 }
                }
            }
        }
    });
}

// 載入overview的資料
async function loadOverview() {

    // 取得勾選的活動
    const eventIds = [...document.querySelectorAll('.event-check:checked')].map(e => e.value);

    if (eventIds.length === 0) {
        document.getElementById("totalViews").innerText = 0;
        document.getElementById("totalSales").innerText = 0;
        document.getElementById("totalRevenue").innerText = 0;
        document.getElementById("totalEvents").innerText = 0;
        return;
    }

    const params = eventIds.map(id => `eventIds=${id}`).join("&");

    const res = await fetch(`/api/analytics/overview?${params}`);
    const data = await res.json();

    console.log("overview 回傳:", data);

    document.getElementById("totalViews").innerText = data.totalViews;
    document.getElementById("totalSales").innerText = data.totalSales;
    document.getElementById("totalRevenue").innerText = data.totalRevenue;
    document.getElementById("totalEvents").innerText = data.totalEvents;
}


