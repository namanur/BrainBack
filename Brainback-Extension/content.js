// Brainback: YouTube De-clutter Logic
// Target: Shorts and Distractions

const blockList = [
    '#shorts-container', 
    'ytd-rich-section-renderer', // Homepage "Shorts" shelf
    'ytd-reel-shelf-renderer',   // Sidebar/search "Shorts" shelf
    'a[href^="/shorts"]',        // Any direct link to shorts
    '#related',                  // Sidebar recommendations
    '#comments'                  // Optional: Hide comments if requested
];

function declutter() {
    blockList.forEach(selector => {
        const elements = document.querySelectorAll(selector);
        elements.forEach(el => {
            el.style.display = 'none';
        });
    });

    // Specific logic for YouTube homepage feed
    const items = document.querySelectorAll('ytd-rich-item-renderer');
    items.forEach(item => {
        if (item.innerText.includes('Shorts')) {
            item.style.display = 'none';
        }
    });
}

// Persistent observer to handle server-driven UI updates
const observer = new MutationObserver((mutations) => {
    declutter();
});

observer.observe(document.body, {
    childList: true,
    subtree: true
});

console.log("Brainback Extension: Active and De-cluttering...");
