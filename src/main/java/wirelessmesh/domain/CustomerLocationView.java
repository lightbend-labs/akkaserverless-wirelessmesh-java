package wirelessmesh.domain;

import com.akkaserverless.javasdk.view.UpdateHandler;
import com.akkaserverless.javasdk.view.View;
import com.google.protobuf.Any;
import customerlocationview.Customerlocationview.*;
import wirelessmeshdomain.Wirelessmeshdomain;

@View
public class CustomerLocationView {

    @UpdateHandler
    public CustomerLocationDto updateCustomerLocation(Wirelessmeshdomain.CustomerLocationAdded event) {
        return CustomerLocationDto.newBuilder()
            .setCustomerLocationId(event.getCustomerLocationId())
            .setEmail(event.getEmail())
            .build();
    }

    @UpdateHandler
    public CustomerLocationDto catchOthers(Any Event) {
        // FIXME: we can't return an empty result currently, so keep upserting on the same fake details
        return CustomerLocationDto.newBuilder()
          .setCustomerLocationId("--ignored--")
          .setEmail("--ignored--")
          .build();
    }
}
