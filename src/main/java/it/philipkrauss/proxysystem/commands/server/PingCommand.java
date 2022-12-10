package it.philipkrauss.proxysystem.commands.server;

import com.google.common.collect.Lists;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PingCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.ping";

    private final ProxySystem instance;

    public PingCommand() {
        this.instance = ProxySystem.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if(!(source instanceof Player player)) {
            source.sendMessage(instance.getOnlyPlayers());
            return;
        }
        String[] args = invocation.arguments();
        if(args.length == 1 && player.hasPermission(PERMISSION)) {
            Optional<Player> playerOptional = instance.getProxyServer().getPlayer(args[0]);
            if(playerOptional.isEmpty()) {
                player.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Spieler ist derzeit §cnicht §7online§8..."));
                return;
            }
            player.sendMessage(Component.text(instance.getPrefix() + "§7Der Ping von " + playerOptional.get().getUsername() + " §8● §e" + getPing(playerOptional.get())));
            return;
        }
        if(args.length != 0) {
            player.sendMessage(instance.getUsage("ping"));
            return;
        }
        player.sendMessage(Component.text(instance.getPrefix() + "§7Dein Ping §8● §e" + getPing(player)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if(invocation.source().hasPermission(PERMISSION) && invocation.arguments().length == 0) {
            List<String> playerNames = Lists.newArrayList();
            instance.getProxyServer().getAllPlayers().forEach(player -> playerNames.add(player.getUsername()));
            return playerNames;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

    private String getPing(Player player) {
        long ping = player.getPing();
        String color;
        if(ping <= 20) color = "§a";
        else if(ping <= 30) color = "§2";
        else if(ping <= 40) color = "§e";
        else if(ping <= 50) color = "§c";
        else color = "§4";
        return color + ping;
    }

}
