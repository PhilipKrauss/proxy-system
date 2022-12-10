package it.philipkrauss.proxysystem.commands.punishment;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.api.player.IOfflineCloudPlayer;
import eu.thesimplecloud.api.property.IProperty;
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise;
import eu.thesimplecloud.module.permission.player.IPermissionPlayer;
import eu.thesimplecloud.module.permission.player.OfflinePlayerExtensionKt;
import it.philipkrauss.proxysystem.player.PlayerProperty;
import it.philipkrauss.proxysystem.provider.punishment.PunishProvider;
import it.philipkrauss.proxysystem.provider.punishment.objects.PunishReason;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BanCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.ban";
    private static final String BYPASS_PERMISSION = "proxy.command.ban.bypass";
    private static final String ADMIN_PERMISSION = "proxy.command.ban.admin";

    private final ProxySystem instance;
    private final ICloudAPI cloudAPI;
    private final PunishProvider punishProvider;

    public BanCommand() {
        this.instance = ProxySystem.getInstance();
        this.cloudAPI = CloudAPI.getInstance();
        this.punishProvider = instance.getPunishProvider();
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
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Verwende §8● §e/ban <Spieler> <Grund>"));
            List<PunishReason> reasons = punishProvider.getReasons().stream().filter(punishReason -> punishReason.getId() > 100 && punishReason.getId() <= 200).toList();
            if (reasons.size() == 0) {
                source.sendMessage(Component.text(instance.getPunishPrefix() + "§8(§c✗§8) §c§lKeine Gründe verfügbar..."));
                return;
            }
            reasons.forEach(reason -> source.sendMessage(Component.text(
                    instance.getPunishPrefix() + "§8(§e" + (reason.getId() - 100) + "§8) §7" + reason.getReason() + " §8● §e" + reason.getDurationText())));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        UUID uniqueId = UUIDProvider.getUniqueId(args[0]);
        if (uniqueId == null) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
            return;
        }
        int reasonId = Integer.parseInt(args[1]);
        if (reasonId < 0 || reasonId > 100) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Dieser Grund wurde §cnicht §7gefunden§8..."));
            return;
        }
        PunishReason punishReason = punishProvider.getReason(reasonId + 100);
        if (punishReason == null) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Dieser Grund wurde §cnicht §7gefunden§8..."));
            return;
        }
        if (source instanceof Player player && player.getUniqueId().equals(uniqueId)) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Du kannst dich §cnicht §7selbst bestrafen§8..."));
            return;
        }
        if (!canPunish(source, uniqueId)) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Du kannst diesen Spieler §cnicht §7bestrafen§8..."));
            return;
        }
        if (this.punishProvider.isPunished(uniqueId.toString(), punishReason.getType())) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Dieser Spieler wurde §cbereits §7bestraft§8..."));
            return;
        }
        int punishment = this.punishProvider.createPunishment(uniqueId.toString(), getAddress(uniqueId), punishReason, source);
        if (punishment != 1) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Ein unbekannter §cFehler §7ist aufgetreten§8..."));
            return;
        }
        source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Du hast §e" + args[0] + " §7erfolgreich bestraft§8."));
        this.punishProvider.executePunishment(uniqueId.toString(), punishReason, source);
    }

    private String getAddress(UUID uniqueId) {
        Optional<Player> playerOptional = instance.getProxyServer().getPlayer(uniqueId);
        if (playerOptional.isPresent()) return playerOptional.get().getRemoteAddress().getAddress().getHostAddress();
        ICloudPlayer cloudPlayer = cloudAPI.getCloudPlayerManager().getCachedCloudPlayer(uniqueId);
        if (cloudPlayer != null) return cloudPlayer.getPlayerConnection().getAddress().getHostname();
        ICommunicationPromise<IOfflineCloudPlayer> promise = cloudAPI.getCloudPlayerManager().getOfflineCloudPlayer(uniqueId);
        ICommunicationPromise<String> addressPromise = promise.then(offlinePlayer -> {
            IProperty<Object> property = offlinePlayer.getProperty(PlayerProperty.IP.name());
            if (property == null) return null;
            return property.getValueAsString();
        });
        return addressPromise.getNow();
    }

    private boolean canPunish(CommandSource source, UUID target) {
        if (!(source instanceof Player player)) return true;
        Optional<Player> playerOptional = instance.getProxyServer().getPlayer(target);
        if (playerOptional.isEmpty()) {
            AtomicBoolean atomicBoolean = new AtomicBoolean(true);
            ICommunicationPromise<ICloudPlayer> promise = cloudAPI.getCloudPlayerManager().getCloudPlayer(target);
            promise.addListener(future -> {
                if (future.isSuccess()) {
                    ICloudPlayer cloudPlayer = promise.get();
                    if (cloudPlayer.hasPermissionSync(ADMIN_PERMISSION)) atomicBoolean.set(false);
                    atomicBoolean.set(!cloudPlayer.hasPermissionSync(BYPASS_PERMISSION) || player.hasPermission(ADMIN_PERMISSION));
                } else {
                    ICommunicationPromise<IOfflineCloudPlayer> communicationPromise = cloudAPI.getCloudPlayerManager().getOfflineCloudPlayer(target);
                    communicationPromise.addListener(offlineFuture -> {
                        if (offlineFuture.isSuccess()) {
                            IPermissionPlayer permissionPlayer = OfflinePlayerExtensionKt.getPermissionPlayer(communicationPromise.get());
                            if (permissionPlayer.hasPermission(ADMIN_PERMISSION)) atomicBoolean.set(false);
                            atomicBoolean.set(!permissionPlayer.hasPermission(BYPASS_PERMISSION) || player.hasPermission(ADMIN_PERMISSION));
                        }
                    });
                }
            });
            return atomicBoolean.get();
        } else {
            Player targetPlayer = playerOptional.get();
            if (targetPlayer.hasPermission(ADMIN_PERMISSION)) return false;
            return !targetPlayer.hasPermission(BYPASS_PERMISSION) || player.hasPermission(ADMIN_PERMISSION);
        }
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
