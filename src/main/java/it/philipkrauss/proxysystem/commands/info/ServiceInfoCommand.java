package it.philipkrauss.proxysystem.commands.info;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.service.ICloudService;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Collections;
import java.util.List;

public class ServiceInfoCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.serviceinfo";

    private final ProxySystem instance;
    private final ICloudAPI cloudAPI;

    public ServiceInfoCommand() {
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
            source.sendMessage(instance.getUsage("/serviceinfo [Service]"));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())){
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        if (args.length == 0) {
            List<ICloudService> services = cloudAPI.getCloudServiceManager().getAllCachedObjects();
            source.sendMessage(Component.text(instance.getPrefix() + "§7Eine Liste aller derzeit aktiven §eServices §8(§e" + services.size() + "§8)"));
            TextComponent.Builder serviceComponentBuilder = Component.text(instance.getPrefix()).append(Component.text("§e" + services.get(0).getName())
                    .clickEvent(ClickEvent.runCommand("/service " + services.get(0).getName()))
                    .hoverEvent(HoverEvent.showText(Component.text("§7Klicke für mehr Informationen")))).toBuilder();
            services.remove(0);
            services.forEach(service -> serviceComponentBuilder.append(Component.text("§8, ")).append(Component.text("§e" + service.getName())
                        .clickEvent(ClickEvent.runCommand("/service " + service.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("§7Klicke für mehr Informationen")))));
            source.sendMessage(serviceComponentBuilder.build());
            return;
        }
        ICloudService service = cloudAPI.getCloudServiceManager().getCloudServiceByName(args[0]);
        if (service == null) {
            source.sendMessage(Component.text(instance.getPrefix() + "§7Dieser Service wurde §cnicht §7gefunden§8..."));
            return;
        }
        String serviceName = service.getName();
        TextComponent nameComponent = Component.text("§e" + serviceName)
                .clickEvent(ClickEvent.runCommand("/connect " + serviceName))
                .hoverEvent(HoverEvent.showText(Component.text("§7Verbinde dich mit §e" + serviceName + "§8...")));
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lServiceInfo §8]§8§m-----------"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Name §8● §e").append(nameComponent));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Gruppe §8● §e" + service.getGroupName()));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Spieler §8● §e" + service.getOnlineCount() + " §8/ §e" + service.getMaxPlayers() +
                " §8(§e" + service.getOnlinePercentage() * 100 + "§8%)"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7State §8● §e" + getServiceState(service)));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Info §8● §e" + service.getMOTD()));
        source.sendMessage(Component.text(instance.getPrefix() + "§7Cloud-Informationen über diesen Service§8:"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Service-Typ §8● §e" + getServiceType(service)));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Wrapper §8● §e" + service.getWrapperName()));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Wartungen §8● §e" +
                visualizeCondition(service.getServiceGroup().isInMaintenance(), "Aktiviert", "Deaktiviert")));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Statisch §8● §e" +
                visualizeCondition(service.getServiceGroup().isStatic(), "Ja", "Nein")));
        source.sendMessage(Component.text(instance.getPrefix() + "§7Server-Informationen über diesen Service§8:"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Host §8● §e" + service.getHost() + ":" + service.getPort()));
        source.sendMessage(Component.text(instance.getPrefix() + "§8» §7Memory §8● §e" + service.getUsedMemory() + "§7Mb §8/ §e" + service.getMaxMemory() + "§7Mb" +
                " §8(§e" + (service.getUsedMemory() / service.getMaxMemory() * 100) + "§8%)"));
        source.sendMessage(Component.text(instance.getPrefix() + "§8§m------------§r§8[ §e§lServiceInfo §8]§8§m-----------"));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if(!invocation.source().hasPermission(PERMISSION)) return Collections.emptyList();
        if (invocation.arguments().length == 0)
            return ProxySystem.getInstance().getTabUtils().getServiceNameCompletions();
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

    private String visualizeCondition(boolean condition, String trueString, String falseString) {
        return condition ? "§a" + trueString + " §8/ §a✔" : "§c" + falseString + " §8/ §c✗";
    }

    private String getServiceState(ICloudService service) {
        return switch (service.getState()) {
            case PREPARED -> "Registriert";
            case STARTING -> "Startet";
            case VISIBLE -> "Online";
            case INVISIBLE -> "Versteckt";
            case CLOSED -> "Offline";
        };
    }

    private String getServiceType(ICloudService service) {
        return switch (service.getServiceType()) {
            case LOBBY -> "Lobby";
            case PROXY -> "Proxy";
            case SERVER -> "Server";
        };
    }

}
