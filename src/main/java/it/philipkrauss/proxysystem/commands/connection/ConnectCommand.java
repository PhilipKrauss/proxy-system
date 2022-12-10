package it.philipkrauss.proxysystem.commands.connection;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.api.player.connection.ConnectionResponse;
import eu.thesimplecloud.api.service.ICloudService;
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;

import java.util.List;

public class ConnectCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.connect";

    private final ProxySystem instance;

    public ConnectCommand() {
        this.instance = ProxySystem.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(instance.getOnlyPlayers());
            return;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(instance.getNoPermissions());
            return;
        }
        String[] args = invocation.arguments();
        if (args.length != 1) {
            player.sendMessage(instance.getUsage("/connect <Server>"));
            return;
        }
        if (ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        ICloudService service = CloudAPI.getInstance().getCloudServiceManager().getCloudServiceByName(args[0]);
        if(service == null) {
            player.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Service wurde §cnicht §7gefunden§8..."));
            return;
        }
        ICloudPlayer cloudPlayer = CloudAPI.getInstance().getCloudPlayerManager().getCachedCloudPlayer(player.getUniqueId());
        if(cloudPlayer == null) return;
        if(cloudPlayer.getConnectedServerName() == null) {
            player.sendMessage(Component.text(instance.getPrefix() + "§7Ein unerwarteter Fehler ist aufgetreten§8..."));
            return;
        }
        if (cloudPlayer.getConnectedServerName().equalsIgnoreCase(service.getName())) {
            player.sendMessage(Component.text(instance.getPrefix() + "§7Du bist §cbereits §7auf diesem Service§8..."));
            return;
        }
        player.sendMessage(Component.text(instance.getPrefix() + "§7Verbinde dich mit §e" + service.getName() + "§8..."));
        if(service.isProxy()) {
            player.sendMessage(Component.text(instance.getPrefix() + "§7Du kannst dich §cnicht §7mit einer Proxy verbinden§8..."));
            return;
        }
        ICommunicationPromise<ConnectionResponse> connectionResponsePromise = cloudPlayer.connect(service);
        if(!connectionResponsePromise.isSuccess()) {
            player.sendMessage(Component.text(instance.getPrefix() + "§7Die Verbindung konnte §cnicht §7aufgebaut werden§8..."));
            return;
        }
        player.sendMessage(Component.text(instance.getPrefix() + "§7Du bist nun mit §e" + service.getName() + " §7verbunden§8."));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return ProxySystem.getInstance().getTabUtils().getServiceNameCompletions(invocation, PERMISSION);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

}
