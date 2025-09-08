const CACHE_NAME = "stamps-cache-v1";
const ASSETS = [
  "/stamps/",
  "/stamps/index.html",
  "/stamps/js/compiled/app.js",
  "/stamps/styles.css",
  "/stamps/manifest.json",
  "/stamps/icons/android-chrome-192x192.png",
  "/stamps/icons/android-chrome-512x512.png"
];

// Install: cache app shell
self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS);
    })
  );
  self.skipWaiting(); // activate immediately
});

// Activate: take control right away
self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});

// Fetch: respond from cache first, fall back to network
self.addEventListener("fetch", (event) => {
  event.respondWith(
    caches.match(event.request).then((response) => {
      return response || fetch(event.request);
    })
  );
});
