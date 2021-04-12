package wirelessmesh;

import com.akkaserverless.javasdk.AkkaServerless;

import wirelessmesh.domain.CustomerLocationEntity;
import wirelessmesh.domain.PubSubPublisherService;
import wirelessmeshservice.Wirelessmeshservice;
import wirelessmeshdomain.Wirelessmeshdomain;

/**
 * This is the entry point into this user function.
 */
public class WirelessMeshMain {
    public static AkkaServerless wirelessMeshService =
            new AkkaServerless()
                .registerEventSourcedEntity(
                    CustomerLocationEntity.class,
                    Wirelessmeshservice.getDescriptor().findServiceByName("WirelessMeshService"),
                    Wirelessmeshdomain.getDescriptor())
                .registerAction(
                    new PubSubPublisherService(),
                    Wirelessmeshdomain.getDescriptor().findServiceByName("PubSubPublisherService"),
                    Wirelessmeshdomain.getDescriptor()
                )
            ;

    public static void main(String... args) throws Exception {
        wirelessMeshService.start().toCompletableFuture().get();
    }
}
