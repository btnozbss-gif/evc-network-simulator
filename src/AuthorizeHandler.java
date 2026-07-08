import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class AuthorizeHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload) {

        String stationId = conn.getResourceDescriptor().replace("/", "");

        String idTag = payload.getString("idTag");
        System.out.println("\n-> [AUTHORIZE] Station ID: " + stationId + " | Verifying RFID Tag: " + idTag);
        JSONObject idTagInfo = new JSONObject();

        if (OcppServer.cards.contains(idTag)) {
            idTagInfo.put("status", "Accepted");
            System.out.println("[RESULT] Card ACCEPTED. Authorization successful.");
        } else {
            idTagInfo.put("status", "Invalid");
            System.out.println("   [RESULT] Card REJECTED! Unauthorized access attempt.");
        }

        JSONObject cevapPayload = new JSONObject();
        cevapPayload.put("idTagInfo", idTagInfo);

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(3);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);

        conn.send(cevapDizisi.toString());
        System.out.println("[RESPOND HAS BEEN SENT] " + cevapDizisi.toString());
    }
}
