function initAnnouncement() {

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

                    <div class="btn-group normal-mode">
                        <button class="edit-btn">編輯</button>
                    </div>

                    <div class="btn-group edit-mode" style="display:none;">
                        <button class="confirm-edit-btn">確認</button>
                        <button class="cancel-edit-btn">取消</button>
                        <button class="delete-btn">刪除</button>
                    </div>

                    <p>${ann.content}</p>
                    <hr>
                </div>
                `).join("");

            $("#anno-list").html(html);
        })
    }

    // 表單送出
    $(document).off("submit", "#anno-form").on("submit", "#anno-form", function (e) {
        e.preventDefault();

        let id = $("#ann-id").val(); // 有id時可以編輯
        let title = $("#title").val();
        let content = $("#content").val();

        let method = id ? "PUT" : "POST";
        let url = id ? `/api/announcements/${id}` : "/api/announcements";

        // console.log("表單送出", { title, content });

        $.ajax({
            url: url,
            method: method,
            contentType: "application/json",
            data: JSON.stringify({ title, content }),
            success: function () {

                resetForm()
                loadAnnouncements();
            }
        });
    });

    $(document).off("click", ".edit-btn").on("click", ".edit-btn", function () {
        let container = $(this).closest(".ann-item");
        let id = container.data("id");
        let title = container.find("strong").text();
        let content = container.find("p").text();

        $("#ann-id").val(id);
        $("#title").val(title);
        $("#content").val(content);

        $("#anno-submit-btn").text("編輯公告");
    })

    $(document).off("click", ".del-btn").on("click", ".del-btn", function () {
        let container = $(this).closest(".ann-item");
        let id = container.data("id");

        if (!confirm("確定要刪除此公告嗎？")) return;

        $.ajax({
            url: `/api/announcements/${id}`,
            method: "DELETE",
            success: function () {
                loadAnnouncements();
            }
        });
    });

    function resetForm() {
        $("#anno-form")[0].reset();
        $("#ann-id").val("");
        $("#anno-submit-btn").text("新增公告");
    }
}
