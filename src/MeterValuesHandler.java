import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class MeterValuesHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload, String stationId) {

        int transactionId = payload.getInt("transactionId");

        if (!OcppServer.activeTransactions.containsKey(transactionId)) {
            System.out.println("[GHOST METER] Ignoring offline MeterValues for old transaction: " + transactionId);

            JSONObject cevapPayload = new JSONObject();
            JSONArray cevapDizisi = new JSONArray();
            cevapDizisi.put(OcppConstants.CALL_RESULT);
            cevapDizisi.put(messageId);
            cevapDizisi.put(cevapPayload);
            conn.send(cevapDizisi.toString());

            return;
        }

        String meterString = "0";
        JSONArray meterValueArray = payload.optJSONArray("meterValue");

        if (meterValueArray != null && meterValueArray.length() > 0) {
            JSONArray sampledValueArray = meterValueArray.getJSONObject(0).optJSONArray("sampledValue");
            if (sampledValueArray != null && sampledValueArray.length() > 0) {
                meterString = sampledValueArray.getJSONObject(0).optString("value", "0");
            }
        }

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