function initActivityCards() {
  const $container = $("#activityContainer");
  const $empty = $("#emptyState");

  if ($container.length === 0 || $empty.length === 0) return;

  $.getJSON("/api/events/latest")
    .done(activities => {

      if (!activities || activities.length === 0) {
        $empty.show();
        return;
      }

      $empty.hide();

      activities.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      activities.slice(0, 3).forEach(act => {
        const imgUrl = act.images?.[0]?.imageUrl || 'https://placehold.co/100x100?text=No+Image';
        const startTime = act.eventStart || "未設定";
        const endTime = act.eventEnd || "未設定";
        const avgStay = act.avgStayTime
          ? `${Math.floor(act.avgStayTime / 60)}分${act.avgStayTime % 60}秒`
          : '—';

        const cardHtml = `
          <div class="activity-card">
            <div class="card-top">
              <img src="${imgUrl}" alt="活動圖片">
              <div class="card-content">
                <h3>${act.title}</h3>
                <div class="meta">
                  <span class="created">活動開始時間：</span>
                  ${startTime}
                  <span class="event-time">活動結束時間：<wbr></span>
                  ${endTime}
                </div>
              </div>
            </div>
            <div class="card-bottom">
              <div class="stats">
                <div class="stat-item">
                  <span class="label">👁️ 瀏覽量</span>
                  <span class="value">${act.views ?? 0}</span>
                </div>
                <div class="stat-item">
                  <span class="label">⏱ 平均停留</span>
                  <span class="value">${avgStay}</span>
                </div>
                <div class="stat-item">
                  <span class="label">🎟️ 售出票數</span>
                  <span class="value">${act.ticketsSold ?? 0}</span>
                </div>
                <div class="stat-item">
                  <span class="label">🔗 分享數</span>
                  <span class="value">${act.shares ?? 0}</span>
                </div>
              </div>
            </div>
          </div>
        `;

        $container.append(cardHtml);
      });
    })
    .fail(() => {
      console.error("無法載入活動資料");

      $empty.show().html(`
        <p style="color:red;">載入資料失敗，請稍後再試。</p>
      `);
    });
}