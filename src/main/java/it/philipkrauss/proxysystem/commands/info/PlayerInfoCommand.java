package it.philipkrauss.proxysystem.commands.info;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.api.player.IOfflineCloudPlayer;
import eu.thesimplecloud.api.property.IProperty;
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise;
import eu.thesimplecloud.module.permission.PermissionPool;
import eu.thesimplecloud.module.permission.player.IPermissionPlayer;
import it.philipkrauss.proxysystem.player.PlayerProperty;
import it.philipkrauss.proxysystem.provider.punishment.PunishType;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.punishment.PunishProvider;
import it.philipkrauss.proxysystem.provider.punishment.objects.PunishReason;
import it.philipkrauss.proxysystem.provider.punishment.objects.Punishment;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PlayerInfoCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.playerinfo";

    private final ProxySystem instance;
    private final ICloudAPI cloudAPI;

    public PlayerInfoCommand() {
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
            source.sendMessage(instance.getUsage("/playerinfo <Spieler>"));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        source.sendMessage(Component.text(instance.getPrefix() + "§7Lade Informationen über §e" + args[0] + "§8..."));
        UUID uniqueId = UUIDProvider.getUniqueId(args[0]);
        if (uniqueId == null) {
            source.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
            return;
        }
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lPlayerInfo §8]§8§m------------"));
        source.sendMessage(Component.text(instance.getPrefix() + "§7Name §8● §e" + args[0]));
        Component uniqueIdComponent = Component.text(instance.getPrefix() + "§7UUID §8● §e").append(Component.text("§e" + uniqueId)
                .hoverEvent(HoverEvent.showText(Component.text("§7Kopieren")))
                .clickEvent(ClickEvent.copyToClipboard(uniqueId.toString())));
        source.sendMessage(uniqueIdComponent);
        ICommunicationPromise<IPermissionPlayer> permissionPlayerPromise = PermissionPool.getInstance().getPermissionPlayerManager().getPermissionPlayer(uniqueId);
        permissionPlayerPromise.addListener(permissionPlayerFuture -> {
            if (permissionPlayerFuture.isSuccess()) {
                IPermissionPlayer permissionPlayer = permissionPlayerPromise.get();
                String name = permissionPlayer.getHighestPermissionGroup().getName();
                source.sendMessage(Component.text(instance.getPrefix() + "§7Rang §8● " + this.instance.getPermissionProvider().getColor(name) + name));
                ICommunicationPromise<ICloudPlayer> onlinePromise = cloudAPI.getCloudPlayerManager().getCloudPlayer(uniqueId);
                onlinePromise.addListener(future -> {
                    if (future.isSuccess()) {
                        ICloudPlayer cloudPlayer = onlinePromise.get();
                        if (cloudPlayer != null) {
                            boolean isOnline = cloudPlayer.isOnline();
                            source.sendMessage(Component.text(instance.getPrefix() + "§7Verbindung §8● " + (isOnline ? "§aOnline §8(§a✔§8)" : "§cOffline §8(§c✗§8)")));
                            if (isOnline) {
                                source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Aktuelle Proxy §8● §e" + cloudPlayer.getConnectedProxyName()));
                                source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Aktueller Service §8● §e" + cloudPlayer.getConnectedServerName()));
                            }
                            IProperty<Object> property = cloudPlayer.getProperty(PlayerProperty.IP.name());
                            source.sendMessage(Component.text(instance.getPrefix() + "§7IP-Adresse §8● §e" +
                                    (property == null ? "§eUnbekannt" : property.getValueAsString().replace("\"", ""))));
                            source.sendMessage(Component.text(instance.getPrefix() + "§7Onlinezeit §8● §e" +
                                    instance.getTimeUtils().convertOnlineTime(cloudPlayer.getOnlineTime())));
                            source.sendMessage(Component.text(instance.getPrefix() + "§7Erster Login §8● §e" +
                                    instance.getTimeUtils().formatDate(cloudPlayer.getFirstLogin())));
                            source.sendMessage(Component.text(instance.getPrefix() + "§7Letzter Login §8● §e" +
                                    instance.getTimeUtils().formatDate(cloudPlayer.getLastLogin())));
                            this.sendPunishmentInfo(source, uniqueId.toString());
                            source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lPlayerInfo §8]§8§m------------"));
                        }
                    } else {
                        ICommunicationPromise<IOfflineCloudPlayer> offlinePromise = cloudAPI.getCloudPlayerManager().getOfflineCloudPlayer(uniqueId);
                        offlinePromise.addListener(offlineFuture -> {
                            if (offlineFuture.isSuccess()) {
                                IOfflineCloudPlayer offlineCloudPlayer = offlinePromise.get();
                                IProperty<Object> property = offlineCloudPlayer.getProperty(PlayerProperty.IP.name());
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Verbindung §8● §cOffline §8(§c✗§8)"));
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Letzte IP-Adresse §8● §e" +
                                        (property == null ? "§eUnbekannt" : property.getValueAsString().replace("\"", ""))));
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Onlinezeit §8● §e" +
                                        instance.getTimeUtils().convertOnlineTime(offlineCloudPlayer.getOnlineTime())));
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Erster Login §8● §e" +
                                        instance.getTimeUtils().formatDate(offlineCloudPlayer.getFirstLogin())));
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Letzter Login §8● §e" +
                                        instance.getTimeUtils().formatDate(offlineCloudPlayer.getLastLogin())));
                                this.sendPunishmentInfo(source, uniqueId.toString());
                                source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lPlayerInfo §8]§8§m------------"));
                            } else {
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Verbindung §8● §cOffline §8(§c✗§8)"));
                                this.sendPunishmentInfo(source, uniqueId.toString());
                                source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lPlayerInfo §8]§8§m------------"));
                            }
                        });
                    }
                });
            } else {
                String name = PermissionPool.getInstance().getPermissionGroupManager().getDefaultPermissionGroup().getName();
                source.sendMessage(Component.text(instance.getPrefix() + "§7Rang §8● " + this.instance.getPermissionProvider().getColor(name) + name));
                ICommunicationPromise<ICloudPlayer> onlinePromise = cloudAPI.getCloudPlayerManager().getCloudPlayer(uniqueId);
                onlinePromise.addListener(future -> {
                    if (future.isSuccess()) {
                        ICloudPlayer cloudPlayer = onlinePromise.get();
                        if (cloudPlayer != null) {
                            boolean isOnline = cloudPlayer.isOnline();
                            source.sendMessage(Component.text(instance.getPrefix() + "§7Verbindung §8● " + (isOnline ? "§aOnline §8(§a✔§8)" : "§cOffline §8(§c✗§8)")));
                            if (isOnline) {
                                source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Aktuelle Proxy §8● §e" + cloudPlayer.getConnectedProxyName()));
                                source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Aktueller Service §8● §e" + cloudPlayer.getConnectedServerName()));
                            }
                            IProperty<Object> property = cloudPlayer.getProperty(PlayerProperty.IP.name());
                            source.sendMessage(Component.text(instance.getPrefix() + "§7IP-Adresse §8● §e" +
                                    (property == null ? "§eUnbekannt" : property.getValueAsString().replace("\"", ""))));
                            source.sendMessage(Component.text(instance.getPrefix() + "§7Onlinezeit §8● §e" +
                                    instance.getTimeUtils().convertOnlineTime(cloudPlayer.getOnlineTime())));
                            source.sendMessage(Component.text(instance.getPrefix() + "§7Erster Login §8● §e" +
                                    instance.getTimeUtils().formatDate(cloudPlayer.getFirstLogin())));
                            source.sendMessage(Component.text(instance.getPrefix() + "§7Letzter Login §8● §e" +
                                    instance.getTimeUtils().formatDate(cloudPlayer.getLastLogin())));
                            this.sendPunishmentInfo(source, uniqueId.toString());
                            source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lPlayerInfo §8]§8§m------------"));
                        }
                    } else {
                        ICommunicationPromise<IOfflineCloudPlayer> offlinePromise = cloudAPI.getCloudPlayerManager().getOfflineCloudPlayer(uniqueId);
                        offlinePromise.addListener(offlineFuture -> {
                            if (offlineFuture.isSuccess()) {
                                IOfflineCloudPlayer offlineCloudPlayer = offlinePromise.get();
                                IProperty<Object> property = offlineCloudPlayer.getProperty(PlayerProperty.IP.name());
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Verbindung §8● §cOffline §8(§c✗§8)"));
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Letzte IP-Adresse §8● §e" +
                                        (property == null ? "§eUnbekannt" : property.getValueAsString().replace("\"", ""))));
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Onlinezeit §8● §e" +
                                        instance.getTimeUtils().convertOnlineTime(offlineCloudPlayer.getOnlineTime())));
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Erster Login §8● §e" +
                                        instance.getTimeUtils().formatDate(offlineCloudPlayer.getFirstLogin())));
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Letzter Login §8● §e" +
                                        instance.getTimeUtils().formatDate(offlineCloudPlayer.getLastLogin())));
                                this.sendPunishmentInfo(source, uniqueId.toString());
                                source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lPlayerInfo §8]§8§m------------"));
                            } else {
                                source.sendMessage(Component.text(instance.getPrefix() + "§7Verbindung §8● §cOffline §8(§c✗§8)"));
                                this.sendPunishmentInfo(source, uniqueId.toString());
                                source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lPlayerInfo §8]§8§m------------"));
                            }
                        });
                    }
                });
            }
        });
    }

    private void sendPunishmentInfo(CommandSource source, String target) {
        PunishProvider punishManager = ProxySystem.getInstance().getPunishProvider();
        boolean banned = punishManager.isPunished(target, PunishType.BAN);
        boolean muted = punishManager.isPunished(target, PunishType.MUTE);
        if (!banned && !muted) {
            source.sendMessage(Component.text(instance.getPrefix() + "§7Aktuelle Strafen §8● §eKeine aktuellen Strafen"));
            return;
        }
        if (banned) {
            Punishment punishment = punishManager.getPunishment(target, PunishType.BAN);
            if (punishment == null) {
                source.sendMessage(Component.text(instance.getPrefix() + "§7Aktuelle Strafen §8● §eUnbekannt"));
                return;
            }
            PunishReason reason = punishment.getReason();
            source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Gebannt §8● §aJa §8(§a✔§8)"));
            source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Grund §8● §e" + reason.getReason()));
            source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Läuft ab §8● §e" + punishment.getEndDate()));
        }
        if (muted) {
            Punishment punishment = punishManager.getPunishment(target, PunishType.MUTE);
            if (punishment == null) {
                source.sendMessage(Component.text(instance.getPrefix() + "§7Aktuelle Strafen §8● §eUnbekannt"));
                return;
            }
            PunishReason reason = punishment.getReason();
            source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Gemuted §8● §aJa §8(§a✔§8)"));
            source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Grund §8● §e" + reason.getReason()));
            source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Läuft ab §8● §e" + punishment.getEndDate()));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission(PERMISSION)) return Collections.emptyList();
        if (invocation.arguments().length == 0) {
            return ProxySystem.getInstance().getTabUtils().getPlayerNameCompletions(invocation, PERMISSION);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

}
