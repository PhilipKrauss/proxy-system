package it.philipkrauss.proxysystem.commands.connection;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;

public class FindCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.find";

    private final ProxySystem instance;
    private final ICloudAPI cloudAPI;

    public FindCommand() {
        this.instance = ProxySystem.getInstance();
        this.cloudAPI = CloudAPI.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission(PERMISSION)) {
            source.sendMessage(instance.getNoPermissions());
            return;
        }
        String[] args = invocation.arguments();
        if (args.length != 1) {
            source.sendMessage(instance.getUsage("/find <Spieler>"));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        ICommunicationPromise<ICloudPlayer> onlinePromise = cloudAPI.getCloudPlayerManager().getCloudPlayer(args[0]);
        onlinePromise.addListener(future -> {
            if (future.isSuccess()) {
                ICloudPlayer cloudPlayer = onlinePromise.get();
                if (!cloudPlayer.isOnline()) {
                    source.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Spieler ist derzeit §cnicht §7online§8..."));
                    return;
                }
                String serverName = cloudPlayer.getConnectedServerName();
                Component serviceComponent = Component.text("§e" + serverName)
                        .hoverEvent(HoverEvent.showText(Component.text("§7Verbinde dich mit §e" + serverName)))
                        .clickEvent(ClickEvent.runCommand("/connect " + serverName));
                source.sendMessage(Component.text(instance.getPrefix()).append(serviceComponent)
                        .append(Component.text(" §7ist auf §e" + serverName + " §8(§e" + cloudPlayer.getConnectedProxyName() + "§8)")));
            } else {
                source.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Spieler ist derzeit §cnicht §7online§8..."));
            }
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return ProxySystem.getInstance().getTabUtils().getPlayerNameCompletions(invocation, PERMISSION);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

}
