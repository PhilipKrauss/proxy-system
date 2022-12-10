package it.philipkrauss.proxysystem.commands.friends;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.friend.FriendProvider;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ReplyCommand implements SimpleCommand {

    private final ProxySystem instance;
    private final FriendProvider friendManager;

    public ReplyCommand() {
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
            player.sendMessage(instance.getUsage(instance.getFriendPrefix(),"/r <Nachricht>"));
            return;
        }
        String friend = friendManager.getReplier(player.getUniqueId().toString());
        if(friend == null) {
            player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du kannst zurzeit §cniemandem §7antworten§8..."));
            return;
        }
        Optional<Player> playerOptional = this.instance.getProxyServer().getPlayer(UUID.fromString(friend));
        if(playerOptional.isEmpty()) {
            player.sendMessage(Component.text(instance.getFriendPrefix() + "§e" + args[0] + " §7ist derzeit §cnicht §7online§8..."));
            return;
        }
        Player friendPlayer = playerOptional.get();
        String uniqueId = player.getUniqueId().toString();
        if(!friendManager.areFriends(uniqueId, friend)) {
            player.sendMessage(Component.text(instance.getFriendPrefix() + "§7Du bist §cnicht §7mit §e" + args[0] + " §7befreundet§8..."));
            return;
        }
        friendManager.setReplier(friend, player.getUniqueId().toString());
        StringBuilder message = new StringBuilder("§7" + args[0]);
        for(int index = 1; index < args.length; index++) {
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
