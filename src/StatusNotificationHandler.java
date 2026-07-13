import java.time.Instant;
import java.util.Locale;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

enum ChargePointStatus {
    AVAILABLE, PREPARING, CHARGING, SUSPENDEDEV, SUSPENDEDEVSE, FINISHING, FAULTED, UNAVAILABLE
}

enum ChargePointErrorCode {
    NOERROR, HIGHTEMPERATURE, READERFAILURE, GROUNDFAILURE, OTHERERROR, OVERCURRENTFAILURE, UNDERVOLTAGE, OVERVOLTAGE
}

public class StatusNotificationHandler implements OcppMessageHandler {

    @Override
    public void handle(WebSocket conn, String messageId, JSONObject payload, String stationId) {
        int connectorId = payload.getInt("connectorId");
        String errorCode = payload.getString("errorCode");
        String status = payload.getString("status");

        ChargePoint availableStation = OcppServer.stations.get(stationId);

        JSONObject uiMessage = new JSONObject();
        uiMessage.put("action", "StatusNotification");
        uiMessage.put("stationId", stationId);
        uiMessage.put("connectorId", connectorId);

        JSONObject cevapPayload = new JSONObject();

        JSONArray cevapDizisi = new JSONArray();
        cevapDizisi.put(OcppConstants.CALL_RESULT);
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
                case UNAVAILABLE:
                    System.out.println("Device unailable to use. " + connectorId);
                    break;
                case SUSPENDEDEV:
                    System.out.println("Charging suspended by EV (Car paused). " + connectorId);
                    break;
                case SUSPENDEDEVSE:
                    System.out.println("Charging suspended by EVSE (Station paused). " + connectorId);
                    break;
                case FAULTED:
                    switch (errorState) {
                        case NOERROR:
                            break;
                        case OTHERERROR:
                            System.out.println(
                                    "[⚠️ ERROR LOG] Station reported 'OtherError' (Check Proximity Pilot / Cable connection).");
                            break;

                        case GROUNDFAILURE:
                            System.out.println("[🚨 CRITICAL] Ground failure detected on station!");
                            break;

                        case OVERCURRENTFAILURE:
                            System.out.println("[🚨 CRITICAL] Overcurrent failure detected!");
                            break;

                        case UNDERVOLTAGE:
                            System.out.println("[⚠️ WARNING] Undervoltage detected on grid line.");
                            break;

                        case OVERVOLTAGE:
                            System.out.println("[⚠️ WARNING] Overvoltage detected on grid line.");
                            break;
                        default:
                            System.out.println("Undefined Error!");
                            break;
                    }
                    break;
                default:
                    System.out.println("Device is closed." + connectorId);
                    break;

            }
            uiMessage.put("status", enumState.name());
            uiMessage.put("errorCode", errorState.name());

            if (availableStation != null) {
                availableStation.setStatus(enumState);
                availableStation.setErrorCode(errorState);
                availableStation.setLastSeen(Instant.now());
            } else {
                System.out.println("[WARNING] A device not registered in the system reported a status.");
            }

            UiWebSocketServer.broadcastToUi(uiMessage.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        conn.send(cevapDizisi.toString());
        System.out.println("[STATE HAS BEEN SENT] " + cevapDizisi.toString());
    }
}
