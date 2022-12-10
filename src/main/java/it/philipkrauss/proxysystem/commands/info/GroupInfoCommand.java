package it.philipkrauss.proxysystem.commands.info;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.servicegroup.ICloudServiceGroup;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Collections;
import java.util.List;

public class GroupInfoCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.groupinfo";

    private final ProxySystem instance;
    private final ICloudAPI cloudAPI;

    public GroupInfoCommand() {
        this.instance = ProxySystem.getInstance();
        this.cloudAPI = CloudAPI.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission(PERMISSION)) {
            source.sendMessage(instance.getNoPermissions());
            return;
        }
        String[] args = invocation.arguments();
        if (args.length > 1) {
            source.sendMessage(instance.getUsage("/groupinfo [Gruppe]"));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())){
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        if (args.length == 0) {
            List<ICloudServiceGroup> serviceGroups = cloudAPI.getCloudServiceGroupManager().getAllCachedObjects();
            source.sendMessage(Component.text(instance.getPrefix() + "§7Eine Liste aller registrierten §eGruppen §8(§e" + serviceGroups.size() + "§8)"));
            TextComponent.Builder serviceComponentBuilder = Component.text(instance.getPrefix()).append(Component.text("§e" + serviceGroups.get(0).getName())
                    .clickEvent(ClickEvent.runCommand("/group " + serviceGroups.get(0).getName()))
                    .hoverEvent(HoverEvent.showText(Component.text("§7Klicke für mehr Informationen")))).toBuilder();
            serviceGroups.remove(0);
            serviceGroups.forEach(group -> serviceComponentBuilder.append(Component.text("§8, ")).append(Component.text("§e" + group.getName())
                        .clickEvent(ClickEvent.runCommand("/group " + group.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("§7Klicke für mehr Informationen")))));
            source.sendMessage(serviceComponentBuilder.build());
            return;
        }
        ICloudServiceGroup group = cloudAPI.getCloudServiceGroupManager().getServiceGroupByName(args[0]);
        if (group == null) {
            source.sendMessage(Component.text(instance.getPrefix() + "§7Diese Gruppe wurde §cnicht §7gefunden§8..."));
            return;
        }
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lGroupInfo §8]§8§m-----------"));
        source.sendMessage(Component.text(instance.getPrefix() + "§7Informationen über diese Gruppe§8:"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Name §8● §e" + group.getName()));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Services §8● §e" + group.getOnlineServiceCount() + " §8/ §e"
                + group.getMaximumOnlineServiceCount() + " §8(§e" + (group.getOnlineServiceCount() / group.getMaximumOnlineServiceCount() * 100) + "§8%)"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Spieler §8● §e" + group.getOnlinePlayerCount() + " §8/ §e"
                + group.getMaxPlayers() + " §8(§e" + (group.getOnlinePlayerCount() / group.getMaxPlayers() * 100) + "§8%)"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Service-Typ §8● §e" + getServiceType(group)));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Wrapper §8● §e" + group.getWrapperName()));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Wartungen §8● §e" +
                visualizeCondition(group.isInMaintenance(), "Aktiviert", "Deaktiviert")));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Statisch §8● §e" + visualizeCondition(group.isStatic(), "Ja", "Nein")));
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lGroupInfo §8]§8§m-----------"));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if(!invocation.source().hasPermission(PERMISSION)) return Collections.emptyList();
        if (invocation.arguments().length == 0)
            return ProxySystem.getInstance().getTabUtils().getServiceGroupNameCompletions();
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

    private String visualizeCondition(boolean condition, String trueString, String falseString) {
        return condition ? "§a" + trueString + " §8/ §a✔" : "§c" + falseString + " §8/ §c✗";
    }

    private String getServiceType(ICloudServiceGroup group) {
        return switch (group.getServiceType()) {
            case LOBBY -> "Lobby";
            case PROXY -> "Proxy";
            case SERVER -> "Server";
        };
    }

}
