import org.java_websocket.WebSocket;
import org.json.JSONObject;

public interface OcppMessageHandler {
    void handle(WebSocket conn, String messageId, JSONObject payload, String stationId);
}