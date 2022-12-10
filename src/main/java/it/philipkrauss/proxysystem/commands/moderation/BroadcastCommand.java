package it.philipkrauss.proxysystem.commands.moderation;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;

public class BroadcastCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.broadcast";

    private final ProxySystem instance;

    public BroadcastCommand() {
        this.instance = ProxySystem.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if(!source.hasPermission(PERMISSION)) {
            source.sendMessage(instance.getNoPermissions());
            return;
        }
        String[] args = invocation.arguments();
        if(args.length < 1) {
            source.sendMessage(instance.getUsage("/broadcast <Nachricht>"));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        StringBuilder stringBuilder = new StringBuilder(args[0]);
        for(int index = 1; index < args.length; index++) {
            stringBuilder.append(" ").append(args[index]);
        }
        String message = stringBuilder.toString();
        String[] lines = message.split("%n");
        instance.getProxyServer().getAllPlayers().forEach(player -> {
            player.sendMessage(Component.text(instance.getBroadcastPrefix() + "§8§m-----------§r§8[ §e§lBroadcast §8]§8§m-----------"));
            player.sendMessage(Component.text(instance.getBroadcastPrefix() + ""));
            if(lines.length != 0) {
                for (String line : lines) {
                    player.sendMessage(Component.text(instance.getBroadcastPrefix() + "§7" + line.replace("&", "§")));
                }
            } else {
                player.sendMessage(Component.text(instance.getBroadcastPrefix() + "§7" + message.replace("&", "§")));
            }
            player.sendMessage(Component.text(instance.getBroadcastPrefix() + ""));
            player.sendMessage(Component.text(instance.getBroadcastPrefix() + "§8§m-----------§r§8[ §e§lBroadcast §8]§8§m-----------"));
        });
        if(lines.length != 0) {
            for (String line : lines) {
                instance.getProxyServer().getConsoleCommandSource().sendMessage(Component.text(instance.getBroadcastPrefix() + "§7" + line.replace("&", "§")));
            }
        } else {
            instance.getProxyServer().getConsoleCommandSource().sendMessage(Component.text(instance.getBroadcastPrefix() + "§7" + message.replace("&", "§")));
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
