package wirelessmesh.domain;

import com.akkaserverless.javasdk.view.UpdateHandler;
import com.akkaserverless.javasdk.view.View;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
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
    public Empty catchOthers(Any Event) {
        return Empty.getDefaultInstance();
    }
}
