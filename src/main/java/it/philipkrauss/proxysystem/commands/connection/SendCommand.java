package it.philipkrauss.proxysystem.commands.connection;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.api.player.SimpleCloudPlayer;
import eu.thesimplecloud.api.player.connection.ConnectionResponse;
import eu.thesimplecloud.api.service.ICloudService;
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SendCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.send";

    private final ProxySystem instance;
    private final ICloudAPI cloudAPI;

    public SendCommand() {
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
        if (args.length != 2) {
            source.sendMessage(instance.getUsage("/send <Spieler/All> <Service>"));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        ICloudService cloudService = cloudAPI.getCloudServiceManager().getCloudServiceByName(args[1]);
        if (cloudService == null) {
            return;
        }
        if (args[0].equalsIgnoreCase("ALL") || args[0].equalsIgnoreCase("*")) {
            ICommunicationPromise<List<SimpleCloudPlayer>> promise = cloudAPI.getCloudPlayerManager().getAllOnlinePlayers();
            AtomicInteger success = new AtomicInteger();
            promise.addListener(onlineFuture -> {
                if (onlineFuture.isSuccess()) {
                    List<SimpleCloudPlayer> simpleCloudPlayers = promise.get();
                    simpleCloudPlayers.forEach(simpleCloudPlayer -> {
                        ICommunicationPromise<ICloudPlayer> cloudPlayerPromise = simpleCloudPlayer.getCloudPlayer();
                        cloudPlayerPromise.addListener(cloudPlayerFuture -> {
                            if (cloudPlayerFuture.isSuccess()) {
                                ICloudPlayer cloudPlayer = cloudPlayerPromise.get();
                                success.set(success.get() + this.sendPlayer(source, cloudPlayer, cloudService, false));
                            } else {
                                source.sendMessage(Component.text(instance.getPrefix() + "§e" + simpleCloudPlayer.getName() + " §7konnte §cnicht §7gesendet werden§8..."));
                            }
                        });
                    });
                } else {
                    List<ICloudPlayer> cloudPlayers = cloudAPI.getCloudPlayerManager().getAllCachedObjects();
                    cloudPlayers.forEach(cloudPlayer -> success.set(success.get() + this.sendPlayer(source, cloudPlayer, cloudService, false)));
                }
            });
            source.sendMessage(Component.text(instance.getPrefix() + "§7Es wurden §e" + success.get() + " §7Spieler gesendet§8."));
            return;
        }
        UUID uniqueId = UUIDProvider.getUniqueId(args[0]);
        if (uniqueId == null) {
            source.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
            return;
        }
        ICommunicationPromise<ICloudPlayer> cloudPlayerPromise = cloudAPI.getCloudPlayerManager().getCloudPlayer(uniqueId);
        cloudPlayerPromise.addListener(cloudPlayerFuture -> {
            if (cloudPlayerFuture.isSuccess()) {
                ICloudPlayer cloudPlayer = cloudPlayerPromise.get();
                if (!cloudPlayer.isOnline()) {
                    source.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Spieler ist derzeit §cnicht §7online§8..."));
                    return;
                }
                this.sendPlayer(source, cloudPlayer, cloudService, true);
            } else {
                source.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Spieler konnte §cnicht §7geladen werden§8..."));
            }
        });
    }

    private int sendPlayer(CommandSource source, ICloudPlayer cloudPlayer, ICloudService cloudService, boolean result) {
        AtomicInteger success = new AtomicInteger();
        if (result)
            source.sendMessage(Component.text(instance.getPrefix() + "§e" + cloudPlayer.getName() + " §7wird auf §e" + cloudService.getName() + " §7gesendet§8..."));
        cloudPlayer.sendMessage(Component.text(instance.getPrefix() + "§7Du wirst auf §e" + cloudService.getName() + " §7gesendet§8..."));
        ICommunicationPromise<ConnectionResponse> communicationPromise = cloudPlayer.connect(cloudService);
        communicationPromise.addListener(sendFuture -> {
            if (sendFuture.isSuccess()) {
                success.getAndIncrement();
                if (result)
                    source.sendMessage(Component.text(instance.getPrefix() + "§e" + cloudPlayer.getName() + " §7ist nun auf §e" + cloudService.getName() + "§8."));
                cloudPlayer.sendMessage(Component.text(instance.getPrefix() + "§7Du bist nun auf §e" + cloudService.getName() + "§8."));
            } else {
                if (result)
                    source.sendMessage(Component.text(instance.getPrefix() + "§e" + cloudPlayer.getName() + " §7konnte §cnicht §7gesendet werden§8..."));
                cloudPlayer.sendMessage(Component.text(instance.getPrefix() + "§7Du konntest §cnicht §7gesendet werden§8..."));
            }
        });
        return success.get();
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission(PERMISSION)) return Collections.emptyList();
        if (invocation.arguments().length == 0) {
            List<ICloudPlayer> cloudPlayers = cloudAPI.getCloudPlayerManager().getAllCachedObjects();
            return cloudPlayers.stream().map(ICloudPlayer::getName).toList();
        }
        if (invocation.arguments().length == 1) {
            return ProxySystem.getInstance().getTabUtils().getServiceNameCompletions();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

}
