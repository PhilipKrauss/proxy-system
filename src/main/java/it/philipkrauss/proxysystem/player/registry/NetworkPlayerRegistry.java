package it.philipkrauss.proxysystem.player.registry;

import it.philipkrauss.proxysystem.player.NetworkPlayer;

import java.util.List;
import java.util.UUID;

public interface NetworkPlayerRegistry {

    void registerNetworkPlayer(NetworkPlayer networkPlayer);

    void unregisterNetworkPlayer(NetworkPlayer networkPlayer);

    NetworkPlayer getNetworkPlayer(UUID uniqueId);

    List<NetworkPlayer> getNetworkPlayers();

}
