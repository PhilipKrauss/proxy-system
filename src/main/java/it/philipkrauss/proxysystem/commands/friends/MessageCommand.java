package it.philipkrauss.proxysystem.commands.friends;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.friend.FriendProvider;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;
import net.kyori.adventure.text.Component;

import java.util.*;

public class MessageCommand implements SimpleCommand {

    private final ProxySystem instance;
    private final FriendProvider friendManager;

    public MessageCommand() {
        this.instance = ProxySystem.getInstance();
        this.friendManager = instance.getFriendProvider();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if(!(source instanceof Player player)) {
            source.sendMessage(instance.getNoPermissions());
            return;
        }
        String[] args = invocation.arguments();
        if(args.length <= 1) {
            player.sendMessage(instance.getUsage(instance.getFriendPrefix(),"/msg <Spieler> <Nachricht>"));
            return;
        }
        UUID friendUUID = UUIDProvider.getUniqueId(args[0]);
        if(friendUUID == null) {
            player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
            return;
        }
        String uniqueId = player.getUniqueId().toString();
        if(friendUUID.toString().equals(uniqueId)) {
            player.sendMessage(Component.text(instance.getPrefix() + "§7Du kannst dir §cnicht §7selbst schreiben§8..."));
            return;
        }
        if(!friendManager.areFriends(uniqueId, friendUUID.toString())) {
            player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du bist §cnicht §7mit §e" + args[0] + " §7befreundet§8..."));
            return;
        }
        Optional<Player> friendOptional = instance.getProxyServer().getPlayer(friendUUID);
        if(friendOptional.isEmpty()) {
            player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Freund ist derzeit §cnicht §7online§8..."));
            return;
        }
        Player friendPlayer = friendOptional.get();
        if(!friendManager.getFriendSettings(friendUUID.toString()).canReceiveMessages()) {
            player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Dieser Freund empfängt §ckeine §7Nachrichten§8..."));
            if(Objects.equals(friendManager.getReplier(friendPlayer.getUniqueId().toString()), player.getUniqueId().toString())) {
                player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Antworte deinem Freund mit §8● §e/r <Nachricht>"));
            }
            return;
        }
        friendManager.setReplier(friendPlayer.getUniqueId().toString(), player.getUniqueId().toString());
        StringBuilder message = new StringBuilder("§7" + args[1]);
        for(int index = 2; index < args.length; index++) {
            message.append(" §7").append(args[index]);
        }
        friendPlayer.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + player.getUsername() + " §8➜ §eDir §8● §7" + message));
        player.sendMessage(Component.text(instance.getFriendPrefix() + "§eDu §8➜ §e" + friendPlayer.getUsername() + " §8● §7" + message));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if(invocation.source() instanceof Player player) {
            String replier = friendManager.getReplier(player.toString());
            Optional<Player> playerOptional = instance.getProxyServer().getPlayer(UUID.fromString(replier));
            if(playerOptional.isEmpty()) return Collections.emptyList();
            String playerName = playerOptional.get().getUsername();
            return Collections.singletonList(playerName);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

}
