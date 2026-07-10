
import java.util.HashMap;
import java.util.Map;

public class OcppHandlerFactory {

    private static final Map<String, OcppMessageHandler> handlers = new HashMap<>();

    static {

        handlers.put("BootNotification", new BootNotificationHandler());
        handlers.put("Heartbeat", new HeartbeatHandler());
        handlers.put("StatusNotification", new StatusNotificationHandler());
        handlers.put("Authorize", new AuthorizeHandler());
        handlers.put("StartTransaction", new StartTransactionHandler());
        handlers.put("StopTransaction", new StopTransactionHandler());
        handlers.put("MeterValues", new MeterValuesHandler());

    }

    public static OcppMessageHandler getHandler(String action) {
        return handlers.get(action);
    }
}