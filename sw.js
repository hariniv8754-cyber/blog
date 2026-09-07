// Background Service Worker for Continuous Location Broadcaster
const CACHE_NAME = 'antigravity-location-v1';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

// Periodic Background Sync event (if supported by browser)
self.addEventListener('periodicsync', (event) => {
  if (event.tag === 'location-sync') {
    event.waitUntil(syncBackgroundLocation());
  }
});

self.addEventListener('sync', (event) => {
  if (event.tag === 'send-location') {
    event.waitUntil(syncBackgroundLocation());
  }
});

async function syncBackgroundLocation() {
  // Service worker background sync handler
  console.log('[SW] Background Sync Triggered');
}
