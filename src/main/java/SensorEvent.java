public class SensorEvent {
    public final String sensorId;
    public final long timestamp;
    public final double value;

    public SensorEvent(String sensorId, long timestamp, double value) {
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        this.value = value;
    }
}

