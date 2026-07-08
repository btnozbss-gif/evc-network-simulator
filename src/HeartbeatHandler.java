import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Instant;

public class HeartbeatHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload) {

        String stationId = conn.getResourceDescriptor().replace("/", "");
        ChargePoint station = OcppServer.stations.get(stationId);

        System.out.println("Station has sent heartbeat! Station ID: " + stationId);

        JSONObject cevapPayload = new JSONObject();
        cevapPayload.put("currentTime", Instant.now().toString());

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(3);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);

        if (station != null) {
            station.setLastSeen(Instant.now());
        }
        conn.send(cevapDizisi.toString());
        System.out.println("[RESPOND HAS BEEN SENT] " + cevapDizisi.toString());

    }

}
