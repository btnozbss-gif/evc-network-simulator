import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

public class UiWebSocketServer extends WebSocketServer {

    public static final Set<WebSocket> clients = ConcurrentHashMap.newKeySet();

    public UiWebSocketServer(InetSocketAddress inetSocketAddress) {
        super(inetSocketAddress);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        System.out.println("\n[CONNECTION] Browser connected: " + conn.getRemoteSocketAddress());

        for (ChargePoint station : OcppServer.stations.values()) {
            try {
                JSONObject bootMsg = new JSONObject();
                bootMsg.put("action", "BootNotification");
                bootMsg.put("stationId", station.getStationId());
                bootMsg.put("chargePointVendor", station.getVendor());
                bootMsg.put("chargePointModel", station.getModel());
                conn.send(bootMsg.toString());

                JSONObject statusMsg = new JSONObject();
                statusMsg.put("action", "StatusNotification");
                statusMsg.put("stationId", station.getStationId());
                statusMsg.put("status", station.getStatus().name());
                statusMsg.put("errorCode", station.getErrorCode().name());
                conn.send(statusMsg.toString());

            } catch (Exception e) {
                System.out.println("[SYNC ERROR] Failed to sync station data to UI.");
            }
        }
        broadcastCardList();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        System.out.println("\n[DISCONNECTED] Browser connection lost: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("\n[MESSAGE RECEIVED] Raw data from the browser: " + message);

        try {
            JSONObject payload = new JSONObject(message);
            String action = payload.optString("action");

            switch (action) {
                case "AddCard":
                    String addTag = payload.optString("idTag");
                    if (addTag != null && !addTag.isEmpty()) {
                        OcppServer.addCard(addTag);
                        System.out.println("-> [UI COMMAND] Added Card: " + addTag);
                        broadcastCardList();
                    }
                    break;

                case "RemoveCard":
                    String removeTag = payload.optString("idTag");
                    if (removeTag != null && !removeTag.isEmpty()) {
                        OcppServer.removeCard(removeTag);
                        System.out.println("-> [UI COMMAND] Removed Card: " + removeTag);
                        broadcastCardList();
                    }
                    break;
                case "RemoteCommand":
                    String targetStation = payload.optString("stationId");
                    String cmd = payload.optString("command");
                    int txId = payload.optInt("transactionId", 0);

                    String uiKarti = payload.optString("idTag", "ADMIN_REMOTE");

                    System.out.println(
                            "\n-> [UI COMMAND] Sending " + cmd + " command to station " + targetStation + "...");

                    String msgId = UUID.randomUUID().toString();
                    JSONArray ocppPaketi = new JSONArray();
                    ocppPaketi.put(OcppConstants.CALL);
                    ocppPaketi.put(msgId);

                    JSONObject cmdPayload = new JSONObject();
                    if (cmd.equals("RemoteStartTransaction")) {
                        ocppPaketi.put("RemoteStartTransaction");
                        cmdPayload.put("idTag", uiKarti);
                        cmdPayload.put("connectorId", 1);
                    } else if (cmd.equals("RemoteStopTransaction")) {
                        ocppPaketi.put("RemoteStopTransaction");
                        cmdPayload.put("transactionId", txId);
                    } else if (cmd.equals("TriggerMessage")) {
                        ocppPaketi.put("TriggerMessage");
                        String reqMsg = payload.optString("requestedMessage", "StatusNotification");
                        cmdPayload.put("requestedMessage", reqMsg);
                    } else if (cmd.equals("SoftReset")) {
                        ocppPaketi.put("Reset");
                        cmdPayload.put("type", "Soft");
                    } else if (cmd.equals("HardReset")) {
                        ocppPaketi.put("Reset");
                        cmdPayload.put("type", "Hard");
                    } else if (cmd.equals("UnlockConnector")) {
                        ocppPaketi.put("UnlockConnector");
                        cmdPayload.put("connectorId", 1);
                    }
                    ocppPaketi.put(cmdPayload);

                    WebSocket cpConn = OcppServer.activeConnections.get(targetStation);
                    if (cpConn != null && cpConn.isOpen()) {
                        cpConn.send(ocppPaketi.toString());
                        System.out.println("   [OCPP SENT] " + ocppPaketi.toString());
                    } else {
                        System.out.println("   [ERROR] Station connection not found or disconnected!");
                    }
                    break;

                case "GetCards":
                    broadcastCardList();
                    break;

                default:
                    System.out.println("[UI WARNING] Unknown action from browser: " + action);
                    break;
            }

        } catch (Exception e) {
            System.out.println("[UI ERROR] Incoming message from browser is not a valid JSON command.");
        }
    }

    public static void broadcastCardList() {
        try {
            JSONObject response = new JSONObject();
            response.put("action", "UpdateCardList");

            JSONArray cardArray = new JSONArray();
            for (String card : OcppServer.cards) {
                cardArray.put(card);
            }
            response.put("cards", cardArray);

            broadcastToUi(response.toString());
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to broadcast card list.");
        }
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
