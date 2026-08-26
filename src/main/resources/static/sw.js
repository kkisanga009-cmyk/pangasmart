self.addEventListener('install', (e) => {
  console.log('PangaSmart Service Worker Imewekwa');
});

self.addEventListener('fetch', (e) => {
  // Inaruhusu app kufanya kazi mtandaoni bila shida
  e.respondWith(fetch(e.request).catch(() => caches.match(e.request)));
});