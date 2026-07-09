
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Instant;

public class BootNotificationHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload) {

        String stationId = conn.getResourceDescriptor().replace("/", "");

        ChargePoint station = new ChargePoint(
                stationId,
                payload.getString("chargePointVendor"),
                payload.getString("chargePointModel"),
                ChargePointStatus.AVAILABLE,
                ChargePointErrorCode.NOERROR,
                Instant.now());

        OcppServer.stations.put(stationId, station);

        JSONObject uiMessage = new JSONObject();
        uiMessage.put("action", "BootNotification");
        uiMessage.put("stationId", stationId);
        uiMessage.put("chargePointVendor", station.getVendor());
        uiMessage.put("chargePointModel", station.getModel());

        String marka = payload.getString("chargePointVendor");
        String model = payload.getString("chargePointModel");
        System.out.println(
                "-> [HANDLER] Station is waking up! ID: " + stationId + " | Brand: " + marka + " | Model: " + model);

        JSONObject cevapPayload = new JSONObject();
        cevapPayload.put("status", "Accepted");
        cevapPayload.put("currentTime", Instant.now().toString());
        cevapPayload.put("interval", 30);

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(OcppConstants.CALL_RESULT);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);
        UiWebSocketServer.broadcastToUi(uiMessage.toString());
        conn.send(cevapDizisi.toString());
        System.out.println("[RESPOND HAS BEEN SENT] " + cevapDizisi.toString());
    }
}