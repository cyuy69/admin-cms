let currentPage = 0;
let pageSize = 5;
let currentKeyword = null;

function loadWithCondition(page = 0) {
    if (currentKeyword) {
        searchAnnouncements(page);
    } else {
        loadAnnouncements(page);
    }
}

function loadAnnouncements(page = 0) {
    currentPage = page;
    currentKeyword = null;

    $.get("/api/announcements", {
        page: page,
        size: pageSize
    }, renderResult);
}

function searchAnnouncements(page = 0) {
    const keyword = $("#searchKeyword").val().trim();
    if (!keyword) {
        currentKeyword = null;
        loadAnnouncements(0);
        return;
    }

    currentPage = page;
    currentKeyword = keyword;

    $.get("/api/announcements", {
        page: page,
        size: pageSize,
        keyword: keyword
    }, renderResult);
}

function renderResult(data) {
    if (!data || !data.content || data.content.length === 0) {
        $("#anno-list").html("<p>沒有資料</p>");
        $("#pagination").html("");
        return;
    }

    const html = data.content.map(ann => `
        <div class="card ann-item" data-id="${ann.id}">
            <div class="ann-header">
                <h4 class="ann-title">${ann.title}</h4>
                <button type="button" class="btn btn-secondary btn-sm edit-btn">編輯</button>
            </div>
            <div class="ann-content">${ann.content}</div>
        </div>
    `).join("");

    $("#anno-list").html(html);
    renderPagination(data.totalPages, data.number);
}

function renderPagination(totalPages, current) {
    const $p = $("#pagination");
    $p.html("");

    if (totalPages <= 1) return;

    if (current > 0) {
        $p.append(`<button class="btn btn-sm" onclick="loadWithCondition(${current - 1})">上一頁</button>`);
    }

    $p.append(`<span> 第 ${current + 1} / ${totalPages} 頁 </span>`);

    if (current + 1 < totalPages) {
        $p.append(`<button class="btn btn-sm" onclick="loadWithCondition(${current + 1})">下一頁</button>`);
    }
}

function initAnnouncement() {
    let editingId = null;

    loadAnnouncements(0);

    // 表單送出(新增或編輯)
    $(document).off("submit", "#anno-form").on("submit", "#anno-form", function (e) {
        e.preventDefault();

        const id = editingId;
        const title = $("#title").val();
        const content = $("#content").val();

        const method = id ? "PUT" : "POST";
        const url = id ? `/api/announcements/${id}` : "/api/announcements";

        $.ajax({
            url: url,
            method: method,
            contentType: "application/json",
            data: JSON.stringify({ title, content }),
            success: function () {
                resetForm();
                loadWithCondition(currentPage);
            },
            error: function (xhr) {
                console.error("送出失敗：", xhr.responseText);
                alert("送出失敗，請檢查格式或伺服器設定");
            }
        });

    });

    // 查詢
    $(document).off("click", "#search-btn")
        .on("click", "#search-btn", () => searchAnnouncements(0));

    // 編輯
    $(document).off("click", ".edit-btn").on("click", ".edit-btn", function () {
        const $btn = $(this);
        const container = $btn.closest(".ann-item");
        const id = container.data("id");

        // 如果現在點的是「取消」
        if ($btn.text() === "取消") {
            resetForm();
            return;
        }

        const title = container.find(".ann-title").text();
        const content = container.find(".ann-content").text();

        editingId = id;

        $("#ann-id").val(id);
        $("#title").val(title);
        $("#content").val(content);

        $("#anno-submit-btn").text("確認編輯");
        $("#cancel-edit-btn, #delete-edit-btn").show();

        // 將所有編輯按鈕恢復為「編輯」
        $(".edit-btn").text("編輯");

        // 將目前這顆改為「取消」
        $btn.text("取消");
    });

    $(document).off("click", "#cancel-edit-btn").on("click", "#cancel-edit-btn", function () {
        console.log("取消編輯按鈕被點擊");
        resetForm();
    });

    // 刪除公告
    $("#delete-edit-btn").on("click", function () {
        if (!editingId) return;
        if (!confirm("確定要刪除此公告嗎？")) return;

        $.ajax({
            url: `/api/announcements/${editingId}`,
            method: "DELETE",
            success: function () {
                resetForm();
                loadWithCondition(currentPage);
            }
        });
    });

    // 重設表單
    function resetForm() {
        editingId = null;
        const formEl = $("#anno-form")[0];
        if (!formEl) return;
        $("#ann-id").val("");
        $("#anno-submit-btn").text("新增公告");
        $("#cancel-edit-btn, #delete-edit-btn").hide();
        $(".edit-btn").text("編輯");
    }
}

$(document).ready(initAnnouncement);
