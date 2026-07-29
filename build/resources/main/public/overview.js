document.addEventListener("DOMContentLoaded", () => {
    fetch('/api/endpoints')
        .then(res => res.json())
        .then(data => {
            const total = data.length;
            const upCount = data.filter(e => e.status === 'UP').length;
            const downCount = total - upCount;

            const container = document.getElementById('overview-stats');
            container.innerHTML = `
                <h3>Total Monitored Services: ${total}</h3>
                <p style="color: green;">Services UP: ${upCount}</p>
                <p style="color: red;">Services DOWN: ${downCount}</p>
            `;
        });
});
