package it.philipkrauss.proxysystem.commands.punishment;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.philipkrauss.proxysystem.provider.punishment.PunishProvider;
import it.philipkrauss.proxysystem.provider.punishment.PunishType;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.provider.punishment.objects.PunishReason;
import it.philipkrauss.proxysystem.provider.uniqueid.UUIDProvider;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;

public class KickCommand implements SimpleCommand {

    private static final String PERMISSION = "proxy.command.kick";

    private final ProxySystem instance;
    private final PunishProvider punishProvider;

    public KickCommand() {
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
        if (args.length < 2) {
            source.sendMessage(instance.getUsage(instance.getPunishPrefix(), "/kick <Spieler> <Grund>"));
            List<PunishReason> reasons = punishProvider.getReasons().stream().filter(punishReason -> punishReason.getId() > 50 && punishReason.getId() <= 100).toList();
            if (reasons.size() == 0) {
                source.sendMessage(Component.text(instance.getPunishPrefix() + "§8(§c✗§8) §c§lKeine Gründe verfügbar..."));
                return;
            }
            reasons.forEach(reason -> source.sendMessage(Component.text(
                    instance.getPunishPrefix() + "§8(§e" + (reason.getId() - 50) + "§8) §7" + reason.getReason())));
            return;
        }
        if (source instanceof Player player && ProxySystem.getInstance().getTeamManager().cantValidateLogin(player.getUniqueId())) {
            player.sendMessage(instance.getRequireLogin());
            return;
        }
        Optional<Player> playerOptional = instance.getProxyServer().getPlayer(UUIDProvider.getUniqueId(args[0]));
        if (playerOptional.isEmpty()) {
            source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Dieser Spieler ist derzeit §cnicht §7online§8."));
            return;
        }
        Player target = playerOptional.get();
        String uniqueId = target.getUniqueId().toString();

        StringBuilder stringBuilder = new StringBuilder(args[1]);
        for (int index = 2; index < args.length; index++) {
            stringBuilder.append(" ").append(args[index]);
        }
        PunishReason punishReason = new PunishReason.Builder().withReason(stringBuilder.toString()).withType(PunishType.KICK).build();
        try {
            int id = Integer.parseInt(args[1]) + 50;
            PunishReason reason = this.punishProvider.getReason(id);
            if (reason != null) {
                punishReason = reason;
            }
        } catch (Exception ignored) {
        }
        instance.getPunishProvider().executePunishment(uniqueId, punishReason, source);
        source.sendMessage(Component.text(instance.getPunishPrefix() + "§7Du hast §e" + target.getUsername() + " §7erfolgreich gekickt§8."));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return instance.getTabUtils().getPlayerNameCompletions(invocation, PERMISSION);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

}
