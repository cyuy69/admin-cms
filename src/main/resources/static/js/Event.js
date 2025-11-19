function toggleTicketDropdown() {
    const dropdown = document.getElementById("ticketDropdown");
    dropdown.style.display = dropdown.style.display === "block" ? "none" : "block";
}

// ✅ 載入票種（預設 + 自訂）
fetch("/api/ticket-types")
    .then((res) => res.json())
    .then((ticketTypes) => {
        const dropdown = document.getElementById("ticketDropdown");
        dropdown.innerHTML = "";

        ticketTypes.forEach((ticket) => {
            const label = document.createElement("label");
            label.innerHTML = `
          <input type="checkbox" name="ticketTypes" value="${ticket.id}" />
          ${ticket.name}（$${ticket.price}）${ticket.isLimited ? `限量 ${ticket.limitQuantity} 張` : "不限張數"}
        `;
            dropdown.appendChild(label);
        });
    });
