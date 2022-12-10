package it.philipkrauss.proxysystem.provider.friend.objects;

public class FriendSettings {

    public static FriendSettings create(String uniqueId) {
        return new FriendSettings(uniqueId);
    }

    private final String uniqueId;
    private boolean receiveMessages;
    private boolean receiveRequests;
    private boolean receiveNotifications;
    private boolean jumpAfter;

    private FriendSettings(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public boolean canReceiveMessages() {
        return this.receiveMessages;
    }

    public void setReceiveMessages(boolean receiveMessages) {
        this.receiveMessages = receiveMessages;
    }

    public boolean canReceiveRequests() {
        return this.receiveRequests;
    }

    public void setReceiveRequests(boolean receiveRequests) {
        this.receiveRequests = receiveRequests;
    }

    public boolean canReceiveNotifications() {
        return this.receiveNotifications;
    }

    public void setReceiveNotifications(boolean receiveNotifications) {
        this.receiveNotifications = receiveNotifications;
    }

    public boolean canJumpAfter() {
        return this.jumpAfter;
    }

    public void setJumpAfter(boolean jumpAfter) {
        this.jumpAfter = jumpAfter;
    }

}
