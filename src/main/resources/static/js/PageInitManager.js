const PageInitializers = {
    "/admin/dashboard": () => {
        if (typeof initActivityCards === "function") initActivityCards();
    },
    "/admin/dashboard/announcement": () => {
        if (typeof initAnnouncement === "function") initAnnouncement();
    },
    "/admin/dashboard/event": () => {
        if (typeof initEvent === "function") initEvent();
    },
    "/admin/dashboard/analytics/traffic": () => {
        if (typeof initTrafficAnalytics === "function") initTrafficAnalytics();
    },
    "/admin/dashboard/analytics/consumer": () => {
        if (typeof initConsumerAnalytics === "function") initConsumerAnalytics();
    },
    "/admin/dashboard/analytics/summary": () => {
        if (typeof initSummaryAnalytics === "function") initSummaryAnalytics();
    }
};

function pageInitializer() {
    const path = location.pathname;
    const init = PageInitializers[path];
    if (typeof init === "function") {
        init();
    }
}