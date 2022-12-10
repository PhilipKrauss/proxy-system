package it.philipkrauss.proxysystem.manager;

import com.google.common.collect.Lists;
import it.philipkrauss.proxysystem.player.registry.NetworkPlayerRegistry;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.player.NetworkPlayer;
import it.philipkrauss.proxysystem.player.PlayerProperty;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;

public class TeamManager {

    public static TeamManager create() {
        return new TeamManager();
    }

    public static final String PERMISSION = "proxy.team";

    private final NetworkPlayerRegistry networkPlayerRegistry;

    private TeamManager() {
        this.networkPlayerRegistry = ProxySystem.getInstance().getNetworkPlayerRegistry();
    }

    public void updateStatus(NetworkPlayer networkPlayer) {
        if (!networkPlayer.hasPermission(PERMISSION)) return;
        if (!networkPlayer.hasProperty(PlayerProperty.TEAMSTATUS))
            networkPlayer.setProperty(PlayerProperty.TEAMSTATUS, false);
    }

    private boolean canValidateLogin(UUID uniqueId) {
        NetworkPlayer networkPlayer = networkPlayerRegistry.getNetworkPlayer(uniqueId);
        if (networkPlayer == null) return false;
        if (!networkPlayer.hasPermission(PERMISSION)) return false;
        this.updateStatus(networkPlayer);
        return Boolean.parseBoolean(networkPlayer.getProperty(PlayerProperty.TEAMSTATUS).getValueAsString());
    }

    public boolean cantValidateLogin(UUID uniqueId) {
        return !canValidateLogin(uniqueId);
    }

    public boolean isLoggedIn(NetworkPlayer networkPlayer) {
        return !this.cantValidateLogin(networkPlayer.getUniqueId());
    }

    public void sendTeamMessage(Component message) {
        List<NetworkPlayer> networkPlayers = Lists.newArrayList();
        ProxySystem.getInstance().getProxyServer().getConsoleCommandSource().sendMessage(message);
        this.networkPlayerRegistry.getNetworkPlayers().stream().filter(networkPlayer ->
                networkPlayer.hasPermission(PERMISSION)
                && networkPlayer.hasProperty(PlayerProperty.TEAMSTATUS)
                && Boolean.parseBoolean(networkPlayer.getProperty(PlayerProperty.TEAMSTATUS).getValueAsString())).forEach(networkPlayers::add);
        networkPlayers.forEach(networkPlayer -> networkPlayer.getCloudPlayer().sendMessage(message));
    }

}
