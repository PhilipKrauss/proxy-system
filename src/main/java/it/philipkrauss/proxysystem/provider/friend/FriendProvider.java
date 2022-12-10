package it.philipkrauss.proxysystem.provider.friend;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.velocitypowered.api.proxy.Player;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.database.DatabaseAdapter;
import it.philipkrauss.proxysystem.provider.friend.objects.FriendSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FriendProvider {

    public static FriendProvider create() {
        return new FriendProvider();
    }

    private final ProxySystem instance;
    private final DatabaseAdapter databaseAdapter;

    private final Map<String, String> replies = Maps.newHashMap();
    private final Map<String, FriendSettings> friendSettings = Maps.newHashMap();

    private FriendProvider() {
        this.instance = ProxySystem.getInstance();
        this.databaseAdapter = instance.getDatabaseAdapter();
        this.setupTables();
    }

    private void setupTables() {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS friends (UUID VARCHAR(36), FRIENDUUID VARCHAR(36))");
            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS friends_requests (UUID VARCHAR(36), FRIENDUUID VARCHAR(36))");
            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS friends_settings (UUID VARCHAR(36), MESSAGES VARCHAR(100), NOTIFY VARCHAR(100), REQUESTS VARCHAR(100), JUMPTO VARCHAR(100))");
            connection.close();
            return;
        }catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void createFriendRequest(String uniqueId, String friend) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO friends_requests (UUID, FRIENDUUID) VALUES (?, ?)");
            preparedStatement.setString(1, uniqueId);
            preparedStatement.setString(2, friend);
            preparedStatement.executeUpdate();
            connection.close();
            return;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void deleteFriendRequest(String uniqueId, String friend) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM friends_requests WHERE UUID = ? AND FRIENDUUID = ?");
            preparedStatement.setString(1, uniqueId);
            preparedStatement.setString(2, friend);
            preparedStatement.executeUpdate();
            connection.close();
            return;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public boolean existsFriendRequest(String uniqueId, String friend) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM friends_requests WHERE UUID = ? AND FRIENDUUID = ?");
            preparedStatement.setString(1, uniqueId);
            preparedStatement.setString(2, friend);
            connection.close();
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public List<String> getFriendRequests(String uniqueId) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM friends_requests WHERE FRIENDUUID = ?");
            preparedStatement.setString(1, uniqueId);
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            List<String> friends = Lists.newArrayList();
            while (resultSet.next()) {
                friends.add(resultSet.getString("UUID"));
            }
            return friends;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public void addFriend(String uniqueId, String friend) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO friends (UUID, FRIENDUUID) VALUES (?, ?)");
            preparedStatement.setString(1, uniqueId);
            preparedStatement.setString(2, friend);
            preparedStatement.executeUpdate();
            connection.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO friends (UUID, FRIENDUUID) VALUES (?, ?)");
            preparedStatement.setString(1, friend);
            preparedStatement.setString(2, uniqueId);
            preparedStatement.executeUpdate();
            connection.close();
            return;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void deleteFriend(String uniqueId, String friend) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM friends WHERE UUID = ? AND FRIENDUUID = ?");
            preparedStatement.setString(1, uniqueId);
            preparedStatement.setString(2, friend);
            preparedStatement.executeUpdate();
            connection.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM friends WHERE UUID = ? AND FRIENDUUID = ?");
            preparedStatement.setString(1, friend);
            preparedStatement.setString(2, uniqueId);
            preparedStatement.executeUpdate();
            connection.close();
            return;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public boolean areFriends(String uniqueId, String friend) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM friends WHERE UUID = ? AND FRIENDUUID = ?");
            preparedStatement.setString(1, uniqueId);
            preparedStatement.setString(2, friend);
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            if (resultSet.next()) return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM friends WHERE UUID = ? AND FRIENDUUID = ?");
            preparedStatement.setString(1, friend);
            preparedStatement.setString(2, uniqueId);
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            if (resultSet.next()) return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public List<String> getFriends(String uniqueId) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM friends WHERE UUID = ?");
            preparedStatement.setString(1, uniqueId);
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            List<String> friends = Lists.newArrayList();
            while (resultSet.next()) {
                friends.add(resultSet.getString("FRIENDUUID"));
            }
            return friends;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public List<Player> getOnlineFriends(String uniqueId) {
        List<Player> onlineFriends = Lists.newArrayList();
        this.getFriends(uniqueId).forEach(friendUUID -> {
            Optional<Player> playerOptional = instance.getProxyServer().getPlayer(UUID.fromString(friendUUID));
            playerOptional.ifPresent(onlineFriends::add);
        });
        return onlineFriends;
    }

    private FriendSettings createFriendSettings(String uniqueId) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO friends_settings (UUID, MESSAGES, NOTIFY, REQUESTS, JUMPTO) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setString(1, uniqueId);
            preparedStatement.setString(2, String.valueOf(true));
            preparedStatement.setString(3, String.valueOf(true));
            preparedStatement.setString(4, String.valueOf(true));
            preparedStatement.setString(5, String.valueOf(true));
            preparedStatement.executeUpdate();
            connection.close();
            FriendSettings friendSettings = FriendSettings.create(uniqueId);
            friendSettings.setReceiveMessages(true);
            friendSettings.setReceiveRequests(true);
            friendSettings.setReceiveNotifications(true);
            friendSettings.setJumpAfter(true);
            this.friendSettings.put(uniqueId, friendSettings);
            return friendSettings;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private boolean hasFriendSettings(String uniqueId) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM friends_settings WHERE UUID = ?");
            preparedStatement.setString(1, uniqueId);
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            if (resultSet.next()) return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public FriendSettings getFriendSettings(String uniqueId) {
        if (this.friendSettings.containsKey(uniqueId)) return this.friendSettings.get(uniqueId);
        if (!hasFriendSettings(uniqueId)) {
            return createFriendSettings(uniqueId);
        }
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM friends_settings WHERE UUID = ?");
            preparedStatement.setString(1, uniqueId);
            ResultSet resultSet = preparedStatement.executeQuery();
            connection.close();
            if (resultSet.next()) {
                FriendSettings friendSettings = FriendSettings.create(uniqueId);
                friendSettings.setReceiveMessages(Boolean.parseBoolean(resultSet.getString("MESSAGES")));
                friendSettings.setReceiveRequests(Boolean.parseBoolean(resultSet.getString("REQUESTS")));
                friendSettings.setReceiveNotifications(Boolean.parseBoolean(resultSet.getString("NOTIFY")));
                friendSettings.setJumpAfter(Boolean.parseBoolean(resultSet.getString("JUMPTO")));
                this.friendSettings.put(uniqueId, friendSettings);
                return friendSettings;
            }
            
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public void updateFriendSettings(FriendSettings friendSettings) {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE friends_settings SET MESSAGES = ?, REQUESTS = ?, NOTIFY = ?, JUMPTO = ? WHERE UUID = ?");
            preparedStatement.setString(1, String.valueOf(friendSettings.canReceiveMessages()));
            preparedStatement.setString(2, String.valueOf(friendSettings.canReceiveRequests()));
            preparedStatement.setString(3, String.valueOf(friendSettings.canReceiveNotifications()));
            preparedStatement.setString(4, String.valueOf(friendSettings.canJumpAfter()));
            preparedStatement.setString(5, friendSettings.getUniqueId());
            preparedStatement.executeUpdate();
            connection.close();
            this.friendSettings.put(friendSettings.getUniqueId(), friendSettings);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void uncacheFriendSettings(String uniqueId) {
        this.friendSettings.remove(uniqueId);
    }

    public void uncacheReply(String uniqueId) {
        this.replies.remove(uniqueId);
        this.replies.forEach((key, value) -> {
            if (this.replies.get(key).equals(uniqueId)) this.replies.remove(key);
        });
    }

    public void setReplier(String uniqueId, String friend) {
        this.replies.put(uniqueId, friend);
    }

    public String getReplier(String uniqueId) {
        return this.replies.getOrDefault(uniqueId, null);
    }

}
