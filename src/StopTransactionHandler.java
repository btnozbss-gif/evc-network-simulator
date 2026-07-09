import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class StopTransactionHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload) {

        String stationId = conn.getResourceDescriptor().replace("/", "");
        int transactionId = payload.getInt("transactionId");
        int meterStop = payload.optInt("meterStop", 0);

        System.out.println("\n[TRANSACTION END] Station " + stationId + " requested to stop transaction "
                + transactionId + ". Final Meter: " + meterStop + " Wh");

        OcppServer.activeTransactions.remove(transactionId);
        ChargePoint station = OcppServer.stations.get(stationId);

        JSONObject idTagInfo = new JSONObject();
        idTagInfo.put("status", "Accepted");

        if (station != null) {
            station.setStatus(ChargePointStatus.FINISHING);

            System.out.println("-> [RESULT] Transaction " + transactionId + " successfully closed. Total Energy: "
                    + meterStop + " Wh");
        } else {
            System.out.println("-> [WARNING] Station " + stationId + " is missing in memory, but transaction "
                    + transactionId + " is force-closed.");
        }

        JSONObject uiMessage = new JSONObject();
        uiMessage.put("action", "StopTransaction");
        uiMessage.put("stationId", stationId);
        uiMessage.put("transactionId", transactionId);
        uiMessage.put("meterStop", meterStop);

        JSONObject cevapPayload = new JSONObject();
        cevapPayload.put("idTagInfo", idTagInfo);

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(OcppConstants.CALL_RESULT);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);

        UiWebSocketServer.broadcastToUi(uiMessage.toString());
        conn.send(cevapDizisi.toString());
        System.out.println(" [RESPOND HAS BEEN SENT] " + cevapDizisi.toString());
    }

}
