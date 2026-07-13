import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Instant;

public class HeartbeatHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload, String stationId) {

        ChargePoint station = OcppServer.stations.get(stationId);

        System.out.println("Station has sent heartbeat! Station ID: " + stationId);

        JSONObject uiMessage = new JSONObject();
        uiMessage.put("action", "Heartbeat");
        uiMessage.put("stationId", stationId);
        uiMessage.put("currentTime", Instant.now().toString());

        JSONObject cevapPayload = new JSONObject();
        cevapPayload.put("currentTime", Instant.now().toString());

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(OcppConstants.CALL_RESULT);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);

        if (station != null) {
            station.setLastSeen(Instant.now());
        }
        UiWebSocketServer.broadcastToUi(uiMessage.toString());
        conn.send(cevapDizisi.toString());
        System.out.println("[RESPOND HAS BEEN SENT] " + cevapDizisi.toString());

    }

}
