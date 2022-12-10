package it.philipkrauss.proxysystem.commands.server;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;

public class HelpCommand implements SimpleCommand {

    private final ProxySystem instance;

    public HelpCommand() {
        this.instance = ProxySystem.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (invocation.arguments().length != 0) {
            source.sendMessage(instance.getUsage("/help"));
            return;
        }
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m--------------§r§8[ §e§lHelp §8]§8§m--------------"));
        source.sendMessage(Component.text(instance.getPrefix() + "§e/help §8● §7Zeigt diese Liste"));
        source.sendMessage(Component.text(instance.getPrefix() + "§e/friend §8● §7Zeigt die Befehle zum Freunde-System"));
        source.sendMessage(Component.text(instance.getPrefix() + "§e/message §8● §7Sendet Nachrichten an Freunde"));
        source.sendMessage(Component.text(instance.getPrefix() + "§e/reply §8● §7Antwortet auf Nachrichten"));
        source.sendMessage(Component.text(instance.getPrefix() + "§e/list §8● §7Zeigt die aktuelle Spieleranzahl"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m--------------§r§8[ §e§lHelp §8]§8§m--------------"));
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
