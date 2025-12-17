function initOrders() {
    console.log("initOrders() 執行");

    //  狀態
    let selectedEvents = [];
    let allOrderEvents = [];
    let visibleCount = 10;

    //  邏輯:狀態存取
    function loadSavedState() {
        const saved = sessionStorage.getItem("selectedEvents");
        selectedEvents = saved ? JSON.parse(saved) : [];
    }

    function saveState() {
        sessionStorage.setItem("selectedEvents", JSON.stringify(selectedEvents));
    }

    //  載入活動
    async function fetchEvents(keyword = "") {
        const resp = await fetch(`/api/events/my?keyword=${keyword}`);
        allOrderEvents = await resp.json();
        visibleCount = 10;
        renderEventList();
    }

    //  渲染 popup 活動清單
    function renderEventList() {
        const listEl = document.getElementById("eventList");
        const moreEl = document.getElementById("eventShowMore");
        if (!listEl) return;

        const slice = allOrderEvents.slice(0, visibleCount);
        let html = "";

        slice.forEach(ev => {
            const checked = selectedEvents.some(se => se.id === ev.id) ? "checked" : "";
            html += `
                <label class="event-item">
                    <input type="checkbox" class="event-checkbox"
                        data-id="${ev.id}" data-title="${ev.title}" ${checked}>
                    <span>${ev.title}</span>
                </label>
            `;
        });

        listEl.innerHTML = html;
        moreEl.style.display = visibleCount < allOrderEvents.length ? "block" : "none";
    }

    //  主畫面活動框
    function renderSelectedEvents() {
        const box = document.getElementById("selectedEventBox");
        const tbody = document.getElementById("orderTableBody");

        if (!box) return;

        // 無活動就顯示尚未選擇
        if (selectedEvents.length === 0) {
            box.innerHTML = `
                <button id="openEventModalBtn" class="events-btn events-btn-secondary">選擇活動</button>
                <div class="placeholder">(尚未選擇活動)</div>
            `;
            if (tbody) tbody.innerHTML = "";
            return;
        }

        // 有活動
        let html = `<div class="event-selected-list">`;
        selectedEvents.forEach(ev => {
            html += `<span class="event-selected-item" data-id="${ev.id}">${ev.title}</span>`;
        });
        html += `</div>`;

        box.innerHTML = html;

        loadOrderList();
    }

    //  載入訂單
    async function loadOrderList() {
        if (selectedEvents.length === 0) return;

        const keyword = document.getElementById("orderSearchInput")?.value.trim() || "";
        const tbody = document.getElementById("orderTableBody");
        if (!tbody) return;

        let params = new URLSearchParams();
        selectedEvents.forEach(ev => params.append("eventIds", ev.id));
        if (keyword) params.append("keyword", keyword);

        const resp = await fetch(`/api/orders?${params.toString()}`);
        const list = await resp.json();

        let html = "";
        list.forEach(o => {
            html += `
                <tr>
                    <td>${o.orderId}</td>
                    <td>${o.createdAt}</td>
                    <td>${o.buyerName}</td>
                    <td>${o.eventTitle}</td>
                    <td>${o.ticketCount}</td>
                    <td>${o.status}</td>
                    <td><button class="events-btn events-btn-secondary order-detail-btn" data-id="${o.orderId}">詳情</button></td>
                </tr>
            `;
        });

        tbody.innerHTML = html;
    }

    //  顯示訂單詳情
    async function showOrderDetail(orderId) {
        const modal = document.getElementById("orderDetailModal");
        const tbody = document.getElementById("orderDetailTbody");
        const totalEl = document.getElementById("orderDetailTotal");

        const resp = await fetch(`/api/orders/detail/${orderId}`);
        const list = await resp.json();

        let html = "";
        let total = 0;

        list.forEach(i => {
            const subtotal = i.quantity * i.unitPrice;
            total += subtotal;
            html += `
                <tr>
                    <td>${i.ticketName}</td>
                    <td>${i.quantity}</td>
                    <td>${i.unitPrice}</td>
                    <td>${i.subtotal}</td>
                </tr>
            `;
        });

        tbody.innerHTML = html;
        totalEl.innerText = total;

        modal.style.display = "flex";
    }

    //  綁定事件
    function bindEvents() {

        // 主畫面 → 開啟活動選擇 modal
        document.getElementById("selectedEventBox")?.addEventListener("click", (e) => {
            if (e.target.id === "openEventModalBtn" ||
                e.target.classList.contains("event-selected-item")) {
                document.getElementById("eventModal").style.display = "flex";
                fetchEvents();
            }
        });

        //  活動 modal 關閉
        document.getElementById("eventModalClose")?.addEventListener("click", () => {
            document.getElementById("eventModal").style.display = "none";
        });

        // modal 外層點擊關閉
        document.getElementById("eventModal")?.addEventListener("click", (e) => {
            if (e.target.id === "eventModal") {
                e.target.style.display = "none";
            }
        });

        // 搜尋活動
        document.getElementById("eventSearchInput")?.addEventListener("input", (e) => {
            fetchEvents(e.target.value.trim());
        });

        // 顯示更多
        document.getElementById("eventShowMore")?.addEventListener("click", () => {
            visibleCount += 10;
            renderEventList();
        });

        // 勾選活動 checkbox
        document.getElementById("eventList")?.addEventListener("change", (e) => {
            if (!e.target.classList.contains("event-checkbox")) return;

            const id = Number(e.target.dataset.id);
            const title = e.target.dataset.title;

            if (e.target.checked) {
                if (!selectedEvents.some(ev => ev.id === id)) {
                    selectedEvents.push({ id, title });
                }
            } else {
                selectedEvents = selectedEvents.filter(ev => ev.id !== id);
            }

            saveState();
        });

        // 套用活動選擇
        document.getElementById("eventModalApply")?.addEventListener("click", () => {
            document.getElementById("eventModal").style.display = "none";
            renderSelectedEvents();
        });

        // 清空活動
        document.getElementById("clearEventSelectionBtn")?.addEventListener("click", () => {
            selectedEvents = [];
            saveState();
            renderEventList();
            renderSelectedEvents();
        });

        // 搜尋訂單
        document.getElementById("orderSearchBtn")?.addEventListener("click", loadOrderList);

        //  新增：事件代理方式處理「詳情」按鈕
        document.getElementById("orderTableBody")?.addEventListener("click", (e) => {
            if (e.target.classList.contains("order-detail-btn")) {
                const orderId = e.target.dataset.id;
                showOrderDetail(orderId);
            }
        });

        // 訂單詳情 modal 關閉
        document.addEventListener("click", (e) => {
            if (e.target.id === "orderDetailClose") {
                document.getElementById("orderDetailModal").style.display = "none";
            }
        });

    }

    //  啟動流程
    loadSavedState();
    bindEvents();
    renderSelectedEvents();
}
