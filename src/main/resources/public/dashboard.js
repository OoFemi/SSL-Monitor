document.addEventListener("DOMContentLoaded", () => {
    fetchEndpoints();
    loadLogo();
    setInterval(fetchEndpoints, 30000);
});

function loadLogo() {
    fetch('/api/admin/logo-status')
        .then(res => res.json())
        .then(data => {
            if (data.logoUrl) {
                const logoImg = document.getElementById('company-logo');
                logoImg.src = data.logoUrl + "?" + new Date().getTime(); // Prevent cache
                logoImg.style.display = 'block';
            }
        });
}

function fetchEndpoints() {
    // Existing endpoint fetch logic...
}
