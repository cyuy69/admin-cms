function loadContent(event, link) {
    console.log("我成功了")
    event.preventDefault();

    document.querySelectorAll(".sidebar li")
        .forEach((li) => li.classList.remove("active"));

    const li = link.closest("li");
    const isDropdownToggle = link.classList.contains("dropdown-toggle");

    if (li) li.classList.add("active");

    if (isDropdownToggle) {
        const dropdown = li.querySelector(".dropdown-menu");
        if (dropdown) dropdown.classList.add("show");
        return; // 不載入內容，只展開
    }

    const url = link.getAttribute("href");
    const logicalPath = link.dataset.path || "";


    history.pushState(
        null,
        "",
        "/admin/dashboard" + (logicalPath ? "/" + logicalPath : ""));
    // 載入 fragment
    fetch(url)
        .then((response) => {
            if (!response.ok) throw new Error("載入失敗");
            return response.text();
        })
        .then((html) => {
            document.getElementById("main-content-area").innerHTML = html;
            initActivityCards()
        })
        .catch((error) => {
            console.error("載入錯誤:", error);
        });
}


window.addEventListener("DOMContentLoaded", () => {
    const path = location.pathname;

    // 清除 active 樣式
    document
        .querySelectorAll(".sidebar li")
        .forEach((li) => li.classList.remove("active"));

    // 還原 dropdown 展開狀態
    document.querySelectorAll(".dropdown-menu").forEach((menu) => {
        const id = menu.id;
        const state = localStorage.getItem("dropdown:" + id);
        if (state === "open") {
            menu.classList.add("show");
        }
    });

    document.querySelectorAll("[data-path]").forEach((link) => {
        const logicalPath = link.dataset.path;
        const expectedPath = "/admin/dashboard" + (logicalPath ? "/" + logicalPath : "");
        if (path === expectedPath) {
            const li = link.closest("li");
            if (li) li.classList.add("active");
            const dropdown = link.closest(".dropdown-menu");
            if (dropdown) dropdown.classList.add("show");
        }
    });

    // ✅ 初始載入 dashboard fragment（僅限首頁）
    if (path === "/admin/dashboard") {
        fetch("/admin/dashboard-frag")
            .then((response) => {
                if (!response.ok) throw new Error("載入失敗");
                return response.text();
            })
            .then((html) => {
                document.getElementById("main-content-area").innerHTML = html;
                initActivityCards()
            })
            .catch((error) => {
                console.error("初始載入錯誤:", error);
            });
    }

});


function toggleDropdown(id) {
    const menu = document.getElementById(id);
    const parentLi = menu.closest("li");

    if (menu.classList.contains("show")) {
        // 正在收合
        const currentHeight = menu.scrollHeight; // 目前高度
        menu.style.maxHeight = currentHeight + "px"; // 先固定住
        requestAnimationFrame(() => {
            menu.style.maxHeight = "0"; // 再收回去
        });
        menu.classList.remove("show");
        parentLi.classList.remove("expanded");

    } else {
        // 正在展開
        menu.classList.add("show");
        const targetHeight = menu.scrollHeight + "px"; // 真實內容高度
        menu.style.maxHeight = "0"; // 從0開始
        requestAnimationFrame(() => {
            menu.style.maxHeight = targetHeight;
        });

        parentLi.classList.add("expanded");

        // 動畫結束後清除 maxHeight 限制，避免高度被鎖死
        menu.addEventListener("transitionend", function handler() {
            if (menu.classList.contains("show")) {
                menu.style.maxHeight = "none";
            }
            menu.removeEventListener("transitionend", handler);
        });
    }

}

