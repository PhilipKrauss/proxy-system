package it.philipkrauss.proxysystem.provider.punishment;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.lang.Nullable;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.database.DatabaseAdapter;
import it.philipkrauss.proxysystem.player.NetworkPlayer;
import it.philipkrauss.proxysystem.provider.punishment.objects.PunishReason;
import it.philipkrauss.proxysystem.provider.punishment.objects.Punishment;
import net.kyori.adventure.text.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PunishProvider {

    public static final String PERMANENT = "§4Permanent";

    public static PunishProvider create() {
        return new PunishProvider();
    }

    private final DatabaseAdapter databaseAdapter;
    private final ProxySystem instance;
    private final List<PunishReason> reasons = Lists.newArrayList();

    private final Map<String, Punishment> banPunishmentCache = Maps.newHashMap();
    private final Map<String, Punishment> mutePunishmentCache = Maps.newHashMap();

    private PunishProvider() {
        this.instance = ProxySystem.getInstance();
        this.databaseAdapter = instance.getDatabaseAdapter();
        this.setupTables();
        this.loadReasons();
    }

    private void setupTables() {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS punishment_reasons (ID INT, TYPE INT, REASON VARCHAR(64), DURATION VARCHAR(32), DURATION_TEXT VARCHAR(32), EXEMPTED_DURATION VARCHAR(32))").executeUpdate();
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS punishment_bans (TARGET VARCHAR(36), ADDRESS VARCHAR(16), REASON INT, PUNISHER VARCHAR(36), END VARCHAR(32))").executeUpdate();
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS punishment_mutes (TARGET VARCHAR(36), ADDRESS VARCHAR(16), REASON INT, PUNISHER VARCHAR(36), END VARCHAR(32))").executeUpdate();
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS punishment_logs (TARGET VARCHAR(36), REASON INT, TYPE VARCHAR(32), PUNISHER VARCHAR(36), END VARCHAR(32), TIME VARCHAR(32))").executeUpdate();
            connection.close();
            this.instance.getLogger().info("[PunishProvider] Successfully created tables...");
            return;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    // METHODS

    public int createPunishment(String target, String address, PunishReason reason, CommandSource source) {
        long end = (reason.getDuration() == -1 ? -1 : System.currentTimeMillis() + reason.getDuration());
        String QUERY = "INSERT INTO " + (reason.getType() == PunishType.BAN ? "punishment_bans" : "punishment_mutes") + " (TARGET, ADDRESS, REASON, PUNISHER, END) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setString(1, target);
            preparedStatement.setString(2, address);
            preparedStatement.setInt(3, reason.getId());
            preparedStatement.setString(4, getPunisher(source));
            preparedStatement.setString(5, String.valueOf(end));
            preparedStatement.executeUpdate();
            connection.close();
            instance.getLogger().info(String.format("[PunishManager] Created new punishment (target: %s | address: %s | type: %s)", target, address, reason.getType().getDisplayName()));
            this.logPunishment(target, reason, reason.getType(), getPunisher(source), end);
            return 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return -1;
    }

    public int exemptPunishment(String target, PunishReason reason, CommandSource source) {
        if (reason.getExemptedDuration() == -1) return 0;
        long end = System.currentTimeMillis() + reason.getExemptedDuration();
        String QUERY = "UPDATE " + (reason.getType() == PunishType.BAN ? "punishment_bans" : "punishment_mutes") + " SET PUNISHER = ?, END = ? WHERE TARGET = ?";
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setString(1, getPunisher(source));
            preparedStatement.setString(2, String.valueOf(end));
            preparedStatement.setString(3, target);
            preparedStatement.executeUpdate();
            connection.close();
            instance.getLogger().info(String.format("[PunishManager] Updated punishment (target: %s)", target));
            this.logPunishment(target, reason, PunishType.EXEMPTION, getPunisher(source), end);
            if (reason.getType() == PunishType.BAN) this.banPunishmentCache.remove(target);
            if (reason.getType() == PunishType.MUTE) this.mutePunishmentCache.remove(target);
            ProxySystem.getInstance().getMessageChannelManager().sendPunishUpdate(target);
            return 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return -1;
        }
    }

    public int deletePunishment(String target, PunishType type) {
        String QUERY = "DELETE FROM " + (type == PunishType.BAN ? "punishment_bans" : "punishment_mutes") + " WHERE TARGET = ?";
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setString(1, target);
            preparedStatement.executeUpdate();
            connection.close();
            instance.getLogger().info(String.format("[PunishManager] Deleted punishment (target: %s | type: %s)", target, type.getDisplayName()));
            Punishment punishment = this.getPunishment(target, type);
            this.logPunishment(target, punishment.getReason(), PunishType.UNPUNISH, punishment.getPunisher(), System.currentTimeMillis());
            if (type == PunishType.BAN) this.banPunishmentCache.remove(target);
            if (type == PunishType.MUTE) this.mutePunishmentCache.remove(target);
            ProxySystem.getInstance().getMessageChannelManager().sendPunishUpdate(target);
            return 1;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return -1;
    }

    public boolean isPunished(String target, PunishType type) {
        if (type == PunishType.BAN && this.banPunishmentCache.containsKey(target)) return true;
        if (type == PunishType.MUTE && this.mutePunishmentCache.containsKey(target)) return true;
        String QUERY = "SELECT * FROM " + (type == PunishType.BAN ? "punishment_bans" : "punishment_mutes") + " WHERE TARGET = ? OR ADDRESS = ?";
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setString(1, target);
            preparedStatement.setString(2, target);
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            if (resultSet.next()) {
                this.getPunishment(target, type);
                return true;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public void executePunishment(String target, PunishReason reason, CommandSource source) {
        PunishType type = reason.getType();
        ICommunicationPromise<ICloudPlayer> promise = CloudAPI.getInstance().getCloudPlayerManager().getCloudPlayer(UUID.fromString(target));
        promise.addListener(future -> {
            if (future.isSuccess()) {
                ICloudPlayer cloudPlayer = promise.get();
                if (type == PunishType.BAN) {
                    cloudPlayer.kick(this.getBanMessage(target));
                    this.sendTeamNotification(target, reason, source);
                } else if (type == PunishType.MUTE) {
                    for (Component component : this.getMuteMessage(target, reason)) {
                        cloudPlayer.sendMessage(component);
                    }
                    this.sendTeamNotification(target, reason, source);
                } else {
                    cloudPlayer.kick("\n§8▎ §e§lFireplayz §8▰§7▰ §7Kick\n\n§8§m------------------------------\n\n§7Du wurdest vom §aNetzwerk §7gekickt§8.\n§7Grund §8● §a"
                            + reason.getReason() + "\n\n§7Wir bitten um dein Verständnis\n§7Sonstige Informationen können im Discord beantragt werden " +
                            "§8● §e§ldiscord.deinserver.net\n\n§8§m------------------------------");
                    this.sendTeamNotification(target, reason, source);
                }
            } else {
                this.sendTeamNotification(target, reason, source);
            }
        });
    }

    private void logPunishment(String target, PunishReason reason, PunishType type, String punisher, long end) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            String QUERY = "INSERT INTO punishment_logs (TARGET, REASON, TYPE, PUNISHER, END, TIME) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setString(1, target);
            preparedStatement.setInt(2, reason.getId());
            preparedStatement.setString(3, type.getDisplayName());
            preparedStatement.setString(4, punisher);
            preparedStatement.setString(5, String.valueOf(end));
            preparedStatement.setString(6, String.valueOf(System.currentTimeMillis()));
            preparedStatement.executeUpdate();
            connection.close();
            instance.getLogger().info(String.format("[PunishProvider] Created log for %s (action: %s)", target, type.getDisplayName()));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public Punishment getPunishment(String target, PunishType type) {
        if (type == PunishType.BAN && this.banPunishmentCache.containsKey(target))
            return this.banPunishmentCache.get(target);
        if (type == PunishType.MUTE && this.mutePunishmentCache.containsKey(target))
            return this.mutePunishmentCache.get(target);
        String QUERY = "SELECT * FROM " + (type == PunishType.BAN ? "punishment_bans" : "punishment_mutes") + " WHERE TARGET = ? OR ADDRESS = ?";
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setString(1, target);
            preparedStatement.setString(2, target);
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            if (resultSet.next()) {
                Punishment punishment = new Punishment.Builder().withTarget(target).withAddress(resultSet.getString("ADDRESS"))
                        .withReason(this.getReason(resultSet.getInt("REASON"))).withPunisher(resultSet.getString("PUNISHER"))
                        .withEnd(Long.parseLong(resultSet.getString("END"))).build();
                if (type == PunishType.BAN) this.banPunishmentCache.put(target, punishment);
                if (type == PunishType.MUTE) this.mutePunishmentCache.put(target, punishment);
                return punishment;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public void resetCache(String target) {
        this.banPunishmentCache.remove(target);
        this.mutePunishmentCache.remove(target);
    }

    // REASONS

    public PunishReason getReason(int id) {
        return this.reasons.stream().filter(reason -> reason.getId() == id).findFirst().orElse(null);
    }

    public List<PunishReason> getReasons() {
        return reasons;
    }

    private void loadReasons() {
        String QUERY = "SELECT * FROM punishment_reasons";
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            while (resultSet.next()) {
                this.reasons.add(new PunishReason.Builder().withId(resultSet.getInt("ID")).withType(PunishType.getTypeById(resultSet.getInt("TYPE")))
                        .withReason(resultSet.getString("REASON")).withDuration(Long.parseLong(resultSet.getString("DURATION")))
                        .withDurationText(resultSet.getString("DURATION_TEXT"))
                        .withExemptedDuration(Long.parseLong(resultSet.getString("EXEMPTED_DURATION"))).build());
            }
            instance.getLogger().info(String.format("[PunishManager] Loaded %s punishment-reasons from database", this.reasons.size()));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    // MESSAGES

    public String getBanMessage(String target) {
        Punishment punishment = this.getPunishment(target, PunishType.BAN);
        if(punishment == null) return "§8▎ §e§lFireplayz §8▰§7▰ §7Bann";
        String reason = punishment.getReason().getReason();
        return "§8▎ §e§lFireplayz §8▰§7▰ §7Bann\n\n§8§m------------------------------\n\n§7Du wurdest vom §eNetzwerk §7gebannt§8.\n\n" +
                "§7Grund §8● §e" + reason + "\n§7Verbleibende Zeit §8● §e" + getRemainingTime(punishment) + "\n\n§8§m------------------------------\n\n" +
                "§7Du wurdest unrecht gebannt?\n§7Stelle im §eDiscord §7einen Entbannungsantrag §8● §e§ldiscord.deinserver.net\n\n§8§m------------------------------";
    }

    public List<Component> getMuteMessage(String target, PunishReason reason) {
        List<Component> components = Lists.newArrayList();
        components.add(Component.text(instance.getPunishPrefix() + "§8§m-----------§r§8[ §e§lPunishSystem §8]§8§m-----------"));
        components.add(Component.text(instance.getPunishPrefix() + "§7Du wurdest vom §eNetzwerk §7gemuted§8."));
        components.add(Component.text(instance.getPunishPrefix() + "§7Grund §8● §e" + reason.getReason()));
        components.add(Component.text(instance.getPunishPrefix() + "§7Verbleibende Zeit §8● §e" + getEndDate(target, reason.getType())));
        components.add(Component.text(instance.getPunishPrefix() + ""));
        components.add(Component.text(instance.getPunishPrefix() + "§7Du wurdest zu unrecht gemuted§8?"));
        components.add(Component.text(instance.getPunishPrefix() + "§7Stelle im §eDiscord §7einen Entbannungsantrag"));
        components.add(Component.text(instance.getPunishPrefix() + "§8» §ediscord.deinserver.net"));
        components.add(Component.text(instance.getPunishPrefix() + "§8§m-----------§r§8[ §e§lPunishSystem §8]§8§m-----------"));
        return components;
    }

    public void sendTeamNotification(String target, @Nullable PunishReason punishReason, CommandSource source) {
        UUID uniqueId = UUID.fromString(target);
        NetworkPlayer networkPlayer = instance.getNetworkPlayerRegistry().getNetworkPlayer(uniqueId);
        if(networkPlayer == null) networkPlayer = NetworkPlayer.create(uniqueId);
        String targetName = networkPlayer.getColoredName();
        String punisher;
        if (source instanceof Player player) {
            punisher = instance.getNetworkPlayerRegistry().getNetworkPlayer(player.getUniqueId()).getColoredName();
        } else {
            punisher = "§e" + this.getPunisher(source);
        }
        if (punishReason == null) {
            instance.getTeamManager().sendTeamMessage(Component.text(instance.getPunishPrefix() + "§8§m-----------§r§8[ §e§lPunishSystem §8]§8§m-----------"));
            instance.getTeamManager().sendTeamMessage(Component.text(instance.getPunishPrefix() + targetName + " §7wurde von " + punisher + " §7entsperrt§8."));
            instance.getTeamManager().sendTeamMessage(Component.text(instance.getPunishPrefix() + "§8§m-----------§r§8[ §e§lPunishSystem §8]§8§m-----------"));
            return;
        }
        instance.getTeamManager().sendTeamMessage(Component.text(instance.getPunishPrefix() + "§8§m-----------§r§8[ §e§lPunishSystem §8]§8§m-----------"));
        instance.getTeamManager().sendTeamMessage(Component.text(instance.getPunishPrefix() + targetName + " §7wurde von " + punisher + " §7bestraft§8."));
        instance.getTeamManager().sendTeamMessage(Component.text(instance.getPunishPrefix() + "§7Typ §8● §e" + punishReason.getType().getDisplayName() +
                " §8▰§7▰ Grund §8● §e" + punishReason.getReason()));
        instance.getTeamManager().sendTeamMessage(Component.text(instance.getPunishPrefix() + "§8§m-----------§r§8[ §e§lPunishSystem §8]§8§m-----------"));
    }

    public String getRemainingTime(Punishment punishment) {
        long punishmentEnd = punishment.getEnd();
        if (punishmentEnd == -1) return PERMANENT;
        long totalSeconds = (punishmentEnd - System.currentTimeMillis()) / 1000;
        int days = (int) (totalSeconds / 86400);
        int hours = (int) (totalSeconds % 86400) / 3600;
        int minutes = (int) (totalSeconds % 3600) / 60;
        int seconds = (int) (totalSeconds % 60);
        StringBuilder stringBuilder = new StringBuilder();
        if (days != 0) stringBuilder.append(days).append(days == 1 ? " Tag " : " Tage ");
        if (hours != 0) stringBuilder.append(hours).append(hours == 1 ? " Stunde " : " Stunden ");
        if (minutes != 0) stringBuilder.append(minutes).append(minutes == 1 ? " Minute " : " Minuten ");
        if (days == 0) stringBuilder.append(seconds).append(seconds == 1 ? " Sekunde " : " Sekunden ");
        return stringBuilder.toString();
    }

    public String getEndDate(String target, PunishType type) {
        Punishment punishment = this.getPunishment(target, type);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(punishment.getEnd());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        return simpleDateFormat.format(calendar.getTime());
    }

    // UTILS

    private String getPunisher(CommandSource source) {
        if (source == null) return "System";
        if (source instanceof Player player) return player.getUniqueId().toString();
        return "Konsole";
    }

}
