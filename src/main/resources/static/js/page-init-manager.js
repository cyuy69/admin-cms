const PageInitializers = {
    "/organizer/dashboard": () => {
        if (typeof initActivityCards === "function") initActivityCards();
        loadKpi();
    },
    "/organizer/dashboard/announcement": () => {
        if (typeof initAnnouncement === "function") initAnnouncement();
    },
    "/organizer/dashboard/event": () => {
        if (typeof initEvent === "function") initEvent();
    },
    "/organizer/dashboard/event/ticket": () => {
        if (typeof initEvent === "function") initEvent();
    },
    "/organizer/dashboard/analytics/traffic": () => {
        if (typeof initTrafficAnalytics === "function") initTrafficAnalytics();
    },
    "/organizer/dashboard/orders": () => {
        if (typeof initOrders === "function") initOrders();
    },
    // Admin Dashboard 初始化
    "/admin/dashboard": () => {
        if (typeof initAdminDashboard === "function") initAdminDashboard();
    },

    // Admin 使用者管理
    "/admin/dashboard/users": () => {
        if (typeof initAdminUsers === "function") initAdminUsers();
    },

    // Admin 訂單管理
    "/admin/dashboard/orders": () => {
        if (typeof initAdminOrders === "function") initAdminOrders();
    },

};

function pageInitializer() {
    console.log("Current path:", location.pathname);
    const path = location.pathname;
    const init = PageInitializers[path];

    if (typeof init === "function") {
        // 延遲到 DOM layout 真正完成
        setTimeout(() => {
            init();
        }, 0);
    }
}
