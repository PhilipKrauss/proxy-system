package it.philipkrauss.proxysystem.scheduler;

import com.google.common.collect.Lists;
import com.velocitypowered.api.scheduler.ScheduledTask;
import eu.thesimplecloud.api.CloudAPI;
import eu.thesimplecloud.api.player.ICloudPlayer;
import eu.thesimplecloud.api.player.SimpleCloudPlayer;
import eu.thesimplecloud.api.service.ServiceType;
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise;
import it.philipkrauss.proxysystem.ProxySystem;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MaintenanceScheduler {

    public static MaintenanceScheduler create() {
        return new MaintenanceScheduler();
    }

    private final ProxySystem instance;

    private ScheduledTask scheduledTask;
    private int duration;

    private final List<Integer> secondAlerts;
    private final List<Integer> minuteAlerts;
    private final List<Integer> hourAlerts;

    private MaintenanceScheduler() {
        this.secondAlerts = Lists.newArrayList(1, 2, 3, 4, 5, 10, 15, 30, 60, 90);
        this.minuteAlerts = Lists.newArrayList(120, 180, 300, 600, 900, 1800, 2700, 3600);
        this.hourAlerts = Lists.newArrayList(7200, 10800, 14400, 18000, 21600, 43200, 86400);
        this.instance = ProxySystem.getInstance();
    }

    public void schedule(int duration) {
        this.duration = duration;
        if(this.scheduledTask != null) this.scheduledTask.cancel();
        this.scheduledTask = instance.getProxyServer().getScheduler().buildTask(ProxySystem.getInstance(), () -> {
            if(this.duration == 0) {
                this.scheduledTask.cancel();
                CloudAPI.getInstance().getCloudServiceGroupManager().getAllCachedObjects().forEach(serviceGroup -> {
                    if (serviceGroup.getServiceType() == ServiceType.PROXY) {
                        serviceGroup.setMaintenance(true);
                        serviceGroup.update();
                    }
                });
                ICommunicationPromise<List<SimpleCloudPlayer>> promise = CloudAPI.getInstance().getCloudPlayerManager().getAllOnlinePlayers();
                promise.addListener(onlineFuture -> {
                    if (onlineFuture.isSuccess()) {
                        List<SimpleCloudPlayer> simpleCloudPlayers = promise.get();
                        simpleCloudPlayers.forEach(simpleCloudPlayer -> {
                            ICommunicationPromise<ICloudPlayer> cloudPlayerPromise = simpleCloudPlayer.getCloudPlayer();
                            cloudPlayerPromise.addListener(cloudPlayerFuture -> {
                                if (cloudPlayerFuture.isSuccess()) {
                                    ICloudPlayer cloudPlayer = cloudPlayerPromise.get();
                                    if (!cloudPlayer.hasPermissionSync("cloud.maintenance.join"))
                                        cloudPlayer.kick("""
                                                §8▎ §e§lFireplayz.net §8▰§7▰ §7Du wurdest vom §eNetzwerk §7gekickt§8.
                                                §8
                                                §7Grund §8● §eDas Netzwerk befindet sich in §cWartungsarbeiten""");
                                }
                            });
                        });
                    }
                });
                instance.getTeamManager().sendTeamMessage(Component.text(instance.getCloudPrefix() + "§7Die §eMaintenance §7wurde erfolgreich §aaktiviert §8/ §a✔"));
            }
            if(this.hourAlerts.contains(this.duration)) {
                this.notifyPlayers(Component.text(instance.getCloudPrefix() + "§7Die §eWartungsarbeiten §7starten in §e" + (this.duration / 3600) + " §7Stunden§8."));
            }
            if(this.minuteAlerts.contains(this.duration)) {
                this.notifyPlayers(Component.text(instance.getCloudPrefix() + "§7Die §eWartungsarbeiten §7starten in §e" + (this.duration / 60) + " §7Minuten§8."));
            }
            if(this.secondAlerts.contains(this.duration)) {
                if(this.duration != 1) {
                    this.notifyPlayers(Component.text(instance.getCloudPrefix() + "§7Die §eWartungsarbeiten §7starten in §e" + this.duration + " §7Sekunden§8."));
                } else {
                    this.notifyPlayers(Component.text(instance.getCloudPrefix() + "§7Die §eWartungsarbeiten §7starten in §eeiner §7Sekunde§8."));
                }
            }
            this.duration--;
        }).repeat(1, TimeUnit.SECONDS).schedule();
    }

    private void notifyPlayers(Component message) {
        ICommunicationPromise<List<SimpleCloudPlayer>> onlinePlayerPromise = CloudAPI.getInstance().getCloudPlayerManager().getAllOnlinePlayers();
        onlinePlayerPromise.addListener(onlineFuture -> {
            if (onlineFuture.isSuccess()) {
                List<SimpleCloudPlayer> simpleCloudPlayers = onlinePlayerPromise.get();
                simpleCloudPlayers.forEach(simpleCloudPlayer -> {
                    ICommunicationPromise<ICloudPlayer> cloudPlayerPromise = simpleCloudPlayer.getCloudPlayer();
                    cloudPlayerPromise.addListener(cloudPlayerFuture -> {
                        if (cloudPlayerFuture.isSuccess()) {
                            ICloudPlayer cloudPlayer = cloudPlayerPromise.get();
                            cloudPlayer.sendMessage(message);
                        }
                    });
                });
            }
        });
    }

}
