package it.philipkrauss.proxysystem.commands.team;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.player.NetworkPlayer;
import it.philipkrauss.proxysystem.player.PlayerProperty;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class LogoutCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.team";

    private final ProxySystem instance;

    public LogoutCommand() {
        this.instance = ProxySystem.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(instance.getOnlyPlayers());
            return;
        }
        if(!player.hasPermission(PERMISSION)) {
            source.sendMessage(instance.getNoPermissions());
            return;
        }
        String[] args = invocation.arguments();
        if(args.length != 0) {
            player.sendMessage(instance.getUsage("/logout"));
            return;
        }
        UUID uniqueId = player.getUniqueId();
        NetworkPlayer networkPlayer = instance.getNetworkPlayerRegistry().getNetworkPlayer(uniqueId);
        if(!instance.getTeamManager().cantValidateLogin(uniqueId)) {
            networkPlayer.setProperty(PlayerProperty.TEAMSTATUS, false);
            player.sendMessage(Component.text(instance.getPrefix() + "§7Du hast dich §causgeloggt§8."));
            instance.getTeamManager().sendTeamMessage(Component.text(instance.getPrefix() + networkPlayer.getFormattedName() + " §7hat sich §causgeloggt§8."));
            return;
        }
        player.sendMessage(Component.text(instance.getPrefix() + "§7Du bist §cbereits §7ausgeloggt§8..."));
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
