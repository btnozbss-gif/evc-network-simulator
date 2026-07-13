import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class AuthorizeHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload, String stationId) {

        String idTag = payload.getString("idTag");
        System.out.println("\n-> [AUTHORIZE] Station ID: " + stationId + " | Verifying RFID Tag: " + idTag);

        JSONObject idTagInfo = new JSONObject();
        JSONObject uiMessage = new JSONObject();

        if (OcppServer.cards.contains(idTag)) {
            idTagInfo.put("status", OcppConstants.STATUS_ACCEPTED);
            System.out.println("[RESULT] Card ACCEPTED. Authorization successful.");
        } else {
            idTagInfo.put("status", OcppConstants.STATUS_INVALID);
            System.out.println("[RESULT] Card REJECTED! Unauthorized access attempt.");
        }
        uiMessage.put("action", "Authorize");
        uiMessage.put("stationId", stationId);
        uiMessage.put("idTag", idTag);
        uiMessage.put("status", idTagInfo.getString("status"));

        JSONObject cevapPayload = new JSONObject();
        cevapPayload.put("idTagInfo", idTagInfo);

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(OcppConstants.CALL_RESULT);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);
        UiWebSocketServer.broadcastToUi(uiMessage.toString());
        conn.send(cevapDizisi.toString());
        System.out.println("[RESPOND HAS BEEN SENT] " + cevapDizisi.toString());
    }
}
