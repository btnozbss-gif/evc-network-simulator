
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class StartTransactionHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload, String stationId) {

        String idTag = payload.getString("idTag");
        int meterStart = payload.optInt("meterStart", 0);
        System.out.println("\n[TRANSACTION] Station " + stationId + " requested to start a transaction. RFID: " + idTag
                + " | Meter: " + meterStart + " Wh");

        JSONObject idTagInfo = new JSONObject();
        int transactionId = 0;

        ChargePoint station = OcppServer.stations.get(stationId);

        if (station != null && OcppServer.cards.contains(idTag)) {
            idTagInfo.put("status", OcppConstants.STATUS_ACCEPTED);
            transactionId = OcppServer.transactionIdGenerator.incrementAndGet();
            OcppServer.activeTransactions.put(transactionId, stationId);
            station.setStatus(ChargePointStatus.CHARGING);
            System.out.println("-> [RESULT] Authorization APPROVED. Transaction is starting... (Transaction ID: "
                    + transactionId + ")");
        } else {
            System.out.println("-> [RESULT] Authorization REJECTED! Invalid or unregistered RFID card.");
            idTagInfo.put("status", OcppConstants.STATUS_INVALID);
        }

        JSONObject uiMessage = new JSONObject();
        uiMessage.put("action", "StartTransaction");
        uiMessage.put("stationId", stationId);
        uiMessage.put("idTag", idTag);
        uiMessage.put("transactionId", transactionId);
        uiMessage.put("status", idTagInfo.getString("status"));

        JSONObject cevapPayload = new JSONObject();
        cevapPayload.put("idTagInfo", idTagInfo);
        cevapPayload.put("transactionId", transactionId);

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(OcppConstants.CALL_RESULT);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);

        UiWebSocketServer.broadcastToUi(uiMessage.toString());
        conn.send(cevapDizisi.toString());
        System.out.println("[RESPOND HAS BEEN SENT] " + cevapDizisi.toString());

    }

}
