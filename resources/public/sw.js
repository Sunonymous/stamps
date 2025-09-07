const CACHE_NAME = "stamps-offline-v1";
const ASSETS = [
  "/stamps/",
  "/stamps/index.html",
  "/stamps/js/compiled/app.js",
  "/stamps/styles.css",
  "/stamps/manifest.json",
  "/stamps/icons/android-chrome-192x192.png",
  "/stamps/icons/android-chrome-512x512.png"
];

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS))
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    )
  );
});

self.addEventListener("fetch", event => {
  event.respondWith(
    caches.match(event.request).then(response => response || fetch(event.request))
  );
});
