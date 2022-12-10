package it.philipkrauss.proxysystem;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.external.ICloudModule;
import it.philipkrauss.proxysystem.commands.cloud.MaintenanceCommand;
import it.philipkrauss.proxysystem.commands.connection.ConnectCommand;
import it.philipkrauss.proxysystem.commands.connection.FindCommand;
import it.philipkrauss.proxysystem.commands.connection.SendCommand;
import it.philipkrauss.proxysystem.commands.friends.FriendCommand;
import it.philipkrauss.proxysystem.commands.friends.MessageCommand;
import it.philipkrauss.proxysystem.commands.friends.ReplyCommand;
import it.philipkrauss.proxysystem.commands.info.GroupInfoCommand;
import it.philipkrauss.proxysystem.commands.info.NetworkInfoCommand;
import it.philipkrauss.proxysystem.commands.info.PlayerInfoCommand;
import it.philipkrauss.proxysystem.commands.info.ServiceInfoCommand;
import it.philipkrauss.proxysystem.commands.moderation.BroadcastCommand;
import it.philipkrauss.proxysystem.commands.punishment.*;
import it.philipkrauss.proxysystem.commands.server.HelpCommand;
import it.philipkrauss.proxysystem.commands.server.ListCommand;
import it.philipkrauss.proxysystem.commands.team.LoginCommand;
import it.philipkrauss.proxysystem.commands.team.LogoutCommand;
import it.philipkrauss.proxysystem.commands.team.TeamChatCommand;
import it.philipkrauss.proxysystem.database.DatabaseAdapter;
import it.philipkrauss.proxysystem.listener.cloud.CloudPlayerDisconnectListener;
import it.philipkrauss.proxysystem.listener.cloud.CloudPlayerRegisteredListener;
import it.philipkrauss.proxysystem.listener.proxy.KickedFromServerListener;
import it.philipkrauss.proxysystem.listener.proxy.PreLoginListener;
import it.philipkrauss.proxysystem.manager.TeamManager;
import it.philipkrauss.proxysystem.message.MessageChannelManager;
import it.philipkrauss.proxysystem.player.registry.NetworkPlayerRegistry;
import it.philipkrauss.proxysystem.player.registry.NetworkPlayerRegistryImpl;
import it.philipkrauss.proxysystem.provider.insults.InsultProvider;
import it.philipkrauss.proxysystem.provider.permission.PermissionProvider;
import it.philipkrauss.proxysystem.provider.punishment.PunishProvider;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;
import it.philipkrauss.proxysystem.scheduler.MaintenanceScheduler;
import it.philipkrauss.proxysystem.utils.TabUtils;
import it.philipkrauss.proxysystem.utils.TimeUtils;
import it.philipkrauss.proxysystem.utils.regex.RegexGenerator;
import it.philipkrauss.proxysystem.commands.server.PingCommand;
import it.philipkrauss.proxysystem.provider.friend.FriendProvider;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

@Plugin(id = "proxy-system", name = "proxy-system", version = "1.0-SNAPSHOT", dependencies = {
        @Dependency(id = "simplecloud_permission"),
        @Dependency(id = "simplecloud_plugin")
})
public class ProxySystem {

    public static final String CLOUD_VERSION = "2.4.1";

    private static ProxySystem instance;

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final DatabaseAdapter databaseAdapter;
    private final UUIDProvider uuidProvider;

    private NetworkPlayerRegistry networkPlayerRegistry;
    private PermissionProvider permissionProvider;
    private PunishProvider punishProvider;
    private InsultProvider insultProvider;
    private FriendProvider friendProvider;
    private TeamManager teamManager;

    private MaintenanceScheduler maintenanceScheduler;

    private MessageChannelManager messageChannelManager;

    private RegexGenerator regexGenerator;

    private TabUtils tabUtils;
    private TimeUtils timeUtils;

    @Inject
    public ProxySystem(ProxyServer proxyServer, Logger logger) {
        instance = this;
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.databaseAdapter = DatabaseAdapter.create();
        this.uuidProvider = UUIDProvider.create();
        this.loadInternal();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        CommandManager commandManager = this.proxyServer.getCommandManager();
        // cloud
        commandManager.register(commandManager.metaBuilder("maintenance").aliases("wartung").plugin(this).build(), new MaintenanceCommand());
        // connection
        commandManager.register(commandManager.metaBuilder("connect").aliases("join").plugin(this).build(), new ConnectCommand());
        commandManager.register(commandManager.metaBuilder("find").aliases("locate").plugin(this).build(), new FindCommand());
        commandManager.register(commandManager.metaBuilder("send").plugin(this).build(), new SendCommand());
        // friends
        commandManager.register(commandManager.metaBuilder("friend").plugin(this).build(), new FriendCommand());
        commandManager.register(commandManager.metaBuilder("message").aliases("msg").plugin(this).build(), new MessageCommand());
        commandManager.register(commandManager.metaBuilder("reply").aliases("r").plugin(this).build(), new ReplyCommand());
        // info
        commandManager.register(commandManager.metaBuilder("groupinfo").plugin(this).build(), new GroupInfoCommand());
        commandManager.register(commandManager.metaBuilder("networkinfo").plugin(this).build(), new NetworkInfoCommand());
        commandManager.register(commandManager.metaBuilder("playerinfo").plugin(this).build(), new PlayerInfoCommand());
        commandManager.register(commandManager.metaBuilder("serviceinfo").aliases("service").plugin(this).build(), new ServiceInfoCommand());
        // moderation
        commandManager.register(commandManager.metaBuilder("broadcast").aliases("bc").plugin(this).build(), new BroadcastCommand());
        // punishment
        commandManager.register(commandManager.metaBuilder("ban").plugin(this).build(), new BanCommand());
        commandManager.register(commandManager.metaBuilder("exemption").aliases("ea").plugin(this).build(), new ExemptionCommand());
        commandManager.register(commandManager.metaBuilder("kick").plugin(this).build(), new KickCommand());
        commandManager.register(commandManager.metaBuilder("mute").plugin(this).build(), new MuteCommand());
        commandManager.register(commandManager.metaBuilder("punish").plugin(this).build(), new PunishCommand());
        commandManager.register(commandManager.metaBuilder("unpunish").plugin(this).build(), new UnpunishCommand());
        // server
        commandManager.register(commandManager.metaBuilder("help").plugin(this).build(), new HelpCommand());
        commandManager.register(commandManager.metaBuilder("list").plugin(this).build(), new ListCommand());
        commandManager.register(commandManager.metaBuilder("ping").plugin(this).build(), new PingCommand());
        // team
        commandManager.register(commandManager.metaBuilder("login").plugin(this).build(), new LoginCommand());
        commandManager.register(commandManager.metaBuilder("logout").plugin(this).build(), new LogoutCommand());
        commandManager.register(commandManager.metaBuilder("teamchat").aliases("tc").plugin(this).build(), new TeamChatCommand());

        EventManager eventManager = this.proxyServer.getEventManager();
        eventManager.register(this, new KickedFromServerListener());
        eventManager.register(this, new PreLoginListener());
    }

    private void loadInternal() {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            boolean valid = connection.isValid(30);
            if(!valid) {
                getLogger().error("The system couldn't create a database-connection...");
                return;
            }
        }catch (SQLException exception) {
            exception.printStackTrace();
            getLogger().error("The system couldn't create a database-connection...");
            return;
        }
        ICloudAPI cloudAPI = CloudAPI.getInstance();
        ICloudModule cloudModule = cloudAPI.getThisSidesCloudModule();
        cloudAPI.getMessageChannelManager().registerMessageChannel(cloudModule, "punishUpdate", String.class);
        this.messageChannelManager = MessageChannelManager.create();
        this.networkPlayerRegistry = NetworkPlayerRegistryImpl.create();
        this.timeUtils = TimeUtils.create();
        this.tabUtils = TabUtils.create();
        this.regexGenerator = RegexGenerator.create();
        this.permissionProvider = PermissionProvider.create();
        this.punishProvider = PunishProvider.create();
        this.insultProvider = InsultProvider.create();
        this.friendProvider = FriendProvider.create();
        this.teamManager = TeamManager.create();
        this.maintenanceScheduler = MaintenanceScheduler.create();
        cloudAPI.getEventManager().registerListener(cloudModule, new CloudPlayerRegisteredListener());
        cloudAPI.getEventManager().registerListener(cloudModule, new CloudPlayerDisconnectListener());
    }

    public static ProxySystem getInstance() {
        return instance;
    }

    public DatabaseAdapter getDatabaseAdapter() {
        return databaseAdapter;
    }

    public UUIDProvider getUUIDProvider() {
        return uuidProvider;
    }

    public RegexGenerator getRegexGenerator() {
        return regexGenerator;
    }

    public NetworkPlayerRegistry getNetworkPlayerRegistry() {
        return networkPlayerRegistry;
    }

    public PermissionProvider getPermissionProvider() {
        return permissionProvider;
    }

    public PunishProvider getPunishProvider() {
        return punishProvider;
    }

    public InsultProvider getInsultProvider() {
        return insultProvider;
    }

    public FriendProvider getFriendProvider() {
        return friendProvider;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public TimeUtils getTimeUtils() {
        return timeUtils;
    }

    public TabUtils getTabUtils() {
        return tabUtils;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public MessageChannelManager getMessageChannelManager() {
        return messageChannelManager;
    }

    public MaintenanceScheduler getMaintenanceScheduler() {
        return maintenanceScheduler;
    }

    public String getPrefix() {
        return "§8▎ §e§lProxy §8▰§7▰ §7";
    }

    public String getCloudPrefix() {
        return "§8▎ §a§lCloud §8▰§7▰ §7";
    }

    public String getPunishPrefix() {
        return "§8▎ §e§lPunish §8▰§7▰ §7";
    }

    public String getFriendPrefix() {
        return "§8▎ §4§lFreunde §8▰§7▰ §7";
    }

    public String getTeamChatPrefix() {
        return "§8▎ §b§lTeamChat §8▰§7▰ §7";
    }

    public String getBroadcastPrefix() {
        return "§8▎ §e§lBroadcast §8▰§7▰ §7";
    }

    public Component getUsage(String command) {
        return Component.text(this.getPrefix() + "§7Verwende §8● §e" + command);
    }

    public Component getUsage(String prefix, String command) {
        return Component.text(prefix + "§7Verwende §8● §e" + command);
    }

    public Component getNoPermissions() {
        return Component.text(this.getPrefix() + "§7Dazu hast du §ckeine §7Berechtigung§8...");
    }

    public Component getRequireLogin() {
        return Component.text(this.getPrefix() + "§7Du musst angemeldet sein um dies zu tun§8...");
    }

    public Component getOnlyPlayers() {
        return Component.text(this.getPrefix() + "§7Dieser Befehl ist §cnur §7für Spieler§8...");
    }

    public Logger getLogger() {
        return logger;
    }

}
