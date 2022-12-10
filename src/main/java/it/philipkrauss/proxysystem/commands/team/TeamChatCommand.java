package it.philipkrauss.proxysystem.commands.team;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.player.NetworkPlayer;
import it.philipkrauss.proxysystem.player.PlayerProperty;
import it.philipkrauss.proxysystem.player.registry.NetworkPlayerRegistry;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;

public class TeamChatCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.team";

    private final ProxySystem instance;

    public TeamChatCommand() {
        this.instance = ProxySystem.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if(!source.hasPermission(PERMISSION)) {
            source.sendMessage(instance.getNoPermissions());
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        String[] args = invocation.arguments();
        if(args.length == 0) {
            NetworkPlayerRegistry playerRegistry = instance.getNetworkPlayerRegistry();
            List<NetworkPlayer> teamPlayers = playerRegistry.getNetworkPlayers().stream().filter(networkPlayer -> networkPlayer.hasProperty(PlayerProperty.TEAMSTATUS) &&
                    Boolean.parseBoolean(networkPlayer.getProperty(PlayerProperty.TEAMSTATUS).getValueAsString())).toList();
            source.sendMessage(Component.text(instance.getTeamChatPrefix() + "§7Derzeit angemeldete Teammitglieder §8● §e" + teamPlayers.size()));
            teamPlayers.forEach(player -> source.sendMessage(Component.text(instance.getTeamChatPrefix() +
                    "§8» " + player.getFormattedName() + " §8(§7" + player.getCloudPlayer().getConnectedServerName() + "§8)")));
            source.sendMessage(Component.text(instance.getTeamChatPrefix() + "§7Verwende §8● §e/" + invocation.alias() + " <Nachricht>"));
            return;
        }
        NetworkPlayerRegistry playerRegistry = instance.getNetworkPlayerRegistry();
        List<NetworkPlayer> teamPlayers = playerRegistry.getNetworkPlayers().stream().filter(networkPlayer -> networkPlayer.hasProperty(PlayerProperty.TEAMSTATUS) &&
                Boolean.parseBoolean(networkPlayer.getProperty(PlayerProperty.TEAMSTATUS).getValueAsString())).toList();
        String message = String.join(" ", args);
        teamPlayers.forEach(player -> player.getCloudPlayer().sendMessage(Component.text(instance.getTeamChatPrefix() + player.getFormattedName() + " §8● §7" + message)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

}
