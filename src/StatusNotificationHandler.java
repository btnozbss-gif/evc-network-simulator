import java.time.Instant;
import java.util.Locale;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

enum ChargePointStatus {
    AVAILABLE, PREPARING, CHARGING, FINISHING, FAULTED, UNAVAILABLE
}

enum ChargePointErrorCode {
    NOERROR, HIGHTEMPERATURE, READERFAILURE, GROUNDFAILURE
}

public class StatusNotificationHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload) {
        int connectorId = payload.getInt("connectorId");
        String errorCode = payload.getString("errorCode");
        String status = payload.getString("status");

        JSONObject cevapPayload = new JSONObject();

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(3);
        cevapDizisi.put(messageId);
        cevapDizisi.put(cevapPayload);
        try {
            ChargePointStatus enumState = ChargePointStatus.valueOf(status.toUpperCase(Locale.ENGLISH));
            ChargePointErrorCode errorState = ChargePointErrorCode.valueOf(errorCode.toUpperCase(Locale.ENGLISH));

            switch (enumState) {
                case AVAILABLE:
                    System.out.println("Device available to use." + connectorId);
                    break;
                case PREPARING:
                    System.out.println("Device preparing to charging." + connectorId);
                    break;
                case CHARGING:
                    System.out.println("Device is charging." + connectorId);
                    break;
                case FINISHING:
                    System.out.println("Device completed charging." + connectorId);
                    break;
                case FAULTED:
                    switch (errorState) {
                        case NOERROR:
                            System.out.println("There is no error in system!!!" + connectorId);
                            break;
                        case HIGHTEMPERATURE:
                            System.out.println("High temperature warning!!!" + connectorId);
                            break;
                        case READERFAILURE:
                            System.out.println("Card reading warning!!!" + connectorId);
                            break;
                        default:
                            System.out.println("Unnamed warning!!!" + connectorId);
                            break;
                    }
                    break;
                default:
                    System.out.println("Device is closed." + connectorId);
                    break;

            }
            String stationId = conn.getResourceDescriptor().replace("/", "");
            ChargePoint availableStation = OcppServer.stations.get(stationId);
            if (availableStation != null) {
                availableStation.setStatus(enumState);
                availableStation.setErrorCode(errorState);
                availableStation.setLastSeen(Instant.now());
            } else {
                System.out.println("[WARNING] A device not registered in the system reported a status.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        conn.send(cevapDizisi.toString());
        System.out.println("[STATE HAS BEEN SENT] " + cevapDizisi.toString());
    }
}
