import http.server
import socketserver
import socket
import webbrowser
import json
import os
import re
import subprocess
import threading
import time
import urllib.parse
from datetime import datetime

PORT = 8080
active_locations = {}
public_https_url = None

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

LOCAL_IP = get_local_ip()

def start_public_tunnel():
    """Starts an automatic zero-config public HTTPS tunnel (No GitHub, No Accounts required)"""
    global public_https_url
    try:
        cmd = ["ssh", "-R", f"80:localhost:{PORT}", "-o", "StrictHostKeyChecking=no", "-o", "ServerAliveInterval=30", "nokey@localhost.run"]
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
        
        for line in iter(process.stdout.readline, ''):
            if not line:
                break
            match = re.search(r'(https://[a-zA-Z0-9\-\.]+\.lhr\.life)', line)
            if match:
                public_https_url = match.group(1)
                print("\n" + "=" * 65)
                print("[LIVE PUBLIC HTTPS LINK READY FOR WHATSAPP (BLUE CLICKABLE LINK)]")
                print(f"   👉 {public_https_url}/share.html")
                print("=" * 65 + "\n")
                break
    except Exception as e:
        print(f"Tunnel info: {e}")

class FastLocationHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

    def do_GET(self):
        parsed_url = urllib.parse.urlparse(self.path)
        
        if parsed_url.path == '/api/config':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            # Prefer public HTTPS URL for WhatsApp clickable links
            base_url = public_https_url if public_https_url else f"http://{LOCAL_IP}:{PORT}"
            cfg = {
                'local_ip': LOCAL_IP,
                'port': PORT,
                'public_https_url': public_https_url,
                'network_base_url': base_url
            }
            self.wfile.write(json.dumps(cfg).encode('utf-8'))
            return

        if parsed_url.path == '/api/locations':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(active_locations).encode('utf-8'))
            return
            
        super().do_GET()

    def do_POST(self):
        parsed_url = urllib.parse.urlparse(self.path)
        
        if parsed_url.path == '/api/update':
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length)
            
            try:
                data = json.loads(post_data.decode('utf-8'))
                user_id = data.get('id', 'user_' + str(len(active_locations) + 1))
                
                active_locations[user_id] = {
                    'id': user_id,
                    'name': data.get('name', 'Anonymous'),
                    'lat': float(data.get('lat', 0)),
                    'lng': float(data.get('lng', 0)),
                    'accuracy': data.get('accuracy', 0),
                    'timestamp': datetime.now().strftime('%I:%M:%S %p'),
                    'last_updated': datetime.now().isoformat()
                }
                
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({'status': 'success', 'id': user_id}).encode('utf-8'))
                return
            except Exception as e:
                self.send_response(400)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({'status': 'error', 'message': str(e)}).encode('utf-8'))
                return

        super().do_POST()

if __name__ == "__main__":
    web_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(web_dir)

    # Start automatic background public HTTPS tunnel
    tunnel_thread = threading.Thread(target=start_public_tunnel, daemon=True)
    tunnel_thread.start()

    print("=" * 65)
    print("MULTI-PERSON LOCATION SERVER RUNNING")
    print("=" * 65)
    print(f"\n[ADMIN DASHBOARD (YOUR BROWSER)]:")
    print(f"   --> http://localhost:{PORT}/index.html")
    print(f"\n[LOCAL WI-FI LINK]:")
    print(f"   --> http://{LOCAL_IP}:{PORT}/share.html")
    print("\nStarting automatic Public HTTPS link generation...")
    print("=" * 65 + "\n")

    webbrowser.open(f"http://localhost:{PORT}/index.html")

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), FastLocationHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")
