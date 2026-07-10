import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

public class OcppServer extends WebSocketServer {

    public static final Map<String, ChargePoint> stations = new ConcurrentHashMap<>();
    public static final Set<String> cards = ConcurrentHashMap.newKeySet();
    public static final Map<Integer, String> activeTransactions = new ConcurrentHashMap<>();
    public static final AtomicInteger transactionIdGenerator = new AtomicInteger(1000);
    public static final Map<String, WebSocket> activeConnections = new ConcurrentHashMap<>();

    static {
        cards.add("VESTEL_CARD_01");
        cards.add("VESTEL_CARD_02");
        cards.add("A1B2C3D4");
        cards.add("ADMIN_REMOTE");
        System.out.println("[BOOT] " + cards.size() + " authorized RFID cards have been loaded into the system.");
    }

    public OcppServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String stationId = conn.getResourceDescriptor().replace("/", "");
        activeConnections.put(stationId, conn);
        System.out.println("\n[CONNECTION] New charge station connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String stationId = conn.getResourceDescriptor().replace("/", "");
        activeConnections.remove(stationId, conn);
        System.out.println("\n[DISCONNECTED] Station connection lost: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("\n[MESSAGE RECEIVED] Raw data from the station: " + message);

        try {

            JSONArray gelenDizi = new JSONArray(message);

            if (gelenDizi.length() == 4) {
                int mesajTipi = gelenDizi.getInt(0);
                String mesajId = gelenDizi.getString(1);
                String aksiyon = gelenDizi.getString(2);
                JSONObject payload = gelenDizi.getJSONObject(3);

                if (mesajTipi == OcppConstants.CALL) {
                    OcppMessageHandler handler = OcppHandlerFactory.getHandler(aksiyon);

                    if (handler != null) {
                        handler.handle(conn, mesajId, payload);
                    } else {
                        System.out.println("[WARNING] Unsupported or not yet implemented action: " + aksiyon);
                    }
                }
            }

        } catch (org.json.JSONException e) {
            System.out.println("[JSON ERROR] The incoming message is not in OCPP format or contains missing data.");
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("\n[ERROR] An error occurred!");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Vestel EVC Lite Server Started...");
        System.out.println("Charging stations are waiting. (Port: " + getPort() + ")\n");
    }

    public static void addCard(String newTag) {
        cards.add(newTag);
    }

    public static void removeCard(String cardTag) {
        if (!cards.isEmpty()) {
            cards.remove(cardTag);
        } else {
            System.out.println("There is no card to remove!");
        }
    }

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 8887;
        int uiPort = 8888;
        WebSocketServer server = new OcppServer(new InetSocketAddress(host, port));
        WebSocketServer uiServer = new UiWebSocketServer(new InetSocketAddress(host, uiPort));
        server.start();
        uiServer.start();
    }
}