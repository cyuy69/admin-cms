function initOrders() {

    if (window.orderPageLoaded) return;
    window.orderPageLoaded = true;

    let selectedEvents = [];
    let allOrderEvents = [];
    let visibleCount = 10;
    let hasRendered = false;

    // 保存活動選項
    function saveSelectedEvents() {
        sessionStorage.setItem("selectedEvents", JSON.stringify(selectedEvents));
    }

    function loadSelectedEvents() {
        const saved = sessionStorage.getItem("selectedEvents");
        if (saved) {
            selectedEvents = JSON.parse(saved);
        }
    }

    /* 打開活動選單 */
    document.addEventListener("click", (e) => {
        if (e.target.closest("#openEventModalBtn")) {
            document.getElementById("eventModal").style.display = "flex";
            loadOrderEventList();
        }
    });

    /* 關閉 modal */
    document.addEventListener("click", (e) => {
        if (e.target.closest("#eventModalClose")) {
            document.getElementById("eventModal").style.display = "none";
        }
    });

    /* 搜尋活動 */
    document.addEventListener("input", (e) => {
        if (e.target.id === "eventSearchInput") {
            loadOrderEventList(e.target.value.trim());
        }
    });

    function loadOrderEventList(keyword = "") {
        fetch(`/api/events/my?keyword=${keyword}`)
            .then(resp => resp.json())
            .then(list => {
                allOrderEvents = list;
                visibleCount = 10;
                renderOrderEventList();
            });
    }

    function renderOrderEventList() {

        const listEl = document.getElementById("eventList");
        const moreEl = document.getElementById("eventShowMore");
        const slice = allOrderEvents.slice(0, visibleCount);

        let html = "";
        slice.forEach(ev => {

            const checked = selectedEvents.some(se => se.id === ev.id) ? "checked" : "";

            html += `
            <label class="event-item">
                <input type="checkbox"
                        class="event-checkbox"
                        data-id="${ev.id}"
                        data-title="${ev.title}"
                        ${checked}>
                <span>${ev.title}</span>
            </label>
        `;
        });

        listEl.innerHTML = html;

        moreEl.style.display = visibleCount < allOrderEvents.length
            ? "block"
            : "none";
    }

    /* 套用活動按鈕 */
    document.addEventListener("click", (e) => {
        if (e.target.closest("#eventModalApply")) {

            const checks = document.querySelectorAll(".event-checkbox:checked");
            selectedEvents = [];

            checks.forEach(chk => {
                selectedEvents.push({
                    id: Number(chk.dataset.id),
                    title: chk.dataset.title
                });
            });
            saveSelectedEvents();

            document.getElementById("eventModal").style.display = "none";
            renderSelectedEvents();
        }
    });

    /* 清空全部選擇按鈕 */
    document.addEventListener("click", (e) => {
        if (e.target.closest("#clearEventSelectionBtn")) {

            // 清掉陣列
            selectedEvents = [];
            saveSelectedEvents();

            // 全部 checkbox 取消
            document.querySelectorAll(".event-checkbox").forEach(c => {
                c.checked = false;
            });

            // 重新更新 UI
            renderOrderEventList();
            renderSelectedEvents();

            // 清除訂單表格
            const tbody = document.getElementById("orderTableBody");
            if (tbody) tbody.innerHTML = "";
        }
    });


    /* 顯示更多 */
    document.addEventListener("click", (e) => {
        if (e.target.closest("#eventShowMore")) {
            visibleCount += 10;
            renderOrderEventList();
        }
    });

    // 避免點顯示更多時，checkbox沒同步的問題
    document.addEventListener("change", (e) => {
        if (e.target.classList.contains("event-checkbox")) {

            const id = Number(e.target.dataset.id);
            const title = e.target.dataset.title;

            if (e.target.checked) {
                // 加入（避免重複）
                if (!selectedEvents.some(ev => ev.id === id)) {
                    selectedEvents.push({ id, title });
                }
            } else {
                // 移除未勾選
                selectedEvents = selectedEvents.filter(ev => ev.id !== id);
            }

            saveSelectedEvents();
        }
    });


    /* 查詢訂單 */
    document.addEventListener("click", (e) => {
        if (e.target.closest("#orderSearchBtn")) {
            loadOrderList();
        }
    });

    function loadOrderList() {

        if (selectedEvents.length === 0) return;

        let keyword = document.getElementById("orderSearchInput").value.trim();
        let params = new URLSearchParams();
        selectedEvents.forEach(ev => params.append("eventIds", ev.id));

        if (keyword) params.append("keyword", keyword);

        fetch(`/api/orders?` + params.toString())
            .then(resp => resp.json())
            .then(list => {
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
                        <td><button onclick="showOrderDetail(${o.orderId})">...</button></td>
                    </tr>
                `;
                });
                document.getElementById("orderTableBody").innerHTML = html;
            });
    }

    function showOrderDetail(orderId) {

        fetch(`/api/orders/detail/${orderId}`)
            .then(resp => resp.json())
            .then(items => {

                const modal = document.getElementById("orderDetailModal");
                const tbody = document.getElementById("orderDetailTbody");
                const totalEl = document.getElementById("orderDetailTotal");

                let html = "";
                let total = 0;

                items.forEach(it => {
                    const qty = it.quantity ?? 0;
                    const unit = Number(it.unitPrice ?? 0);
                    const sub = Number(it.subtotal ?? unit * qty);

                    total += sub;

                    html += `
                    <tr>
                        <td>${it.ticketName}</td>
                        <td>${qty}</td>
                        <td>${unit.toFixed(2)}</td>
                        <td>${sub.toFixed(2)}</td>
                    </tr>
                `;
                });

                tbody.innerHTML = html || `
                <tr>
                    <td colspan="4">此訂單目前沒有任何票券資料</td>
                </tr>
            `;

                totalEl.textContent = total.toFixed(2);

                modal.style.display = "flex";
            });
    }

    // 讓 inline onclick 可以找到這個函式
    window.showOrderDetail = showOrderDetail;

    /* 顯示已選活動 */
    function renderSelectedEvents() {
        const box = document.getElementById("selectedEventBox");
        if (!box) return;
        if (selectedEvents.length === 0) {
            box.innerHTML = `
            <button id="openEventModalBtn" class="select-event-btn">選擇活動</button>
            <div class="placeholder">(尚未選擇活動)</div>
        `;

            const tbody = document.getElementById("orderTableBody");
            if (tbody) tbody.innerHTML = "";
            return;
        }

        let html = `<div class="event-selected-list">`;

        selectedEvents.forEach(ev => {
            html += `
            <span class="event-selected-item" data-id="${ev.id}">
                ${ev.title}
            </span>
        `;
        });

        html += `</div>`;
        box.innerHTML = html;

        loadOrderList();
    }

    loadSelectedEvents();
    renderSelectedEvents();

    const observer = new MutationObserver(() => {
        const box = document.getElementById("selectedEventBox");

        // 若找到 selectedEventBox 且還沒初始化過，就執行一次
        if (box && !hasRendered) {
            hasRendered = true;
            loadSelectedEvents();
            renderSelectedEvents();
            observer.disconnect();
        }
    });

    observer.observe(document.body, { childList: true, subtree: true });



    /* 切換活動 */
    document.addEventListener("click", (e) => {
        const item = e.target.closest(".event-selected-item");
        if (item) {
            document.getElementById("eventModal").style.display = "flex";
            loadOrderEventList(); // 重新載入活動清單
        }

        // 點右下關閉按鈕
        if (e.target.closest("#orderDetailClose")) {
            document.getElementById("orderDetailModal").style.display = "none";
        }

        // 點遮罩空白處也關閉（可選）
        if (e.target.id === "orderDetailModal") {
            document.getElementById("orderDetailModal").style.display = "none";
        }

    });

};
