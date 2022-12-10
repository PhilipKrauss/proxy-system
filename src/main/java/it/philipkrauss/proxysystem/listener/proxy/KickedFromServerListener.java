package it.philipkrauss.proxysystem.listener.proxy;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import it.philipkrauss.proxysystem.provider.punishment.objects.PunishReason;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Optional;

public class KickedFromServerListener {

    private final ProxySystem instance;
    private final PlainTextComponentSerializer serializer;

    public KickedFromServerListener() {
        this.instance = ProxySystem.getInstance();
        this.serializer = PlainTextComponentSerializer.builder().flattener(ComponentFlattener.textOnly()).build();
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        Optional<Component> kickReason = event.getServerKickReason();
        if(kickReason.isEmpty()) return;
        Component reasonComponent = kickReason.get();
        String reasonString = serializer.serialize(reasonComponent);
        if(!reasonString.startsWith("autoban-")) return;
        int id = Integer.parseInt(reasonString.replace("autoban-", ""));
        PunishReason punishReason = instance.getPunishProvider().getReason(id + 300);
        if(punishReason == null) return;
        instance.getPunishProvider().createPunishment(event.getPlayer().getUniqueId().toString(), event.getPlayer().getRemoteAddress().getHostName(), punishReason, null);
    }

}
