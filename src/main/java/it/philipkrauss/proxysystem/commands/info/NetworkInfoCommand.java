package it.philipkrauss.proxysystem.commands.info;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.service.ICloudService;
import eu.thesimplecloud.api.servicegroup.ICloudServiceGroup;
import eu.thesimplecloud.api.wrapper.IWrapperInfo;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Collections;
import java.util.List;

public class NetworkInfoCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.networkinfo";

    private final ProxySystem instance;
    private final ICloudAPI cloudAPI;

    public NetworkInfoCommand() {
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
        if (args.length != 0) {
            source.sendMessage(instance.getUsage("/networkinfo"));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        List<IWrapperInfo> wrappers = cloudAPI.getWrapperManager().getAllCachedObjects();
        List<ICloudServiceGroup> groups = cloudAPI.getCloudServiceGroupManager().getAllCachedObjects();
        List<ICloudService> services = cloudAPI.getCloudServiceManager().getAllCachedObjects();
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lNetworkInfo §8]§8§m-----------"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Cloud-System §8● §eSimpleCloud"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Cloud-Version §8● §e" + ProxySystem.CLOUD_VERSION));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Informationen über alle Wrapper §8(§e" + wrappers.size() + "§8)"));
        wrappers.forEach(wrapper -> source.sendMessage(Component.text(instance.getPrefix() + "§8» §7" + wrapper.getName() + " §8● §e" + wrapper.getHost())));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Informationen über alle Gruppen §8(§e" + groups.size() + "§8)"));
        groups.forEach(group -> {
            Component groupComponent = Component.text(instance.getPrefix() + "§8» ")
                    .append(Component.text("§e" + group.getName())
                            .clickEvent(ClickEvent.runCommand("/groupinfo " + group.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("§7Klicke für mehr Informationen"))))
                    .append(Component.text(" §8(§e" + group.getOnlineServiceCount() + "§8)" + (group.isInMaintenance() ? " §8● §c!" : "")));
            source.sendMessage(groupComponent);
            services.stream().filter(service -> service.getServiceGroup().equals(group)).forEach(service -> source.sendMessage(Component.text(instance.getPrefix() +
                    "§8» §8● §e" + service.getName() + " §8(§e" + service.getOnlineCount() + "§8/§e" + service.getMaxPlayers() + "§8)")));
        });
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Informationen über Spieler §8(§e" + groups.size() + "§8)"));
        source.sendMessage(Component.text(instance.getPrefix() +
                "§8» §7Aktuelle Spielerzahl §8● §e" + cloudAPI.getCloudPlayerManager().getNetworkOnlinePlayerCount().getNow()));
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lNetworkInfo §8]§8§m-----------"));
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
