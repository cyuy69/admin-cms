// 全域變數
let chartTraffic = null;
let chartSales = null;
let chartPie = null;

// 顏色
function getColorByIndex(i) {
    const colors = ["#ff9900", "#0077ff", "#33cc33", "#cc33ff", "#ff3333"];
    return colors[i % colors.length];
}

// 主函式：讀取分析
async function loadAnalytics() {

    // === 取得勾選的活動 ===
    const eventIds = [...document.querySelectorAll('.event-check:checked')]
        .map(e => e.value);

    // === 取得模式：merge / compare ===
    const mode = document.querySelector('input[name="mode"]:checked')?.value || "merge";



    // 無活動 → 顯示空資料（全部為 0）

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
            data: { labels, datasets: [{ label: "銷售數量", data: zeros, borderColor: "#ccc" }] }
        });

        // 圓餅圖
        if (chartPie) chartPie.destroy();
        chartPie = new Chart(document.getElementById("pieChart"), {
            type: "pie",
            data: {
                labels: ["無資料"],
                datasets: [{ data: [1], backgroundColor: ["#cccccc"] }]
            }
        });

        return;
    }



    // 呼叫後端 API

    const query = eventIds.map(id => `eventIds=${id}`).join("&");
    let resp = await fetch(`/api/analytics?${query}&mode=${mode}`);
    let data = await resp.json();



    // ⭐ 比較模式（compare）

    if (mode === "compare") {

        // ===== 流量圖（多條線） =====
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
        chartPie = new Chart(document.getElementById("pieChart"), {
            type: "pie",
            data: {
                labels: ["比較模式無個別圓餅"],
                datasets: [{ data: [1], backgroundColor: ["#cccccc"] }]
            }
        });

        // ===== KPI：顯示合併（你要每活動 KPI 也可以額外做） =====
        const sumViews = eventIds.reduce((sum, id) => sum + data.compare[id].kpi.todayViews, 0);
        const sumSales = eventIds.reduce((sum, id) => sum + data.compare[id].kpi.todaySales, 0);
        const sumRevenue = eventIds.reduce((sum, id) => sum + data.compare[id].kpi.weekRevenue, 0);

        document.getElementById("kpiViews").innerText = sumViews;
        document.getElementById("kpiSales").innerText = sumSales;
        document.getElementById("kpiRevenue").innerText = sumRevenue;
        document.getElementById("kpiConversion").innerText =
            (sumViews === 0 ? 0 : (sumSales / sumViews * 100).toFixed(1)) + "%";

        return; // compare 模式一定要 return
    }

    // 合併模式（merge）

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
    if (chartPie) chartPie.destroy();
    chartPie = new Chart(document.getElementById("pieChart"), {
        type: "pie",
        data: {
            labels: data.ticketPie.labels,
            datasets: [{
                data: data.ticketPie.data,
                backgroundColor: ["#ff6600", "#0099ff", "#66cc33", "#999999"]
            }]
        }
    });
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
        });
    });

    // 監聽模式（合併 / 比對）
    document.querySelectorAll("input[name='mode']").forEach(r => {
        r.addEventListener("change", () => {
            // 保存模式
            localStorage.setItem("analytics_mode", r.value);
            loadAnalytics();
        });
    });
}

function initTrafficAnalytics() {
    // 確保 canvas 存在
    if (!document.getElementById("trafficChart")) {
        return;
    }

    // 從 localStorage 還原活動選擇
    const savedEvents = JSON.parse(localStorage.getItem("analytics_selectedEvents") || "[]");
    if (savedEvents.length > 0) {
        document.querySelectorAll(".event-check").forEach(cb => {
            cb.checked = savedEvents.includes(cb.value);
        });
    }

    // 還原模式 (merge / compare)
    const savedMode = localStorage.getItem("analytics_mode");
    if (savedMode) {
        const radio = document.querySelector(`input[name='mode'][value='${savedMode}']`);
        if (radio) radio.checked = true;
    }

    initEventSelectionListener();
    loadAnalytics();
}

