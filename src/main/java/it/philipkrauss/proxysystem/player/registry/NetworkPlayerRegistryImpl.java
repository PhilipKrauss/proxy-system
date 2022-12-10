package it.philipkrauss.proxysystem.player.registry;

import com.google.common.collect.Lists;
import it.philipkrauss.proxysystem.player.NetworkPlayer;

import java.util.List;
import java.util.UUID;

public class NetworkPlayerRegistryImpl implements NetworkPlayerRegistry {

    public static NetworkPlayerRegistryImpl create() {
        return new NetworkPlayerRegistryImpl();
    }

    private final List<NetworkPlayer> networkPlayers = Lists.newArrayList();

    private NetworkPlayerRegistryImpl() {
    }

    @Override
    public void registerNetworkPlayer(NetworkPlayer networkPlayer) {
        this.networkPlayers.add(0, networkPlayer);
    }

    @Override
    public void unregisterNetworkPlayer(NetworkPlayer networkPlayer) {
        this.networkPlayers.remove(networkPlayer);
    }

    @Override
    public NetworkPlayer getNetworkPlayer(UUID uniqueId) {
        return this.networkPlayers.stream().filter(networkPlayer -> networkPlayer.getUniqueId().equals(uniqueId)).findFirst().orElse(null);
    }

    @Override
    public List<NetworkPlayer> getNetworkPlayers() {
        return this.networkPlayers;
    }
}
