package it.philipkrauss.proxysystem.commands.cloud;

import com.google.common.collect.Lists;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.api.player.SimpleCloudPlayer;
import eu.thesimplecloud.api.service.ServiceType;
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MaintenanceCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.maintenance";
    private static final SimpleDateFormat HOUR_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final ProxySystem instance;
    private final ICloudAPI cloudAPI;

    public MaintenanceCommand() {
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
        if (args.length != 1 && args.length != 2) {
            source.sendMessage(instance.getUsage(instance.getCloudPrefix(), "/maintenance <Now/Datum/Off>"));
            return;
        }
        if (args.length == 1) {
            switch (args[0].toUpperCase()) {
                case "NOW" -> {
                    cloudAPI.getCloudServiceGroupManager().getAllCachedObjects().forEach(serviceGroup -> {
                        if (serviceGroup.getServiceType() == ServiceType.PROXY) {
                            serviceGroup.setMaintenance(true);
                            serviceGroup.update();
                        }
                    });
                    source.sendMessage(Component.text(instance.getCloudPrefix() + "§7Die §eMaintenance §7wurde erfolgreich §aaktiviert §8/ §a✔"));
                    String name = (source instanceof Player player ? instance.getNetworkPlayerRegistry().getNetworkPlayer(player.getUniqueId()).getColoredName() :
                            "Konsole");
                    ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                            instance.getCloudPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
                    ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                            instance.getCloudPrefix() + "Die §eMaintenance §7wurde §aaktiviert §8/ §a✔"));
                    ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                            instance.getCloudPrefix() + "Aktualisiert von §8● §e" + name));
                    ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                            instance.getCloudPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
                    ICommunicationPromise<List<SimpleCloudPlayer>> promise = cloudAPI.getCloudPlayerManager().getAllOnlinePlayers();
                    promise.addListener(onlineFuture -> {
                        if (onlineFuture.isSuccess()) {
                            List<SimpleCloudPlayer> simpleCloudPlayers = promise.get();
                            simpleCloudPlayers.forEach(simpleCloudPlayer -> {
                                ICommunicationPromise<ICloudPlayer> cloudPlayerPromise = simpleCloudPlayer.getCloudPlayer();
                                cloudPlayerPromise.addListener(cloudPlayerFuture -> {
                                    if (cloudPlayerFuture.isSuccess()) {
                                        ICloudPlayer cloudPlayer = cloudPlayerPromise.get();
                                        if (!cloudPlayer.hasPermissionSync("cloud.maintenance.join"))
                                            cloudPlayer.kick("""
                                                    §8▎ §e§lFireplayz.net §8▰§7▰ §7Du wurdest vom §eNetzwerk §7gekickt§8.
                                                    §8
                                                    §7Grund §8● §eDas Netzwerk befindet sich in §cWartungsarbeiten""");
                                    }
                                });
                            });
                        }
                    });
                    return;
                }
                case "OFF" -> {
                    cloudAPI.getCloudServiceGroupManager().getAllCachedObjects().forEach(serviceGroup -> {
                        if (serviceGroup.getServiceType() == ServiceType.PROXY) {
                            serviceGroup.setMaintenance(false);
                            serviceGroup.update();
                        }
                    });
                    source.sendMessage(Component.text(instance.getCloudPrefix() + "§7Die §eMaintenance §7wurde erfolgreich §cdeaktiviert §8/ §c✗"));
                    String name = (source instanceof Player player ? instance.getNetworkPlayerRegistry().getNetworkPlayer(player.getUniqueId()).getColoredName() :
                            "Konsole");
                    ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                            instance.getCloudPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
                    ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                            instance.getCloudPrefix() + "Die §eMaintenance §7wurde §cdeaktiviert §8/ §c✗"));
                    ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                            instance.getCloudPrefix() + "Aktualisiert von §8● §e" + name));
                    ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                            instance.getCloudPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
                    return;
                }
                default -> {
                    try {
                        int seconds = Integer.parseInt(args[0]);
                        String name = (source instanceof Player player ? instance.getNetworkPlayerRegistry().getNetworkPlayer(player.getUniqueId()).getColoredName() :
                                "Konsole");
                        ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                instance.getPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
                        ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                instance.getPrefix() + "Die §eScheduled-Maintenance §7wurde §agestartet"));
                        ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                instance.getPrefix() + "Zeit §8● §e" + seconds + " §7Sekunden"));
                        ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                instance.getPrefix() + "Gestartet von §8● §e" + name));
                        ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                instance.getPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
                        instance.getMaintenanceScheduler().schedule(seconds);
                        return;
                    } catch (Exception ignored) {
                        try {
                            String today = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
                            Date parse = DATE_FORMAT.parse(today + " " + args[0]);
                            long difference = (parse.getTime() - System.currentTimeMillis());
                            int seconds = (int) (difference / 1000);
                            instance.getMaintenanceScheduler().schedule(seconds);
                            String name = (source instanceof Player player ? instance.getNetworkPlayerRegistry().getNetworkPlayer(player.getUniqueId()).getColoredName() :
                                    "Konsole");
                            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                    instance.getCloudPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
                            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                    instance.getCloudPrefix() + "Die §eScheduled-Maintenance §7wurde §agestartet"));
                            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                    instance.getCloudPrefix() + "Zeit §8● §e" + args[0]));
                            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                    instance.getCloudPrefix() + "Gestartet von §8● §e" + name));
                            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                                    instance.getCloudPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
                            return;
                        } catch (Exception ignore) {
                            source.sendMessage(Component.text(instance.getCloudPrefix() + "§7Der Wert §8'§e" + args[0] + "§8' §7ist §cungültig§8..."));
                            return;
                        }
                    }
                }
            }
        }
        try {
            String dateFormat = args[0] + " " + args[1];
            Date parse = DATE_FORMAT.parse(dateFormat);
            long difference = (parse.getTime() - System.currentTimeMillis());
            int converted = (int) (difference / 1000);
            instance.getMaintenanceScheduler().schedule(converted);
            String name = (source instanceof Player player ? instance.getNetworkPlayerRegistry().getNetworkPlayer(player.getUniqueId()).getColoredName() :
                    "Konsole");
            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                    instance.getCloudPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                    instance.getCloudPrefix() + "Die §eScheduled-Maintenance §7wurde §agestartet§8."));
            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                    instance.getCloudPrefix() + "Zeit §8● §e" + dateFormat));
            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                    instance.getCloudPrefix() + "Gestartet von §8● §e" + name));
            ProxySystem.getInstance().getTeamManager().sendTeamMessage(Component.text(
                    instance.getCloudPrefix() + "§8§m------------§r§8[ §a§lMaintenance §8]§8§m----------"));
        } catch (Exception ignore) {
            source.sendMessage(Component.text(instance.getCloudPrefix() + "§7Der Wert §8'§e" + args[0] + "§8' §7ist §cungültig§8..."));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission(PERMISSION)) return Collections.emptyList();
        if (invocation.arguments().length == 0) {
            String hourFormat = HOUR_FORMAT.format(new Date());
            String dateFormat = DATE_FORMAT.format(new Date());
            return Lists.newArrayList("Now", hourFormat, dateFormat, "Off");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

}
