package it.philipkrauss.proxysystem.provider.punishment.objects;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Punishment {

    private final PunishReason reason;
    private final String punisher;
    private final long end;

    private Punishment(PunishReason reason, String punisher, long end) {
        this.reason = reason;
        this.punisher = punisher;
        this.end = end;
    }

    public PunishReason getReason() {
        return reason;
    }

    public String getPunisher() {
        return punisher;
    }

    public long getEnd() {
        return end;
    }

    public String getEndDate() {
        if(this.end == -1) return "§cPermanent";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return simpleDateFormat.format(new Date(this.end));
    }

    public boolean isPresent() {
        if(this.end == -1) return true;
        return this.end > System.currentTimeMillis();
    }

    public static class Builder {

        private String target;
        private String address;
        private PunishReason reason;
        private String punisher;
        private long end;

        public Builder withTarget(String target) {
            this.target = target;
            return this;
        }

        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder withReason(PunishReason reason) {
            this.reason = reason;
            return this;
        }

        public Builder withPunisher(String punisher) {
            this.punisher = punisher;
            return this;
        }

        public Builder withEnd(long end) {
            this.end = end;
            return this;
        }

        public Punishment build() {
            return new Punishment(reason, punisher, end);
        }

    }

}
