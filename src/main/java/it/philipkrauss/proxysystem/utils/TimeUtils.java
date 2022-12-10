package it.philipkrauss.proxysystem.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {

    public static TimeUtils create() {
        return new TimeUtils();
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private TimeUtils() {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
    }

    public String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public String convertOnlineTime(long timeStamp) {
        int seconds = (int) (timeStamp / 1000);
        int minutes = 0;
        int hours = 0;
        int days = 0;
        while (seconds >= 60L) {
            seconds -= 60L;
            minutes++;
        }
        while (minutes >= 60) {
            minutes -= 60;
            hours++;
        }
        while (hours >= 24) {
            hours -= 24;
            days++;
        }
        StringBuilder formatBuilder = new StringBuilder();
        if(days >= 1) formatBuilder.append("§e").append(days).append("§7d ");
        if(hours >= 1) formatBuilder.append("§e").append(hours).append("§7h ");
        if(minutes >= 1) formatBuilder.append("§e").append(minutes).append("§7m");
        return formatBuilder.toString();
    }

}
