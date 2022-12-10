package it.philipkrauss.proxysystem.listener.cloud;

import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.event.player.CloudPlayerRegisteredEvent;
import eu.thesimplecloud.api.eventapi.CloudEventHandler;
import eu.thesimplecloud.api.eventapi.IListener;
import it.philipkrauss.proxysystem.manager.TeamManager;
import it.philipkrauss.proxysystem.player.NetworkPlayer;
import it.philipkrauss.proxysystem.player.PlayerProperty;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.friend.FriendProvider;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;

public class CloudPlayerRegisteredListener implements IListener {

    private final ProxySystem instance;
    private final FriendProvider friendProvider;

    public CloudPlayerRegisteredListener() {
        this.instance = ProxySystem.getInstance();
        this.friendProvider = instance.getFriendProvider();
    }

    @CloudEventHandler
    public void onPlayerConnect(CloudPlayerRegisteredEvent event) {
        UUID uniqueId = event.getCloudPlayer().getUniqueId();
        instance.getUUIDProvider().cachePlayer(uniqueId, event.getCloudPlayer().getName());
        NetworkPlayer networkPlayer = NetworkPlayer.create(uniqueId);
        instance.getNetworkPlayerRegistry().registerNetworkPlayer(networkPlayer);
        // save information
        networkPlayer.setProperty(PlayerProperty.IP, networkPlayer.getCloudPlayer().getPlayerConnection().getAddress().getHostname());
        if(networkPlayer.hasPermission(TeamManager.PERMISSION)) {
            if(instance.getTeamManager().isLoggedIn(networkPlayer)) {
                networkPlayer.getCloudPlayer().sendMessage(Component.text(instance.getPrefix() + "Dein aktueller Status §8● §aEingeloggt §8(§a✔§8)§8"));
                instance.getTeamManager().sendTeamMessage(Component.text(instance.getPrefix() + networkPlayer.getFormattedName() + " §7hat sich §aeingeloggt§8."));
            } else {
                networkPlayer.getCloudPlayer().sendMessage(Component.text(instance.getPrefix() + "Dein aktueller Status §8● §cAusgeloggt §8(§c✗§8)§8"));
            }
        }
        List<String> requests = friendProvider.getFriendRequests(uniqueId.toString());
        if (requests.size() != 0) event.getCloudPlayer().sendMessage(Component.text(instance.getFriendPrefix() + "§7Du hast §e" +
                (requests.size() == 1 ? "eine" : requests.size()) + " §7offene Anfrage" + (requests.size() == 1 ? "" : "n") + "§8."));
        List<Player> friends = friendProvider.getOnlineFriends(uniqueId.toString());
        friends.stream().filter(friend -> friendProvider.getFriendSettings(friend.getUniqueId().toString()).canReceiveNotifications())
                .forEach(friend -> friend.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + event.getCloudPlayer().getName() + " §7ist jetzt §aonline§8.")));
        if (friendProvider.getFriendSettings(uniqueId.toString()).canReceiveNotifications()) {
            event.getCloudPlayer().sendMessage(Component.text(instance.getFriendPrefix() + "§7Derzeit " + (friends.size() == 0 ? "sind §ckeine" :
                    (friends.size() == 1 ? "ist §eeiner" : "sind §e" + friends.size())) + " §7deiner Freunde online§8."));
        }
    }

}
