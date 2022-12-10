package it.philipkrauss.proxysystem.player;

import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.api.player.IOfflineCloudPlayer;
import eu.thesimplecloud.api.property.IProperty;
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise;
import eu.thesimplecloud.module.permission.PermissionPool;
import eu.thesimplecloud.module.permission.group.IPermissionGroup;
import eu.thesimplecloud.module.permission.player.IPermissionPlayer;
import eu.thesimplecloud.module.permission.player.OfflinePlayerExtensionKt;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkPlayer {

    public static NetworkPlayer create(UUID uniqueId) {
        return new NetworkPlayer(uniqueId);
    }

    private final UUID uniqueId;
    private ICloudPlayer cloudPlayer;
    private IOfflineCloudPlayer offlineCloudPlayer;

    private NetworkPlayer(UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.cloudPlayer = CloudAPI.getInstance().getCloudPlayerManager().getCachedCloudPlayer(uniqueId);
        if (!isCached()) {
            ProxySystem.getInstance().getLogger().info("Fetching CloudPlayer of " + uniqueId + "...");
            ICommunicationPromise<ICloudPlayer> communicationPromise = CloudAPI.getInstance().getCloudPlayerManager().getCloudPlayer(uniqueId);
            if (communicationPromise.isSuccess()) this.cloudPlayer = communicationPromise.getNow();
            ICommunicationPromise<IOfflineCloudPlayer> promise = CloudAPI.getInstance().getCloudPlayerManager().getOfflineCloudPlayer(uniqueId);
            this.offlineCloudPlayer = promise.getNow();
            ProxySystem.getInstance().getLogger().info("Successfully created NetworkPlayer of " + uniqueId);
        } else {
            ProxySystem.getInstance().getLogger().info("Successfully created NetworkPlayer of " + uniqueId);
        }
    }

    public boolean isCached() {
        return this.cloudPlayer != null;
    }

    public IProperty<Object> getProperty(PlayerProperty property) {
        if (this.cloudPlayer == null) return this.offlineCloudPlayer.getProperty(property.name());
        return this.cloudPlayer.getProperty(property.name());
    }

    public void setProperty(PlayerProperty property, Object value) {
        if (this.cloudPlayer == null) {
            this.offlineCloudPlayer.setProperty(property.name(), value);
            return;
        }
        this.cloudPlayer.setProperty(property.name(), value);
    }

    public boolean hasProperty(PlayerProperty property) {
        if (this.cloudPlayer == null) return this.offlineCloudPlayer.hasProperty(property.name());
        return this.cloudPlayer.hasProperty(property.name());
    }

    public boolean hasPermission(String permission) {
        if (this.getPermissionPlayer() == null) {
            Optional<Player> playerOptional = ProxySystem.getInstance().getProxyServer().getPlayer(this.uniqueId);
            if (playerOptional.isEmpty()) return false;
            return playerOptional.get().hasPermission(permission);
        }
        return this.getPermissionPlayer().hasPermission(permission);
    }

    public IPermissionPlayer getPermissionPlayer() {
        AtomicReference<IPermissionPlayer> player = new AtomicReference<>();
        IPermissionPlayer permissionPlayer = PermissionPool.getInstance().getPermissionPlayerManager().getCachedPermissionPlayer(uniqueId);
        if (permissionPlayer == null) {
            ICommunicationPromise<IOfflineCloudPlayer> offlineCloudPlayerPromise = CloudAPI.getInstance().getCloudPlayerManager().getOfflineCloudPlayer(this.uniqueId);
            offlineCloudPlayerPromise.addListener(offlineFuture -> {
                if (offlineFuture.isSuccess()) {
                    IOfflineCloudPlayer offlineCloudPlayer = offlineCloudPlayerPromise.get();
                    player.set(OfflinePlayerExtensionKt.getPermissionPlayer(offlineCloudPlayer));
                }
            });
        }
        player.set(permissionPlayer);
        return player.get();
    }

    public IPermissionGroup getPermissionGroup() {
        IPermissionPlayer permissionPlayer = this.getPermissionPlayer();
        IPermissionGroup defaultPermissionGroup = PermissionPool.getInstance().getPermissionGroupManager().getDefaultPermissionGroup();
        if (permissionPlayer == null) return defaultPermissionGroup;
        if (permissionPlayer.getAllNotExpiredPermissionGroups().size() == 0)
            return defaultPermissionGroup;
        return permissionPlayer.getHighestPermissionGroup();
    }

    public String getFormattedName() {
        if (this.cloudPlayer == null)
            return ProxySystem.getInstance().getPermissionProvider().getPrefix(this.getPermissionGroup().getName()) + UUIDProvider.getName(this.uniqueId);
        return ProxySystem.getInstance().getPermissionProvider().getPrefix(this.getPermissionGroup().getName()) + this.cloudPlayer.getName();
    }

    public String getColoredName() {
        if (this.cloudPlayer == null)
            return ProxySystem.getInstance().getPermissionProvider().getColor(this.getPermissionGroup().getName()) + UUIDProvider.getName(this.uniqueId);
        return ProxySystem.getInstance().getPermissionProvider().getColor(this.getPermissionGroup().getName()) + this.cloudPlayer.getName();
    }

    public ICloudPlayer getCloudPlayer() {
        return cloudPlayer;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

}
