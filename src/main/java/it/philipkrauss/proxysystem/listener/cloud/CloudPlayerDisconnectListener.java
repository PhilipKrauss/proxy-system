package it.philipkrauss.proxysystem.listener.cloud;

import eu.thesimplecloud.api.event.player.CloudPlayerDisconnectEvent;
import eu.thesimplecloud.api.eventapi.CloudEventHandler;
import eu.thesimplecloud.api.eventapi.IListener;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.player.NetworkPlayer;
import it.philipkrauss.proxysystem.provider.friend.FriendProvider;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class CloudPlayerDisconnectListener implements IListener {

    private final ProxySystem instance;

    public CloudPlayerDisconnectListener() {
        this.instance = ProxySystem.getInstance();
    }

    @CloudEventHandler
    public void onPlayerDisconnect(CloudPlayerDisconnectEvent event) {
        UUID uniqueId = event.getPlayerUniqueId();
        NetworkPlayer networkPlayer = instance.getNetworkPlayerRegistry().getNetworkPlayer(uniqueId);
        instance.getPunishProvider().resetCache(uniqueId.toString());
        if (instance.getTeamManager().isLoggedIn(networkPlayer)) {
            instance.getTeamManager().sendTeamMessage(Component.text(instance.getPrefix() + networkPlayer.getFormattedName() + " §7hat sich §causgeloggt§8."));
        }
        FriendProvider friendProvider = instance.getFriendProvider();
        friendProvider.uncacheFriendSettings(uniqueId.toString());
        friendProvider.uncacheReply(uniqueId.toString());
        friendProvider.getOnlineFriends(uniqueId.toString()).stream()
                .filter(friend -> friendProvider.getFriendSettings(friend.getUniqueId().toString()).canReceiveNotifications())
                .forEach(friend -> friend.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + networkPlayer.getCloudPlayer().getName()
                        + " §7ist jetzt §coffline§8.")));
        instance.getNetworkPlayerRegistry().unregisterNetworkPlayer(networkPlayer);
    }

}
