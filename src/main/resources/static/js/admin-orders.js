document.addEventListener("DOMContentLoaded", () => {
    console.log("Admin Order Page Loaded");

    loadOrders(0); // 預設 page = 0

    document.getElementById("searchBtn").addEventListener("click", () => {
        loadOrders(0); // 重新查詢從第 0 頁開始
    });
});

async function loadOrders(page) {

    const keyword = document.getElementById("keyword").value.trim();
    const startDate = document.getElementById("startDate").value;
    const endDate = document.getElementById("endDate").value;
    const size = 10; // 每頁 10 筆

    let params = [`page=${page}`, `size=${size}`];

    if (keyword) params.push(`keyword=${keyword}`);
    if (startDate) params.push(`startDate=${startDate}`);
    if (endDate) params.push(`endDate=${endDate}`);

    const query = `?${params.join("&")}`;
    const url = `/api/admin/orders${query}`;

    console.log("呼叫 API:", url);

    try {
        const resp = await fetch(url);

        if (!resp.ok) {
            console.error("API 錯誤:", resp.status);
            return;
        }

        const data = await resp.json(); // Page 格式

        console.log("後端回傳 Page:", data);

        renderOrders(data.content);          // 表格資料
        renderPagination(data);              // 分頁按鈕

    } catch (err) {
        console.error("無法取得訂單資料:", err);
    }
}

// 渲染表格
function renderOrders(list) {
    const tbody = document.querySelector("#orderTable tbody");
    tbody.innerHTML = "";

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;">無訂單資料</td></tr>`;
        return;
    }

    list.forEach(o => {
        const tr = document.createElement("tr");

        tr.innerHTML = `
            <td>${o.orderId}</td>
            <td>${formatDate(o.createdAt)}</td>
            <td>${o.buyerName}</td>
            <td>${o.buyerAccount}</td>
            <td>${o.eventTitle}</td>
            <td>${o.totalQuantity}</td>
            <td>${o.status}</td>
        `;

        tbody.appendChild(tr);
    });
}

// 分頁按鈕
function renderPagination(pageData) {
    const container = document.getElementById("pagination");
    container.innerHTML = "";

    const current = pageData.number;
    const total = pageData.totalPages;

    // 🔥 即使 totalPages=1 一樣顯示
    // 上一頁（如果不是第 0 頁）
    if (current > 0) {
        const prev = document.createElement("button");
        prev.textContent = "上一頁";
        prev.onclick = () => loadOrders(current - 1);
        container.appendChild(prev);
    }

    // 頁碼按鈕（哪怕只有 1 頁也顯示）
    for (let i = 0; i < total; i++) {
        const btn = document.createElement("button");
        btn.textContent = i + 1;
        if (i === current) {
            btn.disabled = true;
        } else {
            btn.onclick = () => loadOrders(i);
        }
        container.appendChild(btn);
    }

    // 下一頁
    if (current < total - 1) {
        const next = document.createElement("button");
        next.textContent = "下一頁";
        next.onclick = () => loadOrders(current + 1);
        container.appendChild(next);
    }
}



// 格式化日期
function formatDate(dateTimeStr) {
    if (!dateTimeStr) return "-";
    const date = new Date(dateTimeStr);
    return date.toLocaleString("zh-TW", { hour12: false });
}