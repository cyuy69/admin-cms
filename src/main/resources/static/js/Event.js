let editingEventId = null;
function initEvent() {
    initEventFormSubmit();
    initTicketDropdownToggle();
    initTicketTypeLoader();
    initTicketFormSubmit()
    initLimitQuantityToggle();
    initTabSwitching();
    loadTicketList();
    loadEventList();
}

// 活動建立送出按鈕初始化
function initEventFormSubmit() {
    const form = document.getElementById("eventForm");
    if (!form) return;

    const coverInput = document.getElementById("cover");
    if (coverInput) {
        coverInput.addEventListener("change", function () {
            const file = this.files[0];
            document.getElementById("coverFilename").textContent = file ? file.name : "（尚未選擇）";

            const preview = document.getElementById("coverPreview");
            if (file) {
                preview.src = URL.createObjectURL(file);
                preview.style.display = "block";
            } else {
                preview.src = "";
                preview.style.display = "none";
            }
        });
    }

    form.addEventListener("submit", function (e) {
        e.preventDefault();

        // 判斷新增 or 編輯（靠 editingEventId）
        const mode = editingEventId ? "edit" : "create";

        const url = editingEventId
            ? `/api/events/${editingEventId}`
            : "/api/events/create";

        const method = editingEventId ? "PUT" : "POST";

        const formData = new FormData(form);
        const selectedTickets = [];
        const rows = document.querySelectorAll("#ticketSelectTbody tr");

        rows.forEach((row) => {
            const checkbox = row.querySelector(".ticket-checkbox");
            if (!checkbox.checked) return;

            selectedTickets.push({
                ticketTemplateId: parseInt(checkbox.value, 10),
                customPrice: row.querySelector(".ticket-custom-price").value || null,
                customLimit: row.querySelector(".ticket-custom-limit").value || null,
            });
        });

        formData.append("eventTicketsJson", JSON.stringify(selectedTickets));

        fetch(url, {
            method: method,
            body: formData,
        })
            .then((res) => res.json())
            .then((data) => {
                alert(mode === "edit"
                    ? `活動「${data.title}」已更新！`
                    : `活動「${data.title}」已建立！`
                );

                resetEventForm();
                loadEventList();
            })
            .catch((err) => {
                console.error("失敗：", err);
                alert(mode === "edit" ? "活動更新失敗！" : "活動建立失敗！");
            });
    });
}

function goEdit(id, btn) {

    if (editingEventId === id && btn.dataset.mode === "cancel") {
        resetEventForm();
        editingEventId = null;

        btn.dataset.mode = "edit";
        btn.textContent = "編輯";
        return;
    }

    // 清空其他按鈕
    document.querySelectorAll(".edit-btn").forEach(b => {
        b.dataset.mode = "edit";
        b.textContent = "編輯";
    });

    // 設定這顆按鈕進入取消模式
    btn.dataset.mode = "cancel";
    btn.textContent = "取消";

    fetch(`/api/events/${id}`)
        .then(res => res.json())
        .then(ev => {
            editingEventId = ev.id;

            // 填入表單欄位
            document.getElementById("title").value = ev.title || "";
            document.getElementById("address").value = ev.address || "";
            document.getElementById("eventStart").value = formatDatetimeLocal(ev.eventStart);
            document.getElementById("eventEnd").value = formatDatetimeLocal(ev.eventEnd);
            document.getElementById("ticketStart").value = formatDatetimeLocal(ev.ticketStart);
            document.getElementById("description").value = ev.description || "";
            document.getElementById("cover").addEventListener("change", function () {
                const file = this.files[0];
                document.getElementById("coverFilename").textContent = file ? file.name : "（尚未選擇）";
            });

            const preview = document.getElementById("coverPreview");
            const filenameSpan = document.getElementById("coverFilename");

            if (ev.images && ev.images.length > 0) {
                const url = ev.images[0].imageUrl;
                const parts = url.split("/");
                const filename = parts[parts.length - 1];

                filenameSpan.textContent = filename;
                preview.src = url;
                preview.style.display = "block";
            } else {
                filenameSpan.textContent = "（尚未上傳）";
                preview.src = "";
                preview.style.display = "none";
            }

            // 清空所有票種勾選與欄位
            document.querySelectorAll("#ticketSelectTbody tr").forEach(row => {
                row.querySelector(".ticket-checkbox").checked = false;
                row.querySelector(".ticket-custom-price").value = "";
                row.querySelector(".ticket-custom-limit").value = "";
            });

            // 還原已選票種
            ev.selectedTickets?.forEach(t => {
                const row = document.querySelector(`#ticketRow_${t.ticketTemplateId}`);
                if (!row) return;

                row.querySelector(".ticket-checkbox").checked = true;
                row.querySelector(".ticket-custom-price").value = t.customPrice ?? "";
                row.querySelector(".ticket-custom-limit").value = t.customLimit ?? "";
            });

            // 改送出按鈕文字
            const btn = document.querySelector("#eventForm button[type='submit']");
            btn.textContent = "確認編輯";

            // 切換到 event tab
            showTab("event");
        })
        .catch(err => {
            console.error("載入活動失敗：", err);
            alert("無法載入活動資料");
        });


    function formatDatetimeLocal(raw) {
        if (!raw) return "";
        const d = new Date(raw);
        return d.toISOString().slice(0, 16);
    }
}

// 下拉選單初始化
function initTicketDropdownToggle() {
    const toggleBtn = document.getElementById("ticketDropdownToggleBtn");
    const dropdown = document.getElementById("ticketDropdown");

    if (toggleBtn && dropdown) {
        toggleBtn.addEventListener("click", function () {
            dropdown.style.display =
                dropdown.style.display === "block" ? "none" : "block";
        });
    }
}

// 票種載入變成表格格式
function initTicketTypeLoader() {
    const dropdown = document.getElementById("ticketDropdown");
    if (!dropdown) return;

    fetch("/api/tickets/for-event")
        .then((res) => res.json())
        .then((ticketTypes) => {

            // 生成表格標頭
            dropdown.innerHTML = `
                <table class="ticket-select-table" style="width:100%; border-collapse: collapse;">
                    <thead>
                        <tr>
                            <th style="border:1px solid #ccc; padding:6px;">啟用</th>
                            <th style="border:1px solid #ccc; padding:6px;">票種名稱</th>
                            <th style="border:1px solid #ccc; padding:6px;">活動票價</th>
                            <th style="border:1px solid #ccc; padding:6px;">活動限量</th>
                        </tr>
                    </thead>
                    <tbody id="ticketSelectTbody"></tbody>
                </table>
            `;

            const tbody = document.getElementById("ticketSelectTbody");

            ticketTypes.forEach((ticket) => {
                const row = document.createElement("tr");
                row.id = `ticketRow_${ticket.id}`;

                row.innerHTML = `
                <td style="border:1px solid #ccc; padding:6px; text-align:center;">
                    <input type="checkbox" class="ticket-checkbox" value="${ticket.id}">
                </td>

                <td style="border:1px solid #ccc; padding:6px;">
                    ${ticket.name}
                    ${ticket.isDefault ? "(自訂)" : ""}
                    <br>
                </td>

                <td style="border:1px solid #ccc; padding:6px; text-align:center;">
                    <input
                        type="number"
                        class="ticket-custom-price"
                        placeholder="${ticket.price}"
                        style="width:80px;">
                </td>

                <td style="border:1px solid #ccc; padding:6px; text-align:center;">
                    <input
                        type="number"
                        class="ticket-custom-limit"
                        placeholder="${ticket.isLimited ? ticket.limitQuantity : ''}"
                        style="width:80px;">
                </td>
            `;

                tbody.appendChild(row);
            });
        });
}


function initLimitQuantityToggle() {
    const isLimitedSelect = document.getElementById("isLimited");
    const limitQuantityContainer = document.getElementById("limitQuantityContainer");

    if (isLimitedSelect && limitQuantityContainer) {
        isLimitedSelect.addEventListener("change", function () {
            limitQuantityContainer.style.display =
                this.value === "true" ? "block" : "none";
        });
    }
}

function initTicketFormSubmit() {
    const form = document.getElementById("ticketForm");
    if (!form) return;

    form.addEventListener("submit", function (e) {
        e.preventDefault();

        const ticketData = {
            name: form.ticketName.value,
            price: parseFloat(form.ticketPrice.value).toFixed(2), // 僅限 price 做浮點處理
            isLimited: form.isLimited.value === "true",
            limitQuantity:
                form.isLimited.value === "true"
                    ? parseInt(form.limitQuantity.value || "0", 10) //票數保持整數
                    : null,
            description: form.ticketDescription.value,
        };

        fetch("/api/tickets", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(ticketData),
        })
            .then((res) => {
                if (!res.ok) throw new Error("票種新增失敗");
                return res.json();
            })
            .then((data) => {
                alert(`票種「${data.name}」已新增！`);
                form.reset();
                document.getElementById("limitQuantityContainer").style.display = "none";
                initTicketTypeLoader(); // 重新載入票種選單
            })
            .catch((err) => {
                console.error("票種建立失敗：", err);
                alert("票種建立失敗！");
            });
    });
}

function showTab(tabName) {
    const eventTab = document.getElementById("eventTab");
    const ticketTab = document.getElementById("ticketTab");
    const eventTabBtn = document.getElementById("eventTabBtn");
    const ticketTabBtn = document.getElementById("ticketTabBtn");

    if (tabName === "ticket") {
        ticketTab.style.display = "block";
        eventTab.style.display = "none";
        ticketTabBtn.classList.add("active");
        eventTabBtn.classList.remove("active");
    } else {
        eventTab.style.display = "block";
        ticketTab.style.display = "none";
        eventTabBtn.classList.add("active");
        ticketTabBtn.classList.remove("active");
    }
}

function initTabSwitching() {
    document.getElementById("eventTabBtn").addEventListener("click", () => {
        history.pushState(null, "", "/admin/dashboard/event");
        showTab("event");
    });

    document.getElementById("ticketTabBtn").addEventListener("click", () => {
        history.pushState(null, "", "/admin/dashboard/event/ticket");
        showTab("ticket");
    });

    // 初次載入根據 URL 顯示對應 tab
    const path = location.pathname;
    if (path.endsWith("/ticket")) {
        showTab("ticket");
    } else {
        showTab("event");
    }

    // 支援瀏覽器返回鍵
    window.addEventListener("popstate", () => {
        const path = location.pathname;
        if (path.endsWith("/ticket")) {
            showTab("ticket");
        } else {
            showTab("event");
        }
    });
}

function loadTicketList() {
    const container = document.getElementById("ticketListContainer");
    if (!container) return;

    fetch("/api/tickets")   // 目前你的 GET /api/tickets 回傳全部票種 DTO
        .then(res => res.json())
        .then(list => {

            if (!list || list.length === 0) {
                container.innerHTML = "<p>目前沒有票種。</p>";
                return;
            }

            let html = `
                <table border="1" cellspacing="0" cellpadding="6">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>票種名稱</th>
                            <th>票價</th>
                            <th>是否限量</th>
                            <th>限量張數</th>
                            <th>描述</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

            list.forEach(t => {
                html += `
                    <tr>
                        <td>${t.id}</td>
                        <td>${t.name}</td>
                        <td>${t.price}</td>
                        <td>${t.isLimited ? "是" : "否"}</td>
                        <td>${t.limitQuantity ?? "-"}</td>
                        <td>${t.description ?? ""}</td>
                    </tr>
                `;
            });

            html += "</tbody></table>";
            container.innerHTML = html;
        })
        .catch(err => {
            console.error("票種載入錯誤：", err);
            container.innerHTML = "<p style='color:red;'>無法載入票種</p>";
        });
}

function resetEventForm() {

    editingEventId = null;

    const form = document.getElementById("eventForm");
    form.reset();

    // 清空 TinyMCE
    // if (tinyMCE.get("description")) {
    //     tinyMCE.get("description").setContent("");
    // }

    // 清空封面 preview + 檔名
    const preview = document.getElementById("coverPreview");
    const filenameSpan = document.getElementById("coverFilename");
    preview.src = "";
    preview.style.display = "none";
    filenameSpan.textContent = "（尚未選擇）";

    // 重設提交按鈕
    const submitBtn = document.querySelector("#eventForm button[type='submit']");
    submitBtn.textContent = "發佈活動";

}


//載入活動表單用的
function loadEventList() {
    const container = document.getElementById("eventListContainer");
    if (!container) return;

    fetch("/api/events")
        .then(res => res.json())
        .then(events => {

            if (!events || events.length === 0) {
                container.innerHTML = "<p>目前沒有活動。</p>";
                return;
            }

            let html = `
                <table border="1" cellspacing="0" cellpadding="6">
                    <thead>
                        <tr>
                            <th>活動名稱</th>
                            <th>開始時間</th>
                            <th>結束時間</th>
                            <th>售票日期</th>
                            <th>狀態</th>
                            <th>瀏覽</th>
                            <th>已售</th>
                            <td>編輯</td>
                        </tr>
                    </thead>
                    <tbody>
            `;

            events.forEach(ev => {
                const status = ev.status ?? "未設定";
                const isEditable = !(status === "已結束" || status === "已取消");

                const editButton = isEditable
                    ? `<button class="edit-btn" onclick="goEdit(${ev.id}, this)">編輯</button>`
                    : `<button class="edit-btn" disabled style="opacity:0.4; cursor:not-allowed;">編輯</button>`;

                html += `
        <tr>
            <td>${ev.title}</td>
            <td>${ev.eventStart ?? "未設定"}</td>
            <td>${ev.eventEnd ?? "未設定"}</td>
            <td>${ev.ticketStart ?? "未設定"}</td>
            <td>${status}</td>
            <td>${ev.views ?? 0}</td>
            <td>${ev.ticketsSold ?? 0}</td>
            <td>${editButton}</td>
        </tr>
    `;
            });


            html += "</tbody></table>";
            container.innerHTML = html;
        })
        .catch(err => {
            console.error("載入活動失敗：", err);
            container.innerHTML = "<p style='color:red;'>活動列表載入失敗。</p>";
        });
}
