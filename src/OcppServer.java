import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;
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

        activeTransactions.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(stationId)) {
                System.out.println("[CLEANUP] Orphaned transaction " + entry.getKey()
                        + " removed for disconnected station: " + stationId);
                return true;
            }
            return false;
        });

        activeConnections.remove(stationId);
        stations.remove(stationId);

        System.out.println("\n[DISCONNECTED] Station connection lost: " + stationId);

        try {
            JSONObject uiMessage = new JSONObject();
            uiMessage.put("action", "StationDisconnected");
            uiMessage.put("stationId", stationId);
            UiWebSocketServer.broadcastToUi(uiMessage.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("\n[MESSAGE RECEIVED] Raw data from the station: " + message);
        String mesajId = "";

        try {
            JSONArray gelenDizi = new JSONArray(message);
            int mesajTipi = gelenDizi.getInt(0);

            if (mesajTipi == OcppConstants.CALL && gelenDizi.length() == 4) {
                mesajId = gelenDizi.getString(1);
                String aksiyon = gelenDizi.getString(2);
                JSONObject payload = gelenDizi.getJSONObject(3);

                OcppMessageHandler handler = OcppHandlerFactory.getHandler(aksiyon);

                if (handler != null) {
                    String stationId = conn.getResourceDescriptor().replace("/", "");
                    try {
                        handler.handle(conn, mesajId, payload, stationId);
                    } catch (JSONException payloadEx) {
                        System.out.println("[PAYLOAD ERROR] Missing required fields in action: " + aksiyon);
                        sendCallError(conn, mesajId, "FormatViolation", "Missing required fields", new JSONObject());
                    } catch (Exception ex) {
                        System.out.println("[INTERNAL SERVER ERROR] Exception in handler: " + aksiyon);
                        ex.printStackTrace();
                        sendCallError(conn, mesajId, "InternalError", "Unexpected error", new JSONObject());
                    }
                } else {
                    System.out.println("[WARNING] Unsupported action requested by station: " + aksiyon);
                    sendCallError(conn, mesajId, "NotImplemented", "Requested action is not supported",
                            new JSONObject());
                }
            } else if (mesajTipi == OcppConstants.CALL_RESULT && gelenDizi.length() == 3) {
                mesajId = gelenDizi.getString(1);
                JSONObject payload = gelenDizi.getJSONObject(2);
                System.out.println("[CALLRESULT RECEIVED] Station acknowledged our request. Message ID: " + mesajId
                        + " | Status: " + payload.optString("status", "Unknown"));
            } else {
                System.out.println("[WARNING] Incoming JSON array is not a valid CALL or CALLRESULT. Length: "
                        + gelenDizi.length());
            }

        } catch (org.json.JSONException e) {
            System.out.println("[JSON ERROR] The incoming message is not a valid OCPP array format.");
            if (!mesajId.isEmpty()) {
                sendCallError(conn, mesajId, "FormatViolation", "Payload is not a valid JSON array", new JSONObject());
            }
        }
    }

    private void sendCallError(WebSocket conn, String messageId, String errorCode, String errorDescription,
            JSONObject errorDetails) {
        JSONArray errorArray = new JSONArray();
        errorArray.put(OcppConstants.CALL_ERROR);
        errorArray.put(messageId);
        errorArray.put(errorCode);
        errorArray.put(errorDescription);
        errorArray.put(errorDetails);

        if (conn.isOpen()) {
            conn.send(errorArray.toString());
            System.out.println("[CALL_ERROR SENT] " + errorArray.toString());
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