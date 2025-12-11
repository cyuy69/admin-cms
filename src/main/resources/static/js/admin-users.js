function initAdminUsers() {
    console.log("初始化 Admin Users 模組...");
    // 綁定查詢按鈕
    const btn = document.getElementById("searchBtn");
    if (btn) {
        btn.onclick = () => loadUsers(0);
    }
    // 第一次載入
    loadUsers(0);
}

async function loadUsers(page) {

    const keyword = document.getElementById("keyword")?.value.trim() || "";
    const size = 10;

    let params = [`page=${page}`, `size=${size}`];
    if (keyword) params.push(`keyword=${keyword}`);

    const url = `/api/admin/users?${params.join("&")}`;
    console.log("呼叫 API:", url);

    const resp = await fetch(url);
    if (!resp.ok) {
        console.error("API 錯誤:", resp.status);
        return;
    }

    const data = await resp.json();
    console.log("後端回傳 Page:", data);

    renderUsers(data.content);
    renderUserPagination(data);
}

function renderUsers(list) {
    const tbody = document.querySelector("#userTable tbody");
    tbody.innerHTML = "";

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">無資料</td></tr>`;
        return;
    }

    list.forEach(u => {
        const tr = document.createElement("tr");

        tr.innerHTML = `
            <td>${u.id}</td>
            <td>${u.account}</td>
            <td>${u.username}</td>
            <td>${roleLabel(u.role)}</td>
            <td>${u.isActive ? "啟用" : "封鎖"}</td>
            <td>
                <button onclick="setRole(${u.id},1)">主辦方</button>
                <button onclick="setRole(${u.id},2)">使用者</button>
                <button onclick="toggleActive(${u.id}, ${u.isActive})">
                    ${u.isActive ? "封鎖" : "啟用"}
                </button>
            </td>
        `;

        tbody.appendChild(tr);
    });
}

function roleLabel(r) {
    if (r === 0) return "開發者";
    if (r === 1) return "主辦方";
    return "使用者";
}

async function setRole(id, role) {
    await fetch(`/api/admin/users/${id}/role?role=${role}`, {
        method: "PUT"
    });
    loadUsers(0);
}

async function toggleActive(id, now) {
    const newState = now ? 0 : 1;
    await fetch(`/api/admin/users/${id}/active?active=${newState}`, {
        method: "PUT"
    });
    loadUsers(0);
}

function renderUserPagination(data) {
    const container = document.getElementById("pagination");
    container.innerHTML = "";

    const total = data.totalPages;
    const current = data.number;

    for (let i = 0; i < total; i++) {
        const btn = document.createElement("button");
        btn.innerText = i + 1;

        if (i === current) {
            btn.disabled = true;
        } else {
            btn.onclick = () => loadUsers(i);
        }

        container.appendChild(btn);
    }
}