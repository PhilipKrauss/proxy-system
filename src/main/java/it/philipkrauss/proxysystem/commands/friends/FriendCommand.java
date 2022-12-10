package it.philipkrauss.proxysystem.commands.friends;

import com.google.common.collect.Lists;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.friend.FriendProvider;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FriendCommand implements SimpleCommand {

    private final ProxySystem instance;

    private final FriendProvider friendManager;

    public FriendCommand() {
        this.instance = ProxySystem.getInstance();
        this.friendManager = ProxySystem.getInstance().getFriendProvider();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if(!(source instanceof Player player)) {
            source.sendMessage(instance.getOnlyPlayers());
            return;
        }
        String[] args = invocation.arguments();
        String uniqueId = player.getUniqueId().toString();
        if(args.length == 1) {
            switch (args[0].toUpperCase()) {
                case "HELP" -> {
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§8§m-----------§r§8[ §4§lFriendSystem §8]§8§m-----------"));
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e/friend add <Name>"));
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e/friend remove <Name>"));
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e/friend accept <Name>"));
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e/friend deny <Name>"));
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e/friend jump <Name>"));
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e/friend requests [Seite]"));
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e/friend list [Seite]"));
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§8§m-----------§r§8[ §4§lFriendSystem §8]§8§m-----------"));
                    return;
                }
                case "REQUESTS" -> {
                    List<String> friendRequests = Lists.newArrayList();
                    friendManager.getFriendRequests(uniqueId).forEach(friendUniqueId -> friendRequests.add(UUIDProvider.getName(UUID.fromString(friendUniqueId))));
                    if(friendRequests.size() == 0) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du hast §ckeine §7offenen Freundschaftsanfragen§8..."));
                        return;
                    }
                    int page = 1;
                    int max = friendRequests.size() / 8 + 1;
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Deine Anfragen §8● §7Seite §e" + page + " §7von §e" + max));
                    if(friendRequests.size() < page * 8) {
                        for (String friendRequest : friendRequests) {
                            TextComponent text = Component.text(instance.getFriendPrefix() + "§8» §e" + friendRequest + " §8● ")
                                    .append(Component.text("§aAnnehmen").clickEvent(ClickEvent.runCommand("/friend accept " + friendRequest))
                                            .hoverEvent(HoverEvent.showText(Component.text("§7Annehmen §8● §e/friend accept"))))
                                    .append(Component.text(" §8/ "))
                                    .append(Component.text("§cAblehnen").clickEvent(ClickEvent.runCommand("/friend deny " + friendRequest))
                                            .hoverEvent(HoverEvent.showText(Component.text("§7Ablehnen §8● §e/friend deny"))));
                            player.sendMessage(text);
                        }
                    } else {
                        for(int index = 0; index < page * 8; index++) {
                            TextComponent text = Component.text(instance.getFriendPrefix() + "§8» §e" + friendRequests.get(index) + " §8● ")
                                    .append(Component.text("§aAnnehmen").clickEvent(ClickEvent.runCommand("/friend accept " + friendRequests.get(index)))
                                            .hoverEvent(HoverEvent.showText(Component.text("§7Annehmen §8● §e/friend accept"))))
                                    .append(Component.text(" §8/ "))
                                    .append(Component.text("§cAblehnen").clickEvent(ClickEvent.runCommand("/friend deny " + friendRequests.get(index)))
                                            .hoverEvent(HoverEvent.showText(Component.text("§7Ablehnen §8● §e/friend deny"))));
                            player.sendMessage(text);
                        }
                    }
                    player.sendMessage(Component.text(instance.getFriendPrefix() + instance.getUsage("/friend requests <Seite>")));
                    return;
                }
                case "LIST" -> {
                    List<String> onlineFriends = Lists.newArrayList();
                    List<String> offlineFriends = Lists.newArrayList();
                    List<String> friendNames = Lists.newArrayList();
                    friendManager.getFriends(uniqueId).forEach(friendUniqueId -> {
                        UUID uuid = UUID.fromString(friendUniqueId);
                        Optional<Player> playerOptional = instance.getProxyServer().getPlayer(uuid);
                        if(playerOptional.isPresent()) {
                            onlineFriends.add(playerOptional.get().getUsername());
                        } else {
                            offlineFriends.add(UUIDProvider.getName(uuid));
                        }
                    });
                    friendNames.addAll(onlineFriends);
                    friendNames.addAll(offlineFriends);
                    if(friendNames.size() == 0) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du hast noch §ckeine §7Freunde§8..."));
                        return;
                    }
                    int page = 1;
                    int max = friendNames.size() / 8 + 1;
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Freunde §8● §7Seite §e" + page + " §7von §e" + max));
                    if(friendNames.size() < page * 8) {
                        for (String friendName : friendNames) {
                            Optional<Player> playerOptional = instance.getProxyServer().getPlayer(friendName);
                            if (playerOptional.isEmpty()) {
                                player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendName + " §8● §7Offline"));
                            } else {
                                Player friend = playerOptional.get();
                                Optional<ServerConnection> currentServer = friend.getCurrentServer();
                                if (currentServer.isPresent()) {
                                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendName + " §8● §7Online auf "
                                            + currentServer.get().getServerInfo().getName()));
                                } else {
                                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendName + " §8● §7Online"));
                                }
                            }
                        }
                    } else {
                        for(int index = 0; index < page * 8; index++) {
                            Optional<Player> playerOptional = instance.getProxyServer().getPlayer(friendNames.get(index));
                            if (playerOptional.isEmpty()) {
                                player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendNames.get(index) + " §8● §7Offline"));
                            } else {
                                Player friend = playerOptional.get();
                                Optional<ServerConnection> currentServer = friend.getCurrentServer();
                                if (currentServer.isPresent()) {
                                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendNames.get(index) + " §8● §7Online auf "
                                            + currentServer.get().getServerInfo().getName()));
                                } else {
                                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendNames.get(index) + " §8● §7Online"));
                                }
                            }
                        }
                    }
                    player.sendMessage(Component.text(instance.getFriendPrefix() + instance.getUsage("/friend list <Seite>")));
                    return;
                }
                default -> source.sendMessage(instance.getUsage("/friend help"));
            }
            return;
        }
        if(args.length == 2) {
            String friendName = args[1];
            if(friendName.equals(player.getUsername())) {
                player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du kannst §cnicht §7mit dir selbst interagieren§8..."));
                return;
            }
            switch (args[0].toUpperCase()) {
                case "ADD" -> {
                    UUID friendUUID = UUIDProvider.getUniqueId(friendName);
                    if(friendUUID == null) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
                        return;
                    }
                    if(!friendManager.getFriendSettings(friendUUID.toString()).canReceiveRequests()) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler akzeptiert §ckeine §7Anfragen§8..."));
                        return;
                    }
                    if(friendManager.existsFriendRequest(uniqueId, friendUUID.toString())) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du hast diesem Spieler §cbereits §7eine Anfrage geschickt§8..."));
                        return;
                    }
                    if(friendManager.existsFriendRequest(friendUUID.toString(), uniqueId)) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler hat dir §cbereits §7eine Anfrage gesendet§8..."));
                        player.sendMessage(Component.text(instance.getFriendPrefix() + instance.getUsage("/friend accept " + friendName)));
                        return;
                    }
                    friendManager.createFriendRequest(uniqueId, friendUUID.toString());
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du hast §e" + friendName + " §7eine Anfrage gesendet§8."));
                    Optional<Player> friendOptional = instance.getProxyServer().getPlayer(friendUUID);
                    if(friendOptional.isPresent()) {
                        Player friendPlayer = friendOptional.get();
                        friendPlayer.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + player.getUsername() + " §7möchte dein Freund sein§8."));
                        TextComponent text = Component.text(instance.getFriendPrefix())
                                .append(Component.text("§a§lAnnehmen").clickEvent(ClickEvent.runCommand("/friend accept " + player.getUsername()))
                                        .hoverEvent(HoverEvent.showText(Component.text("§7Annehmen §8● §e/friend accept"))))
                                .append(Component.text(" §8● "))
                                .append(Component.text("§c§lAblehnen").clickEvent(ClickEvent.runCommand("/friend deny " + player.getUsername()))
                                        .hoverEvent(HoverEvent.showText(Component.text("§7Ablehnen §8● §e/friend deny"))));
                        friendPlayer.sendMessage(text);
                    }
                    return;
                }
                case "REMOVE" -> {
                    UUID friendUUID = UUIDProvider.getUniqueId(friendName);
                    if(friendUUID == null) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
                        return;
                    }
                    if(!friendManager.areFriends(uniqueId, friendUUID.toString())) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du bist §cnicht §7mit §e" + friendName + " §7befreundet§8..."));
                        return;
                    }
                    friendManager.deleteFriend(uniqueId, friendUUID.toString());
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Die Freundschaft mit §e" + friendName + " §7wurde aufgelöst§8."));
                    Optional<Player> friendOptional = instance.getProxyServer().getPlayer(friendUUID);
                    if(friendOptional.isPresent()) {
                        Player friendPlayer = friendOptional.get();
                        friendPlayer.sendMessage(Component.text(instance.getFriendPrefix() + "§7Die Freundschaft mit §e" + player.getUsername() + " §7wurde aufgelöst§8."));
                    }
                    return;
                }
                case "ACCEPT" -> {
                    UUID friendUUID = UUIDProvider.getUniqueId(friendName);
                    if(friendUUID == null) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
                        return;
                    }
                    if(!friendManager.existsFriendRequest(friendUUID.toString(), uniqueId)) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler hat dir §ckeine §7Anfrage geschickt§8..."));
                        return;
                    }
                    friendManager.deleteFriendRequest(friendUUID.toString(), uniqueId);
                    friendManager.addFriend(friendUUID.toString(), uniqueId);
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Die Anfrage von §e" + friendName + " §7wurde §aakzeptiert§8."));
                    Optional<Player> friendOptional = instance.getProxyServer().getPlayer(friendUUID);
                    if(friendOptional.isPresent()) {
                        Player friendPlayer = friendOptional.get();
                        friendPlayer.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + player.getUsername() + " §7hat deine Anfrage §aakzeptiert§8."));
                    }
                    return;
                }
                case "DENY" -> {
                    UUID friendUUID = UUIDProvider.getUniqueId(friendName);
                    if(friendUUID == null) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
                        return;
                    }
                    if(!friendManager.existsFriendRequest(friendUUID.toString(), uniqueId)) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler hat dir §ckeine §7Anfrage geschickt§8..."));
                        return;
                    }
                    friendManager.deleteFriendRequest(friendUUID.toString(), uniqueId);
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Die Anfrage von §e" + friendName + " §7wurde §cabgelehnt§8."));
                    Optional<Player> friendOptional = instance.getProxyServer().getPlayer(friendUUID);
                    if(friendOptional.isPresent()) {
                        Player friendPlayer = friendOptional.get();
                        friendPlayer.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + player.getUsername() + " §7hat deine Anfrage §cabgelehnt§8."));
                    }
                    return;
                }
                case "JUMP" -> {
                    UUID friendUUID = UUIDProvider.getUniqueId(friendName);
                    if(friendUUID == null) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
                        return;
                    }
                    if(!friendManager.areFriends(uniqueId, friendUUID.toString())) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du bist §cnicht §7mit §e" + friendName + " §7befreundet§8..."));
                        return;
                    }
                    Optional<Player> friendOptional = instance.getProxyServer().getPlayer(friendUUID);
                    if(friendOptional.isEmpty()) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Freund ist derzeit §cnicht §7online§8..."));
                        return;
                    }
                    Player friendPlayer = friendOptional.get();
                    if(!friendManager.getFriendSettings(friendUUID.toString()).canJumpAfter()) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du kannst diesem Spieler §cnicht §7nachspringen§8..."));
                        return;
                    }
                    Optional<ServerConnection> connectionOptional = friendPlayer.getCurrentServer();
                    if(connectionOptional.isEmpty()) {
                        player.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Spieler ist auf §ckeinem §7Server§8..."));
                        return;
                    }
                    ServerConnection connection = connectionOptional.get();
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Springe zu §e" + friendPlayer.getUsername() + "§8..."));
                    ConnectionRequestBuilder connectionRequest = player.createConnectionRequest(connection.getServer());
                    CompletableFuture<ConnectionRequestBuilder.Result> connectionFuture = connectionRequest.connect();
                    if (connectionFuture.isDone()) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du wurdest erfolgreich verbunden§8."));
                    } else {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Ein §cunerwarteter §7Fehler ist §caufgetreten§8..."));
                    }
                    return;
                }
                case "REQUESTS" -> {
                    try {
                        Integer.parseInt(args[1]);
                    }catch (NumberFormatException exception) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + instance.getUsage("/friend requests <Seite>")));
                        return;
                    }
                    List<String> friendRequests = Lists.newArrayList();
                    friendManager.getFriendRequests(uniqueId).forEach(friendUniqueId -> friendRequests.add(UUIDProvider.getName(UUID.fromString(friendUniqueId))));
                    if(friendRequests.size() == 0) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du hast §ckeine §7offenen Freundschaftsanfragen§8..."));
                        return;
                    }
                    int page = Integer.parseInt(args[1]);
                    int max = friendRequests.size() / 8 + 1;
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Deine Anfragen §8● §7Seite §e" + page + " §7von §e" + max));
                    if(friendRequests.size() < page * 8) {
                        for (String friendRequest : friendRequests) {
                            TextComponent text = Component.text(instance.getFriendPrefix() + "§8» §e" + friendRequest + " §8● ")
                                    .append(Component.text("§aAnnehmen").clickEvent(ClickEvent.runCommand("/friend accept " + friendRequest))
                                            .hoverEvent(HoverEvent.showText(Component.text("§7Annehmen §8● §e/friend accept"))))
                                    .append(Component.text(" §8/ "))
                                    .append(Component.text("§cAblehnen").clickEvent(ClickEvent.runCommand("/friend deny " + friendRequest))
                                            .hoverEvent(HoverEvent.showText(Component.text("§7Ablehnen §8● §e/friend deny"))));
                            player.sendMessage(text);
                        }
                    } else {
                        for(int index = 0; index < page * 8; index++) {
                            TextComponent text = Component.text(instance.getFriendPrefix() + "§8» §e" + friendRequests.get(index) + " §8● ")
                                    .append(Component.text("§aAnnehmen").clickEvent(ClickEvent.runCommand("/friend accept " + friendRequests.get(index)))
                                            .hoverEvent(HoverEvent.showText(Component.text("§7Annehmen §8● §e/friend accept"))))
                                    .append(Component.text(" §8/ "))
                                    .append(Component.text("§cAblehnen").clickEvent(ClickEvent.runCommand("/friend deny " + friendRequests.get(index)))
                                            .hoverEvent(HoverEvent.showText(Component.text("§7Ablehnen §8● §e/friend deny"))));
                            player.sendMessage(text);
                        }
                    }
                    player.sendMessage(Component.text(instance.getFriendPrefix() + instance.getUsage("/friend requests <Seite>")));
                    return;
                }
                case "LIST" -> {
                    try {
                        Integer.parseInt(args[1]);
                    }catch (NumberFormatException exception) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + instance.getUsage("/friend list <Seite>")));
                        return;
                    }
                    List<String> onlineFriends = Lists.newArrayList();
                    List<String> offlineFriends = Lists.newArrayList();
                    List<String> friendNames = Lists.newArrayList();
                    friendManager.getFriends(uniqueId).forEach(friendUniqueId -> {
                        UUID uuid = UUID.fromString(friendUniqueId);
                        Optional<Player> playerOptional = instance.getProxyServer().getPlayer(uuid);
                        if(playerOptional.isPresent()) {
                            onlineFriends.add(playerOptional.get().getUsername());
                        } else {
                            offlineFriends.add(UUIDProvider.getName(uuid));
                        }
                    });
                    friendNames.addAll(onlineFriends);
                    friendNames.addAll(offlineFriends);
                    if(friendNames.size() == 0) {
                        player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du hast noch §ckeine §7Freunde§8..."));
                        return;
                    }
                    int page = Integer.parseInt(args[1]);
                    int max = friendNames.size() / 8 + 1;
                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Freunde §8● §7Seite §e" + page + " §7von §e" + max));
                    if(friendNames.size() < page * 8) {
                        for (String friend : friendNames) {
                            Optional<Player> playerOptional = instance.getProxyServer().getPlayer(friend);
                            if (playerOptional.isEmpty()) {
                                player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friend + " §8● §7Offline"));
                            } else {
                                Player friendPlayer = playerOptional.get();
                                Optional<ServerConnection> currentServer = friendPlayer.getCurrentServer();
                                if (currentServer.isPresent()) {
                                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendPlayer + " §8● §7Online auf "
                                            + currentServer.get().getServerInfo().getName()));
                                } else {
                                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendPlayer + " §8● §7Online"));
                                }
                            }
                        }
                    } else {
                        for(int index = 0; index < page * 8; index++) {
                            Optional<Player> playerOptional = instance.getProxyServer().getPlayer(friendNames.get(index));
                            if (playerOptional.isEmpty()) {
                                player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendNames.get(index) + " §8● §7Offline"));
                            } else {
                                Player friend = playerOptional.get();
                                Optional<ServerConnection> currentServer = friend.getCurrentServer();
                                if (currentServer.isPresent()) {
                                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendNames.get(index) + " §8● §7Online auf "
                                            + currentServer.get().getServerInfo().getName()));
                                } else {
                                    player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + friendNames.get(index) + " §8● §7Online"));
                                }
                            }
                        }
                    }
                    player.sendMessage(Component.text(instance.getFriendPrefix() + instance.getUsage("/friend list <Seite>")));
                    return;
                }
                default -> source.sendMessage(instance.getUsage("/friend help"));
            }
        }
        source.sendMessage(instance.getUsage("/friend help"));
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
