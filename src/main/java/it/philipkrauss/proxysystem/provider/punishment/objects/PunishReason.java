package it.philipkrauss.proxysystem.provider.punishment.objects;

import it.philipkrauss.proxysystem.provider.punishment.PunishType;

public class PunishReason {

    private final int id;
    private final PunishType type;
    private final String reason;
    private final long duration;
    private final String durationText;
    private final long exemptedDuration;

    private PunishReason(int id, PunishType type, String reason, long duration, String durationText, long exemptedDuration) {
        this.id = id;
        this.type = type;
        this.reason = reason;
        this.duration = duration;
        this.durationText = durationText;
        this.exemptedDuration = exemptedDuration;
    }

    public int getId() {
        return id;
    }

    public PunishType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public long getDuration() {
        return duration;
    }

    public String getDurationText() {
        return durationText;
    }

    public long getExemptedDuration() {
        return exemptedDuration;
    }

    public static class Builder {

        private int id;
        private PunishType type;
        private String reason;
        private long duration;
        private String durationText;
        private long exemptedDuration;

        public Builder withId(int id) {
            this.id = id;
            return this;
        }

        public Builder withType(PunishType type) {
            this.type = type;
            return this;
        }

        public Builder withReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder withDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder withDurationText(String durationText) {
            this.durationText = durationText;
            return this;
        }

        public Builder withExemptedDuration(long exemptedDuration) {
            this.exemptedDuration = exemptedDuration;
            return this;
        }

        public PunishReason build() {
            return new PunishReason(id, type, reason, duration, durationText, exemptedDuration);
        }

    }

}
