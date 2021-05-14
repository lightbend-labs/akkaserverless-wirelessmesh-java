package wirelessmesh;

import com.akkaserverless.javasdk.AkkaServerless;

import customerlocationview.Customerlocationview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import publishing.Publishing;
import devicecontrol.Devicecontrol;
import wirelessmesh.domain.CustomerLocationEntity;
import wirelessmesh.domain.CustomerLocationView;
import wirelessmeshdomain.Wirelessmeshdomain;

/**
 * This is the entry point into this user function.
 */
public class WirelessMeshMain {

    private final static Logger LOG = LoggerFactory.getLogger(WirelessMeshMain.class);

    public static AkkaServerless wirelessMeshService =
            new AkkaServerless()
                    .registerEventSourcedEntity(
                            CustomerLocationEntity.class,
                            Wirelessmesh.getDescriptor().findServiceByName("WirelessMeshService"),
                            Wirelessmeshdomain.getDescriptor()
                    )
                    .registerView(
                            CustomerLocationView.class,
                            Customerlocationview.getDescriptor()
                                    .findServiceByName("CustomerLocationByEmailService"),
                            "customer_locations",
                            Wirelessmeshdomain.getDescriptor(),
                            Customerlocationview.getDescriptor())
                    .registerAction(
                            ToggleNightlightAction.class,
                            Devicecontrol.getDescriptor().findServiceByName("DeviceControlService"),
                            Wirelessmeshdomain.getDescriptor())
                    .registerAction(
                            PublishingAction.class,
                            Publishing.getDescriptor().findServiceByName("PublishingService"),
                            Wirelessmeshdomain.getDescriptor()
                    );

    public static void main(String... args) throws Exception {
        LOG.info("started");
        wirelessMeshService.start().toCompletableFuture().get();
    }
}
