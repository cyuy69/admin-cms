function initAnnouncement() {
    let editingId = null;

    loadAnnouncements();

    function loadAnnouncements() {
        $.get("/api/announcements", function (data) {
            if (!data || data.length === 0) {
                $("#anno-list").html("<p>目前沒有公告</p>");
                return;
            }

            let html = data.map(ann => `
                <div class="ann-item" data-id="${ann.id}">
                    <strong>${ann.title}</strong>
                    <button class="edit-btn">編輯</button>
                    <p>${ann.content}</p>
                    <hr>
                </div>
            `).join("");

            $("#anno-list").html(html);
        });
    }

    // 表單送出（新增或編輯）
    $(document).off("submit", "#anno-form").on("submit", "#anno-form", function (e) {
        e.preventDefault();

        const id = editingId;
        const title = $("#title").val();
        const content = $("#content").val();

        const method = id ? "PUT" : "POST";
        const url = id ? `/api/announcements/${id}` : "/api/announcements";

        $.ajax({
            url,
            method,
            contentType: "application/json",
            data: JSON.stringify({ title, content }),
            success: function () {
                resetForm();
                loadAnnouncements();
            }
        });
    });

    // 點編輯
    $(document).off("click", ".edit-btn").on("click", ".edit-btn", function () {
        const $btn = $(this);
        const container = $btn.closest(".ann-item");
        const id = container.data("id");

        // 如果現在點的是「取消」
        if ($btn.text() === "取消") {
            resetForm();
            return;
        }

        const title = container.find("strong").text();
        const content = container.find("p").text();

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


    // 刪除公告
    $("#delete-edit-btn").on("click", function () {
        if (!editingId) return;
        if (!confirm("確定要刪除此公告嗎？")) return;

        $.ajax({
            url: `/api/announcements/${editingId}`,
            method: "DELETE",
            success: function () {
                resetForm();
                loadAnnouncements();
            }
        });
    });

    // 重設表單
    function resetForm() {
        editingId = null;
        $("#anno-form")[0].reset();
        $("#ann-id").val("");
        $("#anno-submit-btn").text("新增公告");
        $("#cancel-edit-btn, #delete-edit-btn").hide();

        $(".edit-btn").text("編輯");
    }
}
