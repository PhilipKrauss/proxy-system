package it.philipkrauss.proxysystem.utils;

import com.google.common.collect.Lists;
import com.velocitypowered.api.command.SimpleCommand;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.ICloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.api.service.ICloudService;
import eu.thesimplecloud.api.service.ServiceType;
import eu.thesimplecloud.api.servicegroup.ICloudServiceGroup;

import java.util.Collections;
import java.util.List;

public class TabUtils {

    public static TabUtils create() {
        return new TabUtils();
    }

    private final ICloudAPI cloudAPI;

    private TabUtils() {
        this.cloudAPI = CloudAPI.getInstance();
    }

    public List<String> getPlayerNameCompletions(SimpleCommand.Invocation invocation, String permission) {
        if(!invocation.source().hasPermission(permission)) return Collections.emptyList();
        if (invocation.arguments().length == 0) {
            List<ICloudPlayer> cloudPlayers = CloudAPI.getInstance().getCloudPlayerManager().getAllCachedObjects();
            return cloudPlayers.stream().map(ICloudPlayer::getName).toList();
        }
        if(invocation.arguments().length == 1) {
            List<ICloudPlayer> cloudPlayers = CloudAPI.getInstance().getCloudPlayerManager().getAllCachedObjects();
            List<String> completions = cloudPlayers.stream().map(ICloudPlayer::getName).toList();
            return completions.stream().filter(name -> name.regionMatches(true, 0, invocation.arguments()[0], 0, invocation.arguments()[0].length())).toList();
        }
        return Collections.emptyList();
    }

    public List<String> getServiceNameCompletions(SimpleCommand.Invocation invocation, String permission) {
        if(!invocation.source().hasPermission(permission)) return Collections.emptyList();
        if (invocation.arguments().length == 0) {
            List<ICloudService> cloudServices = Lists.newArrayList();
            cloudAPI.getWrapperManager().getAllCachedObjects().forEach(iWrapperInfo ->
                    cloudAPI.getCloudServiceManager().getServicesRunningOnWrapper(iWrapperInfo.getName()).stream().filter(iCloudService ->
                            iCloudService.getServiceGroup().getServiceType() != ServiceType.PROXY).forEach(cloudServices::add));
            return cloudServices.stream().map(ICloudService::getName).toList();
        }
        if(invocation.arguments().length == 1) {
            List<ICloudService> cloudServices = Lists.newArrayList();
            cloudAPI.getWrapperManager().getAllCachedObjects().forEach(iWrapperInfo ->
                    cloudAPI.getCloudServiceManager().getServicesRunningOnWrapper(iWrapperInfo.getName()).stream().filter(iCloudService ->
                            iCloudService.getServiceGroup().getServiceType() != ServiceType.PROXY).forEach(cloudServices::add));
            List<String> completions = cloudServices.stream().map(ICloudService::getName).toList();
            return completions.stream().filter(name -> name.regionMatches(true, 0, invocation.arguments()[0], 0, invocation.arguments()[0].length())).toList();
        }
        return Collections.emptyList();
    }

    public List<String> getServiceNameCompletions() {
        List<String> completions = Lists.newArrayList();
        cloudAPI.getWrapperManager().getAllCachedObjects().forEach(iWrapperInfo ->
                cloudAPI.getCloudServiceManager().getServicesRunningOnWrapper(iWrapperInfo.getName()).stream().filter(iCloudService ->
                        iCloudService.getServiceGroup().getServiceType() != ServiceType.PROXY).forEach(iCloudService ->
                        completions.add(iCloudService.getName())));
        return completions;
    }

    public List<String> getServiceGroupNameCompletions() {
        return cloudAPI.getCloudServiceGroupManager().getAllCachedObjects().stream().map(ICloudServiceGroup::getName).toList();
    }

}
