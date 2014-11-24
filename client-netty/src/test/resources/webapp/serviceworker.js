console.log("in serviceworker.js file:", this);

this.addEventListener("install", function(event) {
    console.log("ServiceWorker installs:", event);
});

this.addEventListener("activate", function(event) {
    console.log("ServiceWorker onactivate");
});

this.addEventListener('message', function(event) {
    console.log("ServiceWorker onpush \"" + event.data + "\"");
    if (Notification.permission === "granted") {
        var notification = new Notification("WebPush notification", {
                serviceWorker: true,
                body: JSON.parse(evt.data),
                tag: 'WebPush'
            });
        console.log(notification);
    }
}, false);


