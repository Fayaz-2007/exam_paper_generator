#!/usr/bin/env python3
import http.server
import socketserver
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

PORT = 8000
Handler = http.server.SimpleHTTPRequestHandler

try:
    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        print(f"[OK] Frontend running at http://localhost:{PORT}")
        print(f"[OK] Login page: http://localhost:{PORT}/login.html")
        httpd.serve_forever()
except KeyboardInterrupt:
    print("\n[OK] Server stopped")
except Exception as e:
    print(f"[ERROR] {e}")
