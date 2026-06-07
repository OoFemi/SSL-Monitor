let currentPage = 1;
const pageSize = 10;

function loadPage(page = 1) {
  fetch(`/api/urls?page=${page}&size=${pageSize}`)
    .then(res => res.json())
    .then(data => {
      const tableBody = document.querySelector("#urlTable tbody");
      tableBody.innerHTML = "";

      data.items.forEach(u => {
        const row = document.createElement("tr");
        row.innerHTML = `
          <td>${u.id}</td>
          <td>${u.url}</td>
          <td>${u.category}</td>
          <td>${u.tags.join(", ")}</td>
          <td>
            <button class="btn-outline" onclick="deleteUrl(${u.id})">Delete</button>
          </td>
        `;
        tableBody.appendChild(row);
      });

      document.querySelector("#pageInfo").textContent =
        `Page ${data.page} of ${Math.ceil(data.total / pageSize)}`;
    })
    .catch(err => console.error("Failed to load URLs:", err));
}

function deleteUrl(id) {
  if (!confirm("Delete this URL?")) return;

  fetch(`/api/urls/${id}`, { method: "DELETE" })
    .then(res => {
      if (res.ok) {
        alert("✅ URL deleted successfully");
        loadPage(1); // reload first page after deletion
      } else {
        alert("❌ Delete failed");
      }
    })
    .catch(err => console.error("Delete error:", err));
}

document.addEventListener("DOMContentLoaded", () => {
  loadPage(currentPage);
});
