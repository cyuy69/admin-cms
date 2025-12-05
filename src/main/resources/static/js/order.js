document.addEventListener("DOMContentLoaded", () => {

    let selectedEventId = null;
    let selectedEvents = [];
    let visibleCount = 10;
    let allOrderEvents = [];

    /* 選擇活動 */
    document.addEventListener("click", (e) => {
        if (e.target && e.target.id === "openEventModalBtn") {
            document.getElementById("eventModal").style.display = "flex";
            loadOrderEventList();
        }
    });

    /* 關閉 modal */
    document.addEventListener("click", (e) => {
        if (e.target.id === "eventModalClose") {
            document.getElementById("eventModal").style.display = "none";
        }
    });


    /* 搜尋活動 */
    document.addEventListener("input", (e) => {
        if (e.target.id === "eventSearchInput") {
            loadOrderEventList(e.target.value.trim());
        }
    });

    /* 載入活動列表 */
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
            html += `
                <label class="event-item">
                    <input type="checkbox"
                           class="event-checkbox"
                           data-id="${ev.id}"
                           data-title="${ev.title}">
                    <span>${ev.title}</span>
                </label>
            `;
        });

        listEl.innerHTML = html;

        moreEl.style.display = visibleCount < allOrderEvents.length
            ? "block"
            : "none";
    }

    /* 套用活動選取 */
    document.addEventListener("click", (e) => {
        if (e.target.id === "eventModalApply") {

            const checks = document.querySelectorAll(".event-checkbox:checked");
            selectedEvents = [];

            checks.forEach(chk => {
                selectedEvents.push({
                    id: Number(chk.dataset.id),
                    title: chk.dataset.title
                });
            });

            document.getElementById("eventModal").style.display = "none";
            renderSelectedEvents();
        }
    });

    /* 顯示更多 */
    document.addEventListener("click", (e) => {
        if (e.target.id === "eventShowMore") {
            visibleCount += 10;
            renderOrderEventList();
        }
    });

    /* 查詢訂單 */
    document.getElementById("orderSearchBtn").addEventListener("click", () => {
        loadOrderList();
    });

    function loadOrderList() {

        if (!selectedEventId) return;

        let keyword = document.getElementById("orderSearchInput").value.trim();

        fetch(`/api/orders?eventId=${selectedEventId}&keyword=${keyword}`)
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
        alert("TODO: 未來做詳細訂單內容 orderId=" + orderId);
    }

    /* 顯示已選活動 */
    function renderSelectedEvents() {
        const box = document.getElementById("selectedEventBox");

        // 沒選 → 顯示預設畫面
        if (selectedEvents.length === 0) {
            box.innerHTML = `
                <button id="openEventModalBtn" class="select-event-btn">選擇活動</button>
                <div class="placeholder">(尚未選擇活動)</div>
            `;
            selectedEventId = null;
            return;
        }

        // 有選 → 顯示活動清單
        selectedEventId = selectedEvents[0].id;

        let html = "<div class='event-selected-list'>";
        selectedEvents.forEach(ev => {
            html += `
                <span class="event-selected-item" onclick="switchEvent(${ev.id})">
                    ${ev.title}
                </span>
            `;
        });
        html += "</div>";

        box.innerHTML = html;

        loadOrderList();
    }

    window.switchEvent = function (eventId) {
        selectedEventId = eventId;
        loadOrderList();
    };

});
