function loadContent(event, link) {
    event.preventDefault();

    document.querySelectorAll(".sidebar li")
        .forEach((li) => li.classList.remove("active"));

    const li = link.closest("li");
    const isDropdownToggle = link.classList.contains("dropdown-toggle");

    if (!isDropdownToggle) {
        localStorage.setItem("activeLink", link.id);
    }

    if (li) li.classList.add("active");

    if (isDropdownToggle) {
        const dropdown = li.querySelector(".dropdown-menu");
        if (dropdown) dropdown.classList.add("show");
        return; // 不載入內容，只展開
    }

    // ------- AJAX 載入 fragment -------
    const url = link.getAttribute("href");
    const logicalPath = link.dataset.path || "";


    history.pushState(
        null,
        "",
        "/admin/dashboard" + (logicalPath ? "/" + logicalPath : ""));

    fetch(url)
        .then((response) => {
            if (!response.ok) throw new Error("載入失敗");
            return response.text();
        })
        .then((html) => {
            document.getElementById("main-content-area").innerHTML = html;
            pageInitializer();
        })
        .catch((error) => {
            console.error("載入錯誤:", error);
        });
}


window.addEventListener("DOMContentLoaded", () => {
    pageInitializer()

    // 收和所有 dropdown
    document.querySelectorAll(".dropdown-menu").forEach((menu) => {
        menu.classList.remove("show");
        menu.style.maxHeight = "0";
    });

    // 還原 dropdown 狀態
    document.querySelectorAll(".dropdown-menu").forEach((menu) => {
        const id = menu.id;
        const state = localStorage.getItem("dropdown:" + id);
        if (state === "open") {
            menu.classList.add("show");
            menu.style.maxHeight = "none";
        }
    });

    // 還原active樣式
    const activeId = localStorage.getItem("activeLink");
    if (activeId) {
        const link = document.getElementById(activeId);
        if (link) {
            const li = link.closest("li");
            if (li) li.classList.add("active");

            const dropdown = link.closest(".dropdown-menu");
            if (dropdown) dropdown.classList.add("show");
        }
    }
});



function toggleDropdown(id) {
    const menu = document.getElementById(id);
    const parentLi = menu.closest("li");
    const isOpening = !menu.classList.contains("show");

    // 儲存狀態
    localStorage.setItem("dropdown:" + id, isOpening ? "open" : "closed");

    if (!isOpening) {
        // === 收合 ===
        const currentHeight = menu.scrollHeight;
        menu.style.maxHeight = currentHeight + "px";
        requestAnimationFrame(() => {
            menu.style.maxHeight = "0";
        });
        menu.classList.remove("show");
        parentLi.classList.remove("expanded");
        return;
    }

    // === 展開 ===
    menu.classList.add("show");
    const targetHeight = menu.scrollHeight + "px";
    menu.style.maxHeight = "0";
    requestAnimationFrame(() => {
        menu.style.maxHeight = targetHeight;
    });

    menu.addEventListener("transitionend", function handler() {
        if (menu.classList.contains("show")) {
            menu.style.maxHeight = "none";
        }
        menu.removeEventListener("transitionend", handler);
    });

    parentLi.classList.add("expanded");
}


