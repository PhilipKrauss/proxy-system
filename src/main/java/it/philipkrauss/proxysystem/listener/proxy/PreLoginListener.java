package it.philipkrauss.proxysystem.listener.proxy;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import it.philipkrauss.proxysystem.provider.punishment.PunishProvider;
import it.philipkrauss.proxysystem.provider.punishment.PunishType;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.punishment.objects.Punishment;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreLoginListener {

    private static final int BYPASS_BAN_REASON = 302;
    private static final int UNALLOWED_NAME_REASON = 303;

    private final ProxySystem instance;
    private final PunishProvider punishProvider;

    public PreLoginListener() {
        this.instance = ProxySystem.getInstance();
        this.punishProvider = instance.getPunishProvider();
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        String username = event.getUsername();
        UUID uniqueId = UUIDProvider.getUniqueId(username);
        if (uniqueId == null) return;
        String hostAddress = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
        this.checkProfileName(uniqueId, hostAddress, username);
        if (!punishProvider.isPunished(uniqueId.toString(), PunishType.BAN)) {
            if (hostAddress == null) return;
            if (!punishProvider.isPunished(hostAddress, PunishType.BAN)) return;
            punishProvider.createPunishment(uniqueId.toString(), hostAddress, punishProvider.getReason(BYPASS_BAN_REASON), null);
        }
        Punishment punishment = punishProvider.getPunishment(uniqueId.toString(), PunishType.BAN);
        if (punishment == null) return;
        if (punishment.isPresent()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text(punishProvider.getBanMessage(uniqueId.toString()))));
            return;
        }
        this.punishProvider.deletePunishment(uniqueId.toString(), PunishType.BAN);
        this.punishProvider.sendTeamNotification(uniqueId.toString(), null, null);
        event.setResult(PreLoginEvent.PreLoginComponentResult.allowed());
    }

    private void checkProfileName(UUID uniqueId, String hostAddress, String username) {
        if(!UUIDProvider.getName(uniqueId).equals(username)) UUIDProvider.updateProfile(uniqueId, username);
        for (String insult : instance.getInsultProvider().getWords()) {
            Pattern pattern = Pattern.compile(instance.getRegexGenerator().get(insult));
            Matcher matcher = pattern.matcher(username);
            if(matcher.find()) {
                this.punishProvider.createPunishment(uniqueId.toString(), hostAddress, this.punishProvider.getReason(UNALLOWED_NAME_REASON), null);
                return;
            }
        }
    }

}
