document.addEventListener("DOMContentLoaded", loadUrls);

function loadUrls() {
  fetch("/api/admin/urls")
    .then(res => {
      if (res.status === 401) {
        window.location.href = "/login.html";
        return;
      }
      return res.json();
    })
    .then(data => {
      const tbody = document.querySelector("#urlTable tbody");
      tbody.innerHTML = "";

      data.items.forEach(u => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${u.id}</td>
          <td>${u.url}</td>
          <td>${u.category || ""}</td>
          <td>${(u.tags || []).join(", ")}</td>
          <td>
            <button onclick="deleteUrl(${u.id})">Delete</button>
          </td>
        `;
        tbody.appendChild(tr);
      });
    });
}

document.querySelector("#addForm").addEventListener("submit", e => {
  e.preventDefault();

  const url = document.querySelector("#url").value;
  const category = document.querySelector("#category").value;
  const tags = document.querySelector("#tags").value.split(",").map(t => t.trim());

  fetch("/api/admin/urls", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ url, category, tags })
  }).then(() => loadUrls());
});

function deleteUrl(id) {
  fetch(`/api/admin/urls/${id}`, { method: "DELETE" })
    .then(() => loadUrls());
}

function logout() {
  fetch("/api/logout", { method: "POST" })
    .then(() => window.location.href = "/login.html");
}
