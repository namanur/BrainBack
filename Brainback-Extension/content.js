// Brainback: YouTube De-clutter Logic (Fixed Runtime)

const blockList = [
    '#shorts-container', 
    'ytd-rich-section-renderer', 
    'ytd-reel-shelf-renderer',   
    'a[href^="/shorts"]',        
    '#related',                  
    '#comments'                  
];

function declutter() {
    blockList.forEach(selector => {
        const elements = document.querySelectorAll(selector);
        elements.forEach(el => {
            el.style.display = 'none';
        });
    });

    const items = document.querySelectorAll('ytd-rich-item-renderer');
    items.forEach(item => {
        if (item.innerText.includes('Shorts')) {
            item.style.display = 'none';
        }
    });
}

// CodeRabbit Fix: Wait for body to be ready
function startObserver() {
    if (!document.body) {
        window.requestAnimationFrame(startObserver);
        return;
    }

    const observer = new MutationObserver((mutations) => {
        declutter();
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
    
    declutter(); // Initial run
}

startObserver();
console.log("Brainback Extension: Hardened Runtime Active.");
