# 🚀 EVC Network Simulator - OCPP PoC

A lightweight, real-time Electric Vehicle Charging (EVC) Station simulator designed to eliminate hardware dependencies for backend testing. Built with a robust State Machine architecture and WebSocket communication.

> **🏢 Corporate Context:** This project was developed as a Proof of Concept (PoC) during a Computer Engineering Internship at Vestel's R&D department. The primary goal was to provide a reliable sandbox environment for server-side testing without requiring physical charging station hardware.

## 🎯 Purpose
Testing backend operations for a network of 50+ EVC stations requires significant physical hardware, time, and resources. This simulator mimics real hardware behaviors, mechanical delays, and network traffic, dramatically accelerating the backend development and testing phases.

## ⚙️ Core Features & Engineering Solutions
* **Hardware Lock (Security):** Software-level locks disable RFID readers when a station is in `Faulted` or `Unavailable` mode, mirroring physical hardware limitations.
* **State Machine Protection:** Strict transition rules prevent impossible state changes (e.g., an `Available` station cannot instantly jump to `Charging` without an ID tag and a transaction ID).
* **Ghost Transaction Prevention:** If an active charging session is forcefully interrupted, the system automatically sends a `StopTransaction` to prevent billing data corruption.
* **Race Condition Mitigation:** Asynchronous flags are implemented to prevent UI and WebSocket spamming during the simulated 3-second hardware relay closures.
* **Boot Storm Management:** Jitter applied to startup sequences to prevent server DDoS during network-wide reboots.

## 🛠️ Tech Stack
* **Backend:** Java (Multithreaded, ConcurrentHashMaps, WebSocket Server)
* **Frontend:** Vanilla JavaScript, HTML5, CSS3 (No external dependencies)
* **Protocol:** OCPP-inspired custom JSON messaging over WebSockets.

## 🚀 Getting Started (How to Run)

**1. Start the Java Backend:**
* Open the project in your preferred IDE (IntelliJ IDEA, Eclipse, or VS Code).
* Ensure the `.jar` files inside the `lib/` folder are added to your project's Build Path/Classpath.
* Run the `OcppServer.java` file. The server will start listening for WebSocket connections.

**2. Start the Simulator Client:**
* Simply double-click the `test.html` file to open it in any modern web browser.
* Use the interface to trigger BootNotifications, Authorize ID tags, and monitor the terminal logs in real-time.

## 🔮 Future Roadmap
- [ ] **MeterValues Integration:** Streaming real-time KW/h consumption data every 10 seconds.
- [ ] **Database Persistence:** Connecting SQLite/PostgreSQL to log billing and transaction histories.
- [ ] **Offline Detection:** Server-side heartbeat monitoring to detect disconnected stations.
