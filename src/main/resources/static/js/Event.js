let editingEventId = null;
let editingTicketId = null;

// 查詢與排序狀態
let eventQuery = {
    page: 1,
    size: 10,
    keyword: "",
    sort: "createdAt",
};

function initEvent() {
    initEventFormSubmit();
    initTicketDropdownToggle();
    initTicketTypeLoader();
    initTicketFormSubmit()
    initLimitQuantityToggle();
    initTabSwitching();
    loadEventList();
    initEventSearch();
    initEventSortButtons();
    initCancelEditButton();
    loadTicketTemplates();
    loadMyTickets();
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
            if (!checkbox || !checkbox.checked) return;

            // 早鳥票欄位
            const ebCheckbox = row.querySelector(".early-bird-checkbox");
            const ebDaysInput = row.querySelector(".early-bird-days");
            const ebDiscountInput = row.querySelector(".early-bird-discount");

            selectedTickets.push({
                ticketTemplateId: parseInt(checkbox.value, 10),

                customPrice: row.querySelector(".ticket-custom-price").value || null,
                customLimit: row.querySelector(".ticket-custom-limit").value || null,

                isEarlyBird: ebCheckbox ? ebCheckbox.checked : false,

                // 若沒 early bird 或沒有填值 → 預設值
                earlyBirdDays: ebDaysInput && ebDaysInput.value !== ""
                    ? parseInt(ebDaysInput.value, 10)
                    : 5,

                discountRate: ebDiscountInput && ebDiscountInput.value !== ""
                    ? parseInt(ebDiscountInput.value, 10)
                    : 85,
            });
        });

        formData.append("eventTicketsJson", JSON.stringify(selectedTickets));

        fetch(url, {
            method: method,
            body: formData,
        })
            .then(async (res) => {

                if (!res.ok) {
                    // 錯誤文字（通常是 HTML 或字串）
                    const text = await res.text();
                    throw new Error(text);
                }

                // 解析 JSON (新增/編輯成功情況）
                return res.json();
            })
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

// 編輯按鈕
function goEdit(id, btn) {

    if (editingEventId === id && btn.dataset.mode === "cancel") {

        // 1. 重置編輯狀態
        resetEventForm();
        editingEventId = null;

        // 2. 把所有 edit-btn 重設為「編輯」
        document.querySelectorAll(".edit-btn").forEach(b => {
            b.dataset.mode = "edit";
            b.textContent = "編輯";
            b.disabled = false;
            b.style.opacity = 1;
            b.style.cursor = "pointer";
        });

        // 3. 隱藏表單內的取消按鈕
        document.getElementById("eventCancelBtn").style.display = "none";

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

            document.getElementById("eventCancelBtn").style.display = "inline-block";
            document.getElementById("cover").value = "";

            // 填入表單欄位
            document.getElementById("title").value = ev.title || "";
            document.getElementById("address").value = ev.address || "";
            document.getElementById("eventStart").value = formatDatetimeLocal(ev.eventStart);
            document.getElementById("eventEnd").value = formatDatetimeLocal(ev.eventEnd);
            document.getElementById("ticketStart").value = formatDatetimeLocal(ev.ticketStart);
            document.getElementById("description").value = ev.description || "";
            document.getElementById("cover").addEventListener("change", function () {
                const file = this.files[0];
                updateCoverPreview(file || null);
            });

            if (ev.images && ev.images.length > 0) {
                updateCoverPreview(ev.images[0].imageUrl);
            } else {
                updateCoverPreview(null);
            }

            // 清空所有票種勾選與欄位
            clearAllTicketRows()

            // 還原已選票種
            ev.selectedTickets?.forEach(t => {
                const row = document.querySelector(`#ticketRow_${t.ticketTemplateId}`);
                if (!row) return;

                // 勾選啟用
                row.querySelector(".ticket-checkbox").checked = true;

                // 回填價格
                row.querySelector(".ticket-custom-price").value = t.customPrice ?? "";

                // 回填限量
                if (t.customLimit != null && t.customLimit > 0) {
                    row.querySelector(".ticket-custom-limit").value = t.customLimit;
                } else {
                    row.querySelector(".ticket-custom-limit").value = "";
                }

                // 回填早鳥設定
                const earlyBirdCheckbox = row.querySelector(".early-bird-checkbox");

                // 1. 設定早鳥啟用狀態
                earlyBirdCheckbox.checked = t.isEarlyBird === true;

                // 2. 立即觸發 change()，讓欄位生成
                earlyBirdCheckbox.dispatchEvent(new Event("change"));

                // 3. 欄位建立後再抓
                const earlyBirdDaysInput = row.querySelector(".early-bird-days");
                const discountRateInput = row.querySelector(".early-bird-discount");

                // 4. 回填天數
                if (earlyBirdDaysInput) {
                    earlyBirdDaysInput.value = t.earlyBirdDays ?? "";
                }

                // 5. 回填折扣
                if (discountRateInput) {
                    discountRateInput.value = t.discountRate ?? "";
                }
            });

            // 改送出按鈕文字
            const submitBtn = document.querySelector("#eventForm button[type='submit']");
            submitBtn.textContent = "確認編輯";

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
        d.setMinutes(0);
        d.setSeconds(0);
        d.setMilliseconds(0);
        // yyyy-MM-ddTHH:00
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

// 活動區塊的票種載入
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
                            <th style="border:1px solid #ccc; padding:6px;">描述</th>
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

                <td style="border:1px solid #ccc; padding:6px;">
                    <span>${ticket.description ?? ""}</span>
                </td>
                <td><input type="checkbox" class="early-bird-checkbox"></td>

                <td class="eb-days-cell"></td>
                <td class="eb-discount-cell"></td>
                <td class="eb-final-cell"></td>

            `;

                tbody.appendChild(row);
                earlyBirdForm(row);
            });
        });
}

// 顯示勾選早鳥票後出現的欄位方法
function earlyBirdForm(row) {
    const ebCheck = row.querySelector(".early-bird-checkbox");
    const ebDaysCell = row.querySelector(".eb-days-cell");
    const ebDiscountCell = row.querySelector(".eb-discount-cell");
    const ebFinalCell = row.querySelector(".eb-final-cell");

    ebCheck.addEventListener("change", () => {
        if (ebCheck.checked) {
            // 顯示三個 input 欄位
            ebDaysCell.innerHTML = `
            <input type="number" class="early-bird-days" placeholder="天數" style="width:60px;">
        `;
            ebDiscountCell.innerHTML = `
            <input type="number" class="early-bird-discount" placeholder="1~99" style="width:60px;">
        `;
            ebFinalCell.innerHTML = `
            <input type="text" class="early-bird-final-price" readonly style="width:80px; background:#eee;">
        `;
            // 取得剛插入的元素
            const ebDays = ebDaysCell.querySelector(".early-bird-days");
            const ebDiscount = ebDiscountCell.querySelector(".early-bird-discount");
            const ebFinal = ebFinalCell.querySelector(".early-bird-final-price");
            const priceInput = row.querySelector(".ticket-custom-price");

            // 綁定價格計算
            const calculate = () => {
                const basePrice = parseFloat(priceInput.value || priceInput.placeholder);
                const rate = parseFloat(ebDiscount.value);
                if (!rate || rate < 1 || rate > 99) {
                    ebFinal.value = "";
                    return;
                }

                const discountRate = rate / 100;
                ebFinal.value = Math.round(basePrice * discountRate);
            };
            ebDiscount.addEventListener("input", calculate);
            priceInput.addEventListener("input", calculate);
        } else {
            // 不啟用 → 清空欄位
            ebDaysCell.innerHTML = "";
            ebDiscountCell.innerHTML = "";
            ebFinalCell.innerHTML = "";
        }
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

        let url = "/api/tickets";
        let method = "POST";
        let isEdit = false;
        if (editingTicketId !== null) {
            url = `/api/tickets/${editingTicketId}`;
            method = "PUT";
            isEdit = true;
        }

        fetch(url, {
            method: method,
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
                alert(
                    isEdit
                        ? `票種「${data.name ?? ""}」已更新！`
                        : `票種「${data.name}」已新增！`
                );
                resetTicketForm(); // 重置表單
                initTicketTypeLoader(); // 重新載入活動用票種
                loadTicketTemplates();  // 重新載入票種模板
                loadMyTickets(); // 重新載入自訂票種
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
        history.pushState(null, "", "/organizer/dashboard/event");
        showTab("event");
    });

    document.getElementById("ticketTabBtn").addEventListener("click", () => {
        history.pushState(null, "", "/organizer/dashboard/event/ticket");
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

// 清空活動表單方法
function resetEventForm() {

    editingEventId = null;

    const form = document.getElementById("eventForm");
    form.reset();

    document.getElementById("cover").value = "";

    // 清空 TinyMCE
    // if (tinyMCE.get("description")) {
    //     tinyMCE.get("description").setContent("");
    // }

    // 清空封面 preview + 檔名
    updateCoverPreview(null);

    clearAllTicketRows()

    // 重設提交按鈕
    const submitBtn = document.querySelector("#eventForm button[type='submit']");
    submitBtn.textContent = "發佈活動";
    document.getElementById("eventCancelBtn").style.display = "none";

}

function clearAllTicketRows() {
    document.querySelectorAll("#ticketSelectTbody tr").forEach(row => {
        row.querySelector(".ticket-checkbox").checked = false;
        row.querySelector(".ticket-custom-price").value = "";
        row.querySelector(".ticket-custom-limit").value = "";

        // Early bird 清空
        row.querySelector(".early-bird-checkbox").checked = false;
        row.querySelector(".eb-days-cell").innerHTML = "";
        row.querySelector(".eb-discount-cell").innerHTML = "";
        row.querySelector(".eb-final-cell").innerHTML = "";
    });
}

//載入活動表單
function loadEventList() {
    const container = document.getElementById("eventListContainer");
    if (!container) return;

    const url = `/api/events`
        + `?keyword=${encodeURIComponent(eventQuery.keyword)}`
        + `&page=${eventQuery.page}`
        + `&size=${eventQuery.size}`
        + `&sort=${eventQuery.sort}`;

    fetch(url)
        .then(res => res.json())
        .then(data => {

            const events = data.content;

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
                const canEdit = !(status === "已結束" || status === "已取消");
                const canCancel = (status === "未開放");

                let actionButtons = "";

                if (canEdit) {
                    actionButtons += `<button class="edit-btn" onclick="goEdit(${ev.id}, this)">編輯</button>`;
                } else {
                    actionButtons += `<button class="edit-btn" disabled style="opacity:0.4; cursor:not-allowed;">編輯</button>`;
                }

                if (canCancel) {
                    actionButtons += `
                        <button class="cancel-btn"
                                onclick="cancelEvent(${ev.id})"
                                style="color:red; margin-left:6px;">
                            取消活動
                        </button>`;
                }

                html += `
                    <tr style="${(status === '已結束' || status === '已取消') ? 'opacity:0.5;' : ''}">
                        <td>${ev.title}</td>
                        <td>${ev.eventStart ?? "未設定"}</td>
                        <td>${ev.eventEnd ?? "未設定"}</td>
                        <td>${ev.ticketStart ?? "未設定"}</td>
                        <td>${status}</td>
                        <td>${ev.views ?? 0}</td>
                        <td>${ev.ticketsSold ?? 0}</td>
                        <td>${actionButtons}</td>
                    </tr>
                `;
            });

            html += "</tbody></table>";
            container.innerHTML = html;
            renderPagination(data);
        })
        .catch(err => {
            console.error("載入活動失敗：", err);
            container.innerHTML = "<p style='color:red;'>活動列表載入失敗。</p>";
        });
}

function updateCoverPreview(fileOrUrl) {
    const preview = document.getElementById("coverPreview");
    const filenameSpan = document.getElementById("coverFilename");

    if (!fileOrUrl) {
        filenameSpan.textContent = "（尚未選擇）";
        preview.src = "";
        preview.style.display = "none";
        return;
    }

    if (typeof fileOrUrl === "string") {
        // 是 URL
        const parts = fileOrUrl.split("/");
        const filename = parts[parts.length - 1];
        filenameSpan.textContent = filename;
        preview.src = fileOrUrl;
    } else {
        // 是 File
        filenameSpan.textContent = fileOrUrl.name;
        preview.src = URL.createObjectURL(fileOrUrl);
    }

    preview.style.display = "block";
}

function initCancelEditButton() {
    const cancelBtn = document.getElementById("eventCancelBtn");
    cancelBtn.addEventListener("click", () => {
        resetEventForm();
        editingEventId = null;

        // 把列表中的「取消」改回「編輯」
        document.querySelectorAll(".edit-btn").forEach(b => {
            b.dataset.mode = "edit";
            b.textContent = "編輯";
        });

        cancelBtn.style.display = "none";
    });
}

function cancelEvent(id) {
    if (!confirm("確定要取消這個活動嗎？\n活動取消後將不可再編輯！")) return;

    fetch(`/api/events/${id}/cancel`, {
        method: "PUT"
    })
        .then(res => {
            if (!res.ok) throw new Error("取消失敗");
            return res.text();
        })
        .then(() => {
            alert("活動已取消！");
            loadEventList();  // 重新載入
            resetEventForm(); // 若正在編輯，也重置
        })
        .catch(err => {
            console.error("取消活動錯誤:", err);
            alert("取消活動失敗！");
        });
}

function initEventSearch() {
    const search = document.getElementById("eventSearch");
    if (!search) return;

    let timer = null;

    search.addEventListener("keyup", () => {
        clearTimeout(timer);

        timer = setTimeout(() => {
            eventQuery.keyword = search.value.trim();
            eventQuery.page = 1;
            loadEventList();
        }, 300);
    });
}

function initEventSortButtons() {
    const sortCreateBtn = document.getElementById("sortCreateBtn");
    const sortEventBtn = document.getElementById("sortEventBtn");

    sortCreateBtn.addEventListener("click", () => {
        eventQuery.sort = "createdAt";
        eventQuery.page = 1;
        loadEventList();
        highlightSortButton();
    });

    sortEventBtn.addEventListener("click", () => {
        eventQuery.sort = "eventStart";
        eventQuery.page = 1;
        loadEventList();
        highlightSortButton();
    });
}

function highlightSortButton() {
    document.getElementById("sortCreateBtn").classList.remove("active");
    document.getElementById("sortEventBtn").classList.remove("active");

    if (eventQuery.sort === "createdAt") {
        document.getElementById("sortCreateBtn").classList.add("active");
    } else {
        document.getElementById("sortEventBtn").classList.add("active");
    }
}

function renderPagination(data) {
    const container = document.getElementById("pagination");
    if (!container) return;

    let html = "";

    if (!data.first) {
        html += `<button onclick="gotoPage(${eventQuery.page - 1})">上一頁</button>`;
    }

    for (let i = 1; i <= data.totalPages; i++) {
        html += `<button onclick="gotoPage(${i})" ${i === eventQuery.page ? "style='font-weight:bold;'" : ""}>${i}</button>`;
    }

    if (!data.last) {
        html += `<button onclick="gotoPage(${eventQuery.page + 1})">下一頁</button>`;
    }

    container.innerHTML = html;
}

function gotoPage(p) {
    eventQuery.page = p;
    loadEventList();
}

function loadTicketTemplates() {
    fetch("/api/tickets/templates")
        .then(res => res.json())
        .then(data => {
            const tbody = document.getElementById("templateTableBody");
            tbody.innerHTML = "";

            data.forEach(t => {
                // 用 data-ticket 存一整個 JSON(用單引號 escape 一下避免炸掉）
                const ticketJson = JSON
                    .stringify(t)
                    .replace(/'/g, "&#39;");

                tbody.innerHTML += `
                    <tr data-ticket='${ticketJson}'>
                        <td>${t.name}</td>
                        <td>${t.price}</td>
                        <td>${t.isLimited ? "是" : "否"}</td>
                        <td>${t.limitQuantity ?? "-"}</td>
                        <td>${t.description ?? ""}</td>
                        <td>
                            <button type="button" onclick="applyTemplate(this)">
                                套用
                            </button>
                        </td>
                    </tr>
                `;
            });
        });
}

function applyTemplate(button) {
    const tr = button.closest("tr");
    if (!tr) return;

    // 取回剛剛塞在 data-ticket 裡的 JSON
    const raw = tr.dataset.ticket;
    if (!raw) return;

    const t = JSON.parse(raw.replace(/&#39;/g, "'"));

    const form = document.getElementById("ticketForm");
    if (!form) return;

    // 1. 填基本欄位
    form.ticketName.value = t.name ?? "";
    form.ticketPrice.value = t.price ?? "";

    // 2. 是否限量 + 顯示 / 隱藏限量輸入框
    const isLimitedSelect = form.isLimited;
    const limitContainer = document.getElementById("limitQuantityContainer");
    const limitInput = form.limitQuantity;

    if (t.isLimited) {
        isLimitedSelect.value = "true";
        if (limitContainer) limitContainer.style.display = "block";
        if (limitInput) limitInput.value = t.limitQuantity ?? "";
    } else {
        isLimitedSelect.value = "false";
        if (limitContainer) limitContainer.style.display = "none";
        if (limitInput) limitInput.value = "";
    }

    // 3. 描述
    form.ticketDescription.value = t.description ?? "";

}


// 載入自訂表單列表
function loadMyTickets() {
    fetch("/api/tickets/my")
        .then(res => res.json())
        .then(data => {
            const tbody = document.getElementById("customTableBody");
            tbody.innerHTML = "";

            let index = 1;
            data.forEach(t => {
                tbody.innerHTML += `
                    <tr>
                        <td>${index++}</td>
                        <td>${t.name}</td>
                        <td>${t.price}</td>
                        <td>${t.isLimited ? "是" : "否"}</td>
                        <td>${t.limitQuantity ?? "-"}</td>
                        <td>${t.description ?? ""}</td>
                        <td>
                            <button class="ticket-edit-btn"
                                    data-mode="edit"
                                    onclick="editTicket(${t.id}, this)">
                                編輯
                            </button>
                        </td>
                        <td><button onclick="deleteTicket(${t.id})">刪除</button></td>
                    </tr>
                `;
            });
        });
}

function deleteTicket(id) {
    if (!confirm("確定要刪除此票種嗎？")) return;

    fetch(`/api/tickets/${id}`, {
        method: "DELETE",
        headers: {
            "Authorization": "Bearer " + localStorage.getItem("token"),
        },
    })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                alert("刪除成功");
                if (editingTicketId === id) {
                    resetTicketForm();
                }

                loadMyTickets();  // 刷新列表
            } else {
                alert(data.message || "刪除失敗");
            }
        });
}

// 編輯票種
function editTicket(id, btn) {

    // 如果已經在編輯這一筆，而且按鈕現在是「取消」→ 直接還原
    if (editingTicketId === id && btn.dataset.mode === "cancel") {
        resetTicketForm();
        return;
    }

    // 先把其他票種的按鈕恢復成「編輯」
    document.querySelectorAll(".ticket-edit-btn").forEach(b => {
        b.dataset.mode = "edit";
        b.textContent = "編輯";
    });

    // 這顆按鈕切換成「取消」
    btn.dataset.mode = "cancel";
    btn.textContent = "取消";

    fetch(`/api/tickets/${id}`)
        .then(res => res.json())
        .then(t => {
            editingTicketId = t.id;

            const form = document.getElementById("ticketForm");
            if (!form) return;

            form.ticketName.value = t.name ?? "";
            form.ticketPrice.value = t.price ?? "";

            const limitContainer = document.getElementById("limitQuantityContainer");

            if (t.isLimited) {
                form.isLimited.value = "true";
                if (limitContainer) limitContainer.style.display = "block";
                form.limitQuantity.value = t.limitQuantity ?? "";
            } else {
                form.isLimited.value = "false";
                if (limitContainer) limitContainer.style.display = "none";
                form.limitQuantity.value = "";
            }

            form.ticketDescription.value = t.description ?? "";

            // 送出按鈕文字改成「確認編輯」
            const submitBtn = document.querySelector("#ticketForm button[type='submit']");
            if (submitBtn) {
                submitBtn.textContent = "確認編輯";
            }

            // 自動切換到「票種」分頁
            showTab("ticket");
        })
        .catch(err => {
            console.error("載入票種失敗：", err);
            alert("無法載入票種資料");
        });
}

// 取消票種
function cancelEditTicket(btn) {
    resetTicketForm();

    editingTicketId = null;

    btn.textContent = "編輯";
    btn.onclick = () => editTicketForm(btn.dataset.id, btn);
}

function resetTicketForm() {
    const form = document.getElementById("ticketForm");
    if (form) {
        form.reset();
    }

    // 限量區塊收起來
    const limitContainer = document.getElementById("limitQuantityContainer");
    if (limitContainer) {
        limitContainer.style.display = "none";
    }

    // 狀態回到「新增模式」
    editingTicketId = null;

    // 送出按鈕文字改回「新增票種」
    const submitBtn = document.querySelector("#ticketForm button[type='submit']");
    if (submitBtn) {
        submitBtn.textContent = "新增票種";
    }

    // 把列表中的編輯按鈕都改回「編輯」
    document.querySelectorAll(".ticket-edit-btn").forEach(b => {
        b.dataset.mode = "edit";
        b.textContent = "編輯";
    });
}

