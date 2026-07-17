# ⚡ Vestel EVC Lite - OCPP 1.6 CSMS Platform
> Modern, Asynchronous, and High-Performance Electric Vehicle Charging Station Management System (CSMS)

![Platform](https://img.shields.io/badge/Platform-Web-blue)
![Java](https://img.shields.io/badge/Backend-Java_WebSocket-orange)
![Frontend](https://img.shields.io/badge/Frontend-Vanilla_JS-yellow)
![OCPP](https://img.shields.io/badge/Protocol-OCPP_1.6-brightgreen)

Vestel EVC Lite is a comprehensive simulation and management platform designed to monitor, control, and bill electric vehicle charging stations (Charge Points) in real-time via a centralized cloud architecture. The system communicates asynchronously with devices using the standard **OCPP 1.6 (Open Charge Point Protocol)** over a WebSocket bridge.

## 🚀 Key Features

*   **Real-Time Network Topology:** Live SVG animations to monitor data and power flows between the station and the cloud.
*   **Full OCPP 1.6 Support:** Instant processing of `BootNotification`, `Heartbeat`, `StatusNotification`, `MeterValues`, `Authorize`, and `Start/Stop Transaction` payloads.
*   **Remote Operations:** Send remote commands seamlessly from the dashboard, including `RemoteStartTransaction`, `RemoteStopTransaction`, `UnlockConnector`, `Soft/Hard Reset`, and `TriggerMessage`.
*   **Fault-Tolerant Architecture:** Features automated Orphaned Transaction cleanup, an "Exponential Backoff" algorithm for auto-reconnection, and smart UI recovery during network outages.
*   **Cyberpunk UI & Glassmorphism:** A high-performance, DOM-optimized interface built entirely with Vanilla JS, HTML5, and CSS3—no external frontend frameworks required.
*   **Holographic Billing:** Dynamic receipt and invoice generation engine based on real-time energy consumption (Wh) upon transaction completion.

## 🛠️ Tech Stack

*   **Backend:** Java (Java-WebSocket Library)
*   **Frontend:** HTML5, CSS3 (Glassmorphism & Cyberpunk Theme), Vanilla JavaScript
*   **Data Format & Comm:** JSON, WebSocket (ws://)
*   **Data Visualization:** Chart.js (Live Power Consumption Metrics)

---

## ⚙️ Installation & Usage

Follow these steps to run and test the system on your local machine (Localhost).

### 1. Starting the Backend (Java Server)
Open a terminal in the root directory of the project and run the following commands to compile and start the server:

```bash
# Compile the project
javac -d bin -cp ".:lib/*" src/*.java

# Start the server
java -cp ".:lib/*:bin" OcppServer

> **Note:** Once started, the server listens for OCPP devices on port `8887` and serves the Web UI bridge on port `8888`.

## 2. Accessing the Frontend (Management Panel)

After the Java server is up and running, open the `dashboard.html` file in any modern web browser.

### 🔐 System Administrator Credentials
Use the following credentials to log in and gain access to authorized controls:

- **Username:** `admin`
- **Password:** `vestel123`

---

## 🧪 Testing & Simulation

You can test the project workflow by following these steps:

1. **Login:** Enter the system using `admin` / `vestel123`.
2. **Connect:** Click the **🔌 Connect to Server** button on the left sidebar to establish the UI WebSocket bridge.
3. **Add a Station:** If you don't have a physical charging station, use an OCPP simulator tool (e.g., Postman WebSocket or a NodeJS OCPP Client) to connect to `ws://localhost:8887/CP-01` and send a `BootNotification` payload. The station will instantly appear on the dashboard.
4. **Control:** Click on the station card on the grid to open the off-canvas panel for detailed analytics and remote operations.

> **Developer Note:** This project was developed to demonstrate advanced software architecture capabilities and asynchronous data communication standards. The UI log terminal features CSS-based typewriter optimizations, ensuring stable performance and preventing layout thrashing even under heavy load.

### 🏎️ Legacy Load Tester (Conceptual UI Mockup)
This repository also includes an earlier load testing concept located in the `testSimulator/evcTestSimulator.html` file. 

> **⚠️ Important Note:** The core application and the fully operational CSMS dashboard is **`dashboard.html`**. The test simulator was designed as an initial UI stress-test mockup. Due to its legacy single-socket multiplexing approach, it is **not** fully compatible with the current, strict OCPP 1.6 WebSocket backend architecture. It remains in the repository solely to demonstrate advanced DOM manipulation, visual load-testing logic, and SVG animation capabilities under simulated heavy data flow.