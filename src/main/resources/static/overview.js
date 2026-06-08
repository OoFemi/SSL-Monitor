const BASE_URL = window.location.origin; // e.g. http://localhost

function loadOverview() {
  fetch(`${BASE_URL}/api/overview`)
    .then(res => {
      if (!res.ok) {
        throw new Error(`Failed to load overview: ${res.status}`);
      }
      return res.json();
    })
    .then(data => {
      const tbody = document.querySelector("#overviewTable tbody");
      tbody.innerHTML = "";

      data.forEach(item => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${item.url}</td>
          <td>${item.category || ""}</td>
          <td>${(item.tags || []).join(", ")}</td>
          <td style="color:${item.isUp ? 'green' : 'red'}">${item.isUp ? "UP" : "DOWN"}</td>
          <td>${item.responseTime} ms</td>
          <td>${item.sslDays}</td>
        `;
        tbody.appendChild(tr);
      });
    })
    .catch(err => {
      console.error(err);
      alert("Failed to load overview data");
    });
}

document.addEventListener("DOMContentLoaded", loadOverview);
