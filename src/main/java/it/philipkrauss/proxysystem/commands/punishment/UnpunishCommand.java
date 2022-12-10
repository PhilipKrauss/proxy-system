package it.philipkrauss.proxysystem.commands.punishment;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.punishment.PunishProvider;
import it.philipkrauss.proxysystem.provider.punishment.PunishType;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;
import it.philipkrauss.proxysystem.provider.punishment.objects.Punishment;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UnpunishCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.unpunish";
    private static final String ADMIN_PERMISSION = "proxy.command.unpunish.admin";

    private final ProxySystem instance;
    private final PunishProvider punishProvider;

    public UnpunishCommand() {
        this.instance = ProxySystem.getInstance();
        this.punishProvider = instance.getPunishProvider();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission(PERMISSION)) {
            source.sendMessage(instance.getNoPermissions());
            return;
        }
        String[] args = invocation.arguments();
        if (args.length != 1) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Verwende §8● §e/unpunish <Spieler>"));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        UUID uniqueId = UUIDProvider.getUniqueId(args[0]);
        if (uniqueId == null) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Dieser Spieler existiert §cnicht§8..."));
            return;
        }
        boolean banned = this.punishProvider.isPunished(uniqueId.toString(), PunishType.BAN);
        boolean muted = this.punishProvider.isPunished(uniqueId.toString(), PunishType.MUTE);
        if (!banned && !muted) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Dieser Spieler ist §cnicht §7bestraft§8..."));
            return;
        }
        Punishment punishment = this.punishProvider.getPunishment(uniqueId.toString(), (banned ? PunishType.BAN : PunishType.MUTE));
        if (punishment.getEnd() == -1 && !source.hasPermission(ADMIN_PERMISSION)) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Du kannst diese Strafe §cnicht §7entfernen§8..."));
            return;
        }
        int deletePunishment = this.punishProvider.deletePunishment(uniqueId.toString(), punishment.getReason().getType());
        if (deletePunishment != 1) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Die Strafe konnte §cnicht §7entfernt werden§8..."));
            return;
        }
        source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Du hast §e" + args[0] + " §7erfolgreich entsperrt§8."));
        this.punishProvider.sendTeamNotification(uniqueId.toString(), null, source);
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
