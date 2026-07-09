import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class UiWebSocketServer extends WebSocketServer {

    public static final Set<WebSocket> clients = ConcurrentHashMap.newKeySet();

    public UiWebSocketServer(InetSocketAddress inetSocketAddress) {
        super(inetSocketAddress);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        System.out.println("\n[CONNECTION] Browser connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        System.out.println("\n[DISCONNECTED] Browser connection lost: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("\n[MESSAGE RECEIVED] Raw data from the browser: " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("\n[ERROR] An error occurred!");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Vestel EVC Lite UI Server Started...");
        System.out.println("Waiting for dashboards (Port: " + getPort() + ")\n");
    }

    public static void broadcastToUi(String text) {
        for (WebSocket client : clients) {
            if (client.isOpen()) {
                client.send(text);
            }
        }

    }

}
