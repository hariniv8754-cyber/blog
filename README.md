# 📍 Multi-Person Live Location Tracker & Google Maps Sync

A real-time, privacy-focused location tracker with Google Maps integration.

## 🚀 Features
- **Host / Admin Map**: Only the creator sees the full interactive map with everyone's live location pins.
- **Participant Link (No Map Shown)**: Other people receive a simple check-in link that turns on their GPS with 1 click. They **never see the map** or other people's locations.
- **Direct Google Maps Navigation**: Every person on the host map has an instant "Open in Google Maps" button.
- **Rooms Hub**: Create and manage multiple tracking rooms right from the home page.
- **Worldwide Real-Time Sync**: Uses HiveMQ Cloud WebSocket relay to sync across mobile networks (4G/5G/Wi-Fi) worldwide.
- **GitHub Pages Ready**: 100% static frontend—enable GitHub Pages to get an instant `https://` domain for WhatsApp!

## 📂 Project Structure
- `index.html` — Rooms Hub & "Create Room" page.
- `admin.html` — Admin live Google Map view (only for the creator).
- `share.html` — Participant location check-in page (auto-activates GPS).
- `server.py` — Local Python server with automatic zero-config HTTPS tunnel.

## 🌐 Enable Free GitHub Pages (Live WhatsApp Links)
1. Push this repository to GitHub.
2. Go to **Settings** > **Pages**.
3. Under **Branch**, select `main` (or `master`) and click **Save**.
4. You will get a live HTTPS URL (e.g., `https://yourusername.github.io/map-location-finder/index.html`).
5. Share links from this URL will automatically display as **blue clickable hyperlinks with preview** in WhatsApp!
