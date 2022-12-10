package it.philipkrauss.proxysystem.message;

import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.message.IMessageChannel;
import eu.thesimplecloud.api.service.ICloudService;

import java.util.List;

public class MessageChannelManager {

    public static MessageChannelManager create() {
        return new MessageChannelManager();
    }

    private final ICloudAPI cloudAPI;

    private final IMessageChannel<String> punishUpdateChannel;

    private MessageChannelManager() {
        this.cloudAPI = CloudAPI.getInstance();
        this.punishUpdateChannel = cloudAPI.getMessageChannelManager().getMessageChannelByName("punishUpdate");
    }

    public void sendPunishUpdate(String target) {
        List<ICloudService> cloudServices = cloudAPI.getCloudServiceManager().getAllCachedObjects();
        punishUpdateChannel.sendMessage(target, cloudServices);
    }

}
