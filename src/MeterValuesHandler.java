import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class MeterValuesHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload) {

        String stationId = conn.getResourceDescriptor().replace("/", "");
        int transactionId = payload.getInt("transactionId");

        String meterString = payload.getJSONArray("meterValue")
                .getJSONObject(0)
                .getJSONArray("sampledValue")
                .getJSONObject(0)
                .getString("value");

        System.out.println("-> [METER] Station: " + stationId + " | Energy: " + meterString + " Wh");

        JSONObject uiMessage = new JSONObject();
        uiMessage.put("action", "MeterValues");
        uiMessage.put("stationId", stationId);
        uiMessage.put("transactionId", transactionId);
        uiMessage.put("meterValue", meterString);

        UiWebSocketServer.broadcastToUi(uiMessage.toString());

        JSONObject cevapPayload = new JSONObject();

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(OcppConstants.CALL_RESULT);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);

        conn.send(cevapDizisi.toString());
    }
}