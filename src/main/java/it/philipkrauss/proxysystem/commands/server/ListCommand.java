package it.philipkrauss.proxysystem.commands.server;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.api.service.ICloudService;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;

public class ListCommand implements SimpleCommand {

    private final ProxySystem instance;
    private final ICloudAPI cloudAPI;

    public ListCommand() {
        this.instance = ProxySystem.getInstance();
        this.cloudAPI = CloudAPI.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if(invocation.arguments().length != 0) {
            source.sendMessage(instance.getUsage("/list"));
            return;
        }
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lFireplayz §8]§8§m-------------"));
        source.sendMessage(Component.text(instance.getPrefix() + "§7Aktuell auf dem Netzwerk §8● §e" +
                cloudAPI.getCloudPlayerManager().getNetworkOnlinePlayerCount().getNow()));
        if(source instanceof Player player) {
            ICloudPlayer cloudPlayer = cloudAPI.getCloudPlayerManager().getCachedCloudPlayer(player.getUniqueId());
            if(cloudPlayer == null) return;
            ICloudService proxy = cloudPlayer.getConnectedProxy();
            if(proxy == null) return;
            source.sendMessage(Component.text(instance.getPrefix() + "§7Spieler auf deiner Proxy §8(§e" + proxy.getName() + "§8) ● §e" + proxy.getOnlineCount()));
            ICloudService service = cloudPlayer.getConnectedServer();
            if(service == null) return;
            source.sendMessage(Component.text(instance.getPrefix() + "§7Spieler auf §e" + service.getName() + " §8● §e" + service.getOnlineCount() + " §8/ §e" + service.getMaxPlayers()));
        }
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lFireplayz §8]§8§m-------------"));
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
