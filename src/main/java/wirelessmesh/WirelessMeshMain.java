package wirelessmesh;

import com.akkaserverless.javasdk.AkkaServerless;

import customerlocationview.Customerlocationview;
import publishing.Publishing;
import devicecontrol.Devicecontrol;
import wirelessmesh.domain.CustomerLocationEntity;
import wirelessmesh.domain.CustomerLocationView;
import wirelessmeshdomain.Wirelessmeshdomain;

/**
 * This is the entry point into this user function.
 */
public class WirelessMeshMain {
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
                           new ToggleNightlightAction(),
                           Devicecontrol.getDescriptor().findServiceByName("DeviceControlService"));
    //                    .registerAction(
//                            new PublishingAction(),
//                            Publishing.getDescriptor().findServiceByName("PublishingService"))

    public static void main(String... args) throws Exception {
        wirelessMeshService.start().toCompletableFuture().get();
    }
}
