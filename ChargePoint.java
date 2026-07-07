import java.time.Instant;

public class ChargePoint {

    private String stationId;
    private String vendor;
    private String model;
    private volatile ChargePointStatus status;
    private volatile ChargePointErrorCode errorCode;
    private java.time.Instant lastSeen;

    public ChargePoint(String stationId, String vendor, String model, ChargePointStatus status,
            ChargePointErrorCode errorCode, Instant lastSeen) {
        this.stationId = stationId;
        this.vendor = vendor;
        this.model = model;
        this.status = status;
        this.errorCode = errorCode;
        this.lastSeen = lastSeen;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public ChargePointErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ChargePointErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public java.time.Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(java.time.Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public ChargePointStatus getStatus() {
        return status;
    }

    public void setStatus(ChargePointStatus status) {
        this.status = status;
    }

}
