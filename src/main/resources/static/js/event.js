let editingEventId = null;
let editingTicketId = null;
let editorInited = false;

// 查詢與排序狀態
let eventQuery = {
    page: 1,
    size: 10,
    keyword: "",
    sort: "createdAt",
    order: "desc"
};

function initEvent() {
    initEventDescriptionEditor();
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

    // --- 1. 初始化小時下拉選單 (00:00 ~ 23:00) ---
    const hourSelects = ["eventStartHour", "eventEndHour", "ticketStartHour"];
    hourSelects.forEach(id => {
        const select = document.getElementById(id);
        if (!select) return;
        for (let i = 0; i < 24; i++) {
            const hour = i.toString().padStart(2, '0');
            const option = document.createElement("option");
            option.value = `${hour}:00`;
            option.text = `${hour}:00`;
            select.appendChild(option);
        }
    });

    // --- 2. 設定日期限制 (不能選過去) ---
    const dateInputs = ["eventStartDate", "eventEndDate", "ticketStartDate"];
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;
    const today = new Date(now.getTime() - offset).toISOString().split("T")[0];

    dateInputs.forEach(id => {
        const input = document.getElementById(id);
        if (input) input.min = today;
    });

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

        const combineTime = (dateId, hourId, hiddenId) => {
            const dateVal = document.getElementById(dateId).value;
            const hourVal = document.getElementById(hourId).value;
            const hiddenInput = document.getElementById(hiddenId);
            if (dateVal && hourVal) {
                // 組合格式：YYYY-MM-DDTHH:mm
                hiddenInput.value = `${dateVal}T${hourVal}`;
            }
        };

        // 執行組合
        combineTime("eventStartDate", "eventStartHour", "eventStart");
        combineTime("eventEndDate", "eventEndHour", "eventEnd");
        combineTime("ticketStartDate", "ticketStartHour", "ticketStart");

        // --- 邏輯檢查 ---
        const startVal = document.getElementById("eventStart").value;
        const endVal = document.getElementById("eventEnd").value;

        if (!startVal || !endVal) {
            alert("請完整選擇日期與時間！");
            return;
        }

        const startDate = new Date(startVal);
        const endDate = new Date(endVal);
        const mode = editingEventId ? "edit" : "create";

        // 檢查：活動開始時間必須是「今天 + 7天」以後
        const limitDate = new Date();
        limitDate.setDate(limitDate.getDate() + 7);
        limitDate.setHours(0, 0, 0, 0);

        if (mode === "create") {
            const limitDate = new Date();
            limitDate.setDate(limitDate.getDate() + 7);
            limitDate.setHours(0, 0, 0, 0);

            if (startDate < limitDate) {
                alert("活動開始時間必須在「一週後」才能建立！");
                return;
            }
        }

        if (endDate <= startDate) {
            alert("活動結束時間必須晚於開始時間！");
            return;
        }

        if (mode === "edit") {
            const ticketStartVal = document.getElementById("ticketStart").value;
            if (ticketStartVal) {
                const ticketStartDate = new Date(ticketStartVal);
                const now = new Date();

                ticketStartDate.setSeconds(0, 0);

                if (ticketStartDate <= now) {
                    // 這裡可以選擇直接鎖定欄位，或是提示使用者
                    const ticketStartInput = document.getElementById("ticketStartDate");
                    if (ticketStartInput) ticketStartInput.disabled = true;

                    const ticketStartHourInput = document.getElementById("ticketStartHour");
                    if (ticketStartHourInput) ticketStartHourInput.disabled = true;
                }
            }
        }


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

    const targetId = Number(id);
    const currentEditingId = editingEventId ? Number(editingEventId) : null;

    if (currentEditingId !== null && currentEditingId === targetId) {
        // 取消編輯
        resetEventForm();
        editingEventId = null;
        return;
    }

    resetEventForm();
    editingEventId = targetId;

    document.querySelectorAll(".edit-btn").forEach(b => {
        b.textContent = "編輯";
        b.className = "events-btn events-btn-secondary edit-btn";
    });

    if (btn) {
        btn.textContent = "取消";
        btn.className = "events-btn events-btn-danger edit-btn"; // 變紅
    }

    fetch(`/api/events/${targetId}`)
        .then(res => res.json())
        .then(ev => {
            editingEventId = ev.id;

            document.getElementById("eventCancelBtn").style.display = "inline-block";
            document.getElementById("cover").value = "";

            // 填入表單欄位
            document.getElementById("title").value = ev.title || "";
            document.getElementById("address").value = ev.address || "";
            if (tinymce.get("description")) {
                tinymce.get("description").setContent(ev.description || "");
            } else {
                document.getElementById("description").value = ev.description || "";
            }

            // --- 時間回填 ---
            const fillDateTime = (isoString, dateId, hourId, hiddenId) => {
                if (!isoString) return;
                const d = new Date(isoString);

                // 轉成 YYYY-MM-DD
                const year = d.getFullYear();
                const month = String(d.getMonth() + 1).padStart(2, '0');
                const day = String(d.getDate()).padStart(2, '0');
                const dateStr = `${year}-${month}-${day}`;

                // 轉成 HH:00
                const hour = String(d.getHours()).padStart(2, '0');
                const timeStr = `${hour}:00`;

                // 填入顯示欄位
                const dateInput = document.getElementById(dateId);
                const hourInput = document.getElementById(hourId);
                const hiddenInput = document.getElementById(hiddenId);

                if (dateInput) dateInput.value = dateStr;
                if (hourInput) hourInput.value = timeStr;
                // 也要填入 hidden 欄位，以免沒改時間直接送出時變空的
                if (hiddenInput) hiddenInput.value = isoString;
            };

            fillDateTime(ev.eventStart, "eventStartDate", "eventStartHour", "eventStart");
            fillDateTime(ev.eventEnd, "eventEndDate", "eventEndHour", "eventEnd");
            fillDateTime(ev.ticketStart, "ticketStartDate", "ticketStartHour", "ticketStart");

            if (ev.ticketStart) {
                const ticketStartDate = new Date(ev.ticketStart);
                const now = new Date();
                if (ticketStartDate <= now) {
                    const ticketStartDateInput = document.getElementById("ticketStartDate");
                    const ticketStartHourInput = document.getElementById("ticketStartHour");
                    if (ticketStartDateInput) {
                        ticketStartDateInput.disabled = true;
                        ticketStartDateInput.removeAttribute("min");
                    }

                    if (ticketStartHourInput) ticketStartHourInput.disabled = true;
                }
            }

            // 展開下拉式選單
            const dropdown = document.getElementById("ticketDropdown");
            if (dropdown) dropdown.style.display = "block";

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
                earlyBirdCheckbox.checked = t.isEarlyBird === true;

                // 立即觸發 change()，讓欄位生成
                earlyBirdCheckbox.dispatchEvent(new Event("change"));

                // 欄位建立後再抓
                const earlyBirdDaysInput = row.querySelector(".early-bird-days");
                const discountRateInput = row.querySelector(".early-bird-discount");

                if (t.isEarlyBird) {
                    if (earlyBirdDaysInput) earlyBirdDaysInput.value = t.earlyBirdDays ?? "";
                    if (discountRateInput) {
                        discountRateInput.value = t.discountRate ?? "";
                        // 觸發 input 事件讓價格自動計算出來
                        discountRateInput.dispatchEvent(new Event("input"));
                    }
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
            editingEventId = null;
            loadEventList();
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

            <table class="events-table">
                    <thead>
                        <tr>
                            <th style="width:50px; text-align:center;">啟用</th>
                            <th>票種名稱</th>
                            <th style="width:100px;">活動票價</th>
                            <th style="width:100px;">活動限量</th>
                            <th>描述</th>
                            <th style="width:350px;">早鳥優惠設定</th>
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
                <td style="text-align:center;">
                    <input type="checkbox" class="ticket-checkbox" value="${ticket.id}" style="cursor:pointer; width:18px; height:18px; accent-color:#f07509;">
                </td>

                <td style="padding: 8px;">
                    <div class="events-ticket-name-wrapper">
                        <span class="events-ticket-name">
                            ${ticket.name} ${ticket.isDefault ? '<span style="font-size:0.8em; color:#888;">(預設)</span>' : ''}
                        </span>
                    </div>
                </td>

                <td>
                    <input type="number" class="events-input ticket-custom-price" 
                            placeholder="${ticket.price}" style="padding:5px;">
                </td>

                <td>
                    <input type="number" class="events-input ticket-custom-limit" 
                            placeholder="${ticket.isLimited ? ticket.limitQuantity : '無'}" style="padding:5px;">
                </td>

                <td style="color:#666; font-size:0.9em;">
                    ${ticket.description ? ticket.description : "-"}
                </td>

                <td style="padding:8px;">
                    <div style="margin-bottom: 5px;">
                        <input type="checkbox" class="early-bird-checkbox" id="eb_cb_${ticket.id}" style="cursor:pointer;">
                        <label for="eb_cb_${ticket.id}" style="cursor:pointer; font-size:0.9rem;">啟用早鳥</label>
                    </div>
                    <div class="eb-settings-container" style="display:none; margin-top:4px;"></div>
                </td>
            `;

                tbody.appendChild(row);
                earlyBirdForm(row);
            });
        });
}

// 顯示勾選早鳥票後出現的欄位方法
function earlyBirdForm(row) {
    const ebCheck = row.querySelector(".early-bird-checkbox");
    const container = row.querySelector(".eb-settings-container");

    ebCheck.addEventListener("change", () => {
        if (ebCheck.checked) {

            // 顯示容器
            container.style.display = "block";

            // 注入 HTML (加上 min="1" 防止輸入負數)
            container.innerHTML = `
                <div class="events-eb-wrapper" style="display:flex; align-items:center; gap:5px; flex-wrap:wrap; font-size:0.9rem;">
                    <span>開始售票</span>
                    <input type="number" class="early-bird-days" placeholder="5" min="1" style="width:50px; padding:3px;">
                    <span>天內，打</span>
                    <input type="number" class="early-bird-discount" placeholder="85" min="1" max="99" style="width:50px; padding:3px;">
                    <span>折</span>
                    <span style="color:#aaa; margin-left:5px;">➜</span>
                    <span font-weight:bold;">$</span>
                    <input type="text" class="early-bird-final-price" readonly 
                            style="width:30px; background:transparent; border:none; font-weight:bold;" tabindex="-1">
                </div>
            `;

            const ebDiscount = container.querySelector(".early-bird-discount");
            const ebFinal = container.querySelector(".early-bird-final-price");
            const ebDays = container.querySelector(".early-bird-days");

            // 往上找票價欄位
            const priceInput = row.querySelector(".ticket-custom-price");

            // 防止負數防護
            [ebDiscount, ebDays].forEach(input => {
                input.addEventListener("input", function () {
                    if (this.value < 0) this.value = Math.abs(this.value); // 轉正
                    if (this.value === "0") this.value = ""; // 不接受 0
                });
            });

            // 計算邏輯
            // 綁定價格計算
            const calculate = () => {
                const basePrice = parseFloat(priceInput.value) || parseFloat(priceInput.placeholder);
                const rate = parseFloat(ebDiscount.value) || parseFloat(ebDiscount.placeholder);

                if (isNaN(basePrice) || isNaN(rate)) {
                    ebFinal.value = "-";
                    return;
                }
                let discountRate = rate;
                if (rate > 10) {
                    discountRate = rate / 100;
                } else {
                    discountRate = rate / 10;
                }

                ebFinal.value = Math.round(basePrice * discountRate);
            };

            // 當「折扣」或「原價」改變時，重新計算
            ebDiscount.addEventListener("input", calculate);
            priceInput.addEventListener("input", calculate);

            // 初始化先算一次
            calculate();

        } else {
            container.innerHTML = "";
            container.style.display = "none";
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

// 初始化活動描述文字編輯器
function initEventDescriptionEditor() {
    const textarea = document.getElementById("description");
    if (!textarea) return;

    // 避免重複初始化（非常重要）
    if (tinymce.get("description")) {
        tinymce.get("description").remove();
    }

    tinymce.init({
        selector: "#description",
        valid_elements: '*[*]',
        height: 300,
        menubar: false,
        branding: false,
        statusbar: false,
        license_key: 'gpl',

        plugins: "lists link table code preview wordcount",
        toolbar:
            "undo redo | bold italic underline | " +
            "alignleft aligncenter alignright | " +
            "bullist numlist | link table | hr |" +
            "code preview",
        formats: {
            alignleft: [
                { selector: 'p', classes: 'text-left' },
                { selector: 'ul', classes: 'text-left' },
                { selector: 'ol', classes: 'text-left' },
                { selector: 'li', classes: 'text-left' }
            ],
            aligncenter: [
                { selector: 'p', classes: 'text-center' },
                { selector: 'ul', classes: 'text-center' },
                { selector: 'ol', classes: 'text-center' },
                { selector: 'li', classes: 'text-center' }
            ],
            alignright: [
                { selector: 'p', classes: 'text-right' },
                { selector: 'ul', classes: 'text-right' },
                { selector: 'ol', classes: 'text-right' },
                { selector: 'li', classes: 'text-right' }
            ]
        },

        content_style: `
            body { text-align: center; }
            ul, ol { display: inline-block; text-align: left; }
        `,


        // 讓 form submit 時自動同步回 textarea
        setup(editor) {
            editor.on("change keyup", () => {
                editor.save();
            });
        }
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

    if (tabName === "event" && !editorInited) {
        initEventDescriptionEditor();
        editorInited = true;
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

    // 清除編輯狀態 ID
    editingEventId = null;

    const form = document.getElementById("eventForm");
    if (form) form.reset();

    document.getElementById("eventStart").value = "";
    document.getElementById("eventEnd").value = "";
    document.getElementById("ticketStart").value = "";

    const coverInput = document.getElementById("cover");
    if (coverInput) coverInput.value = "";

    updateCoverPreview(null);

    if (typeof clearAllTicketRows === "function") {
        clearAllTicketRows();
    }

    if (tinymce.get("description")) {
        tinymce.get("description").setContent("");
    }

    // 隱藏取消按鈕
    const cancelBtn = document.getElementById("eventCancelBtn");
    if (cancelBtn) cancelBtn.style.display = "none";

    // 重設提交按鈕文字
    const submitBtn = document.querySelector("#eventForm button[type='submit']");
    if (submitBtn) submitBtn.textContent = "發佈活動";

    loadEventList();

}

function clearAllTicketRows() {
    document.querySelectorAll("#ticketSelectTbody tr").forEach(row => {
        // 1. 重置基本勾選與輸入框
        const ticketCheckbox = row.querySelector(".ticket-checkbox");
        if (ticketCheckbox) ticketCheckbox.checked = false;

        const priceInput = row.querySelector(".ticket-custom-price");
        if (priceInput) priceInput.value = "";

        const limitInput = row.querySelector(".ticket-custom-limit");
        if (limitInput) limitInput.value = "";

        const ebCheckbox = row.querySelector(".early-bird-checkbox");
        if (ebCheckbox) {
            ebCheckbox.checked = false;
            ebCheckbox.dispatchEvent(new Event("change"));
        }
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
                <table class="events-table">
                    <thead>
                        <tr>
                            <th>活動名稱</th>
                            <th>開始時間</th>
                            <th>結束時間</th>
                            <th>售票日期</th>
                            <th>狀態</th>
                            <td>編輯</td>
                        </tr>
                    </thead>
                    <tbody>
            `;

            events.forEach(ev => {
                const status = ev.status ?? "未設定";
                const canEdit = !(status === "已結束" || status === "已取消");
                const canCancel = (status === "未開放");

                const isEditing = editingEventId === ev.id;

                let actionButtons = "";

                if (canEdit) {
                    const btnClass = isEditing ? "events-btn events-btn-danger editing" : "events-btn events-btn-secondary";
                    const btnText = isEditing ? "取消" : "編輯";

                    actionButtons += `<button class="${btnClass} events-btn events-btn-secondary edit-btn" onclick="goEdit(${ev.id}, this)">${btnText}</button>`;
                } else {
                    actionButtons += `<button class="events-btn events-btn-danger cancel-btn" disabled style="opacity:0.4; cursor:not-allowed;">編輯</button>`;
                }

                if (canCancel) {
                    actionButtons += `
                        <button class="events-btn events-btn-danger cancel-btn"
                                onclick="cancelEvent(${ev.id})"
                                margin-left:6px;">
                            取消活動
                        </button>`;
                }

                html += `
                    <tr style="${(status === '已結束' || status === '已取消') ? 'opacity:0.5;' : ''}">
                        <td>
                            <div class="marquee-wrapper">
                                <span class="marquee-content" title="${ev.title}">${ev.title}</span>
                            </div>
                        </td>
                        <td>${ev.eventStart ?? "未設定"}</td>
                        <td>${ev.eventEnd ?? "未設定"}</td>
                        <td>${ev.ticketStart ?? "未設定"}</td>
                        <td>${status}</td>
                        <td>${actionButtons}</td>
                    </tr>
                `;
            });

            html += "</tbody></table>";
            container.innerHTML = html;
            renderEventPagination(data);
            initMarqueeDetection();
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
    if (!cancelBtn) return;
    cancelBtn.addEventListener("click", () => {
        resetEventForm();
        editingEventId = null;
        // 重新載入列表以更新按鈕狀態
        loadEventList();
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
            loadEventList();
            resetEventForm();
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
        toggleSort("createdAt");
    });

    sortEventBtn.addEventListener("click", () => {
        toggleSort("eventStart");
    });
}

function toggleSort(field) {
    if (eventQuery.sort === field) {
        // 同一欄位 → 反轉排序方向
        eventQuery.order = eventQuery.order === "asc" ? "desc" : "asc";
    } else {
        // 換欄位 → 預設為升序
        eventQuery.sort = field;
        eventQuery.order = "asc";
    }

    eventQuery.page = 1;
    loadEventList();
    highlightSortButton();
}

function highlightSortButton() {
    const createBtn = document.getElementById("sortCreateBtn");
    const eventBtn = document.getElementById("sortEventBtn");

    createBtn.classList.remove("active", "asc", "desc");
    eventBtn.classList.remove("active", "asc", "desc");

    const activeBtn = eventQuery.sort === "createdAt" ? createBtn : eventBtn;
    activeBtn.classList.add("active");
    activeBtn.classList.add(eventQuery.order); // 加上 asc 或 desc 樣式
}

function renderEventPagination(data) {
    const container = document.getElementById("eventPagination");
    if (!container) return;

    let html = "";

    if (!data.first) {
        html += `<button class="events-page-btn" onclick="gotoPage(${eventQuery.page - 1})">上一頁</button>`;
    }

    for (let i = 1; i <= data.totalPages; i++) {
        html += `<button class="events-page-btn" onclick="gotoPage(${i})" ${i === eventQuery.page ? "style='font-weight:bold;'" : ""}>${i}</button>`;
    }

    if (!data.last) {
        html += `<button class="events-page-btn" onclick="gotoPage(${eventQuery.page + 1})">下一頁</button>`;
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
                            <button class="events-btn events-btn-secondary"; type="button" onclick="applyTemplate(this)">
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
                            <button class="events-btn events-btn-secondary ticket-edit-btn"
                                    data-mode="edit"
                                    onclick="editTicket(${t.id}, this)">
                                編輯
                            </button>
                        </td>
                        <td><button class="events-btn events-btn-secondary" onclick="deleteTicket(${t.id})">刪除</button></td>
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

            const cancelBtn = document.getElementById("ticketCancelBtn");
            if (cancelBtn) cancelBtn.style.display = "inline-flex";

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
function cancelEditTicket() {
    resetTicketForm();
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

    const cancelBtn = document.getElementById("ticketCancelBtn");
    if (cancelBtn) cancelBtn.style.display = "none";

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

// 跑馬燈相關
function initMarqueeDetection() {
    const wrappers = document.querySelectorAll('.marquee-wrapper');

    wrappers.forEach(wrapper => {
        const content = wrapper.querySelector('.marquee-content');

        // 檢查：如果內容寬度 > 容器寬度，才加上動畫 class
        // 我們這裡利用 CSS class 來控制 animation

        if (content.scrollWidth > wrapper.clientWidth) {
            content.classList.add('enable-scroll');
        } else {
            content.classList.remove('enable-scroll');
        }
    });
}

