function initEvent() {
    initEventFormSubmit();
    initTicketDropdownToggle();
    initTicketTypeLoader();
    initTicketFormSubmit()
    initLimitQuantityToggle();
    initTabSwitching();
}

function initEventFormSubmit() {
    const form = document.getElementById("eventForm");
    if (!form) return;

    form.addEventListener("submit", function (e) {
        e.preventDefault();
        const formData = new FormData(form);

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

function initTicketTypeLoader() {
    const dropdown = document.getElementById("ticketDropdown");
    if (!dropdown) return;

    fetch("/api/tickets")
        .then((res) => res.json())
        .then((ticketTypes) => {
            dropdown.innerHTML = "";

            ticketTypes.forEach((ticket) => {
                const label = document.createElement("label");
                label.innerHTML = `
            <input type="checkbox" name="ticketTypes" value="${ticket.id}" />
            ${ticket.name}（$${ticket.price}）${ticket.isLimited ? `限量 ${ticket.limitQuantity} 張` : "不限張數"
                    }
        `;
                dropdown.appendChild(label);
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

