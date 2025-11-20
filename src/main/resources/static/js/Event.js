function initEvent() {
    initEventFormSubmit();
    initTicketDropdownToggle();
    initTicketTypeLoader();
    initTicketFormSubmit()
    initLimitQuantityToggle();
    initTabSwitching();
}

// 活動建立送出按鈕初始化
function initEventFormSubmit() {
    const form = document.getElementById("eventForm");
    if (!form) return;

    form.addEventListener("submit", function (e) {
        e.preventDefault();
        const formData = new FormData(form);

        const selectedTickets = [];
        const rows = document.querySelectorAll("#ticketSelectTbody tr");

        rows.forEach((row) => {
            const checkbox = row.querySelector(".ticket-checkbox");
            if (!checkbox.checked) return;

            const priceInput = row.querySelector(".ticket-custom-price");
            const limitInput = row.querySelector(".ticket-custom-limit");

            selectedTickets.push({
                ticketTemplateId: parseInt(checkbox.value, 10),
                customPrice: priceInput.value ? parseFloat(priceInput.value) : null,
                customLimit: limitInput.value ? parseInt(limitInput.value) : null,
            });
        });

        formData.append("eventTicketsJson", JSON.stringify(selectedTickets));

        fetch("/api/events/create", {
            method: "POST",
            body: formData,
        })
            .then((res) => res.json())
            .then((data) => {
                alert(`活動「${data.title}」已建立！`);
                window.location.href = "/admin/dashboard/event";
            })
            .catch((err) => {
                console.error("建立失敗：", err);
                alert("建立活動失敗！");
            });
    });
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

// 票種載入變成表格格式，而不是一堆 label
function initTicketTypeLoader() {
    const dropdown = document.getElementById("ticketDropdown");
    if (!dropdown) return;

    fetch("/api/tickets")
        .then((res) => res.json())
        .then((ticketTypes) => {

            // ⭐ 生成表格標頭
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

                row.innerHTML = `
                    <td style="border:1px solid #ccc; padding:6px; text-align:center;">
                        <input type="checkbox" class="ticket-checkbox" value="${ticket.id}">
                    </td>

                    <td style="border:1px solid #ccc; padding:6px;">
                        ${ticket.name}
                        ${ticket.isDefault ? "(自訂)" : "(模板)"}
                        <br>
                        <small>模板價：$${ticket.price}</small>
                        ${ticket.isLimited ? `<br><small>模板限量：${ticket.limitQuantity} 張</small>` : ""}
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

