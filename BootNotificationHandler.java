
// BootNotificationHandler.java
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Instant;

public class BootNotificationHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload) {
        ChargePoint station = new ChargePoint(payload.getString("stationId"), payload.getString("chargePointVendor"),
                payload.getString("chargePointModel"), ChargePointStatus.AVAILABLE, ChargePointErrorCode.NOERROR,
                Instant.now());

        OcppServer.stations.put(payload.getString("stationId"), station);
        
        String marka = payload.getString("chargePointVendor");
        String model = payload.getString("chargePointModel");
        System.out.println("-> [HANDLER] Station is waking up! Brand: " + marka + " | Model: " + model);

        JSONObject cevapPayload = new JSONObject();
        cevapPayload.put("status", "Accepted");
        cevapPayload.put("currentTime", Instant.now().toString());
        cevapPayload.put("interval", 300);

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(3);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);

        conn.send(cevapDizisi.toString());
        System.out.println("[RESPOND HAS BEEN SENT] " + cevapDizisi.toString());
    }
}