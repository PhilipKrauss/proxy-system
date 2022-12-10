package it.philipkrauss.proxysystem.provider.uniqueid;

import com.google.common.collect.Maps;
import it.philipkrauss.proxysystem.database.DatabaseAdapter;
import it.philipkrauss.proxysystem.provider.uniqueid.mojang.UUIDFetcher;
import it.philipkrauss.proxysystem.ProxySystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class UUIDProvider {

    public static UUIDProvider create() {
        return new UUIDProvider();
    }

    private static final Map<UUID, String> CACHE = Maps.newHashMap();
    private static UUIDProvider instance;

    private final DatabaseAdapter databaseAdapter;

    private UUIDProvider() {
        instance = this;
        this.databaseAdapter = ProxySystem.getInstance().getDatabaseAdapter();
        this.setupTables();
    }

    private void setupTables() {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS players (UUID VARCHAR(36), NAME VARCHAR(36))").executeUpdate();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM players");
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            while (resultSet.next()) CACHE.put(UUID.fromString(resultSet.getString("UUID")), resultSet.getString("NAME"));
            ProxySystem.getInstance().getLogger().info(String.format("[UUIDProvider] Successfully loaded %s profiles into cache...", CACHE.size()));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static UUID getUniqueId(String username) {
        for (UUID uniqueId : CACHE.keySet()) {
            if (CACHE.get(uniqueId).equals(username)) return uniqueId;
        }
        UUID uniqueId = UUIDFetcher.getUUID(username);
        if(uniqueId == null) return null;
        String name = UUIDFetcher.getName(uniqueId);
        if(name != null) getInstance().cachePlayer(uniqueId, name);
        return uniqueId;
    }

    public static String getName(UUID uniqueId) {
        String cachedName = CACHE.get(uniqueId);
        if(cachedName != null) return cachedName;
        String name = UUIDFetcher.getName(uniqueId);
        if(name != null) getInstance().cachePlayer(uniqueId, name);
        return name;
    }

    public void cachePlayer(UUID uniqueId, String username) {
        if(existsProfile(uniqueId)) return;
        insertProfile(uniqueId, username);
    }

    private void insertProfile(UUID uniqueId, String username) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO players (UUID, NAME) VALUES (?, ?)");
            preparedStatement.setString(1, uniqueId.toString());
            preparedStatement.setString(2, username);
            preparedStatement.executeUpdate();
            connection.close();
            CACHE.put(uniqueId, username);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private boolean existsProfile(UUID uniqueId) {
        if(CACHE.containsKey(uniqueId)) return true;
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM players WHERE UUID = ?");
            preparedStatement.setString(1, uniqueId.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            if(resultSet.next()) return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public static void updateProfile(UUID uniqueId, String username) {
        if(CACHE.containsKey(uniqueId)) CACHE.put(uniqueId, username);
        try(Connection connection = getInstance().databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE players SET NAME = ? WHERE UUID = ?");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, uniqueId.toString());
            preparedStatement.executeUpdate();
        }catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private static UUIDProvider getInstance() {
        return instance;
    }

}
