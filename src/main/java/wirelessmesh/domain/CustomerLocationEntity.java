package wirelessmesh.domain;

import com.google.protobuf.Empty;
import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.eventsourcedentity.*;

import wirelessmeshdomain.Wirelessmeshdomain.*;
import wirelessmesh.Wirelessmesh.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

 /**
  * A customer location entity.
  *
  * As an entity, I will be seeded with my current state upon loading and thereafter will completely serve the
  * backend needs for a particular device. There is no practical limit to how many of these entities you can have,
  * across the application cluster. Look at each instance of this entity as being roughly equivalent to a row in a
  * database, only each one is completely addressable and in memory.
  *
  * Event sourcing was selected in order to have complete traceability into the behavior of devices for the purposes
  * of security, analytics and simulation.
  */
 @EventSourcedEntity(entityType = "customer-location-entity")
 public class CustomerLocationEntity {

     /**
      * This section contains the private state variables necessary for this entity.
      */

     private final String customerLocationId;

     private boolean added = false;

     private boolean removed = false;

     private String accessToken = "";

     private String email = "";

     private String zipcode = "";

     private List<DeviceState> devices = new ArrayList<>();

     /**
      * Constructor.
      * @param customerLocationId The entity id will be the customerLocationId, the unique key for this entity.
      */
     public CustomerLocationEntity(@EntityId String customerLocationId) {
         this.customerLocationId = customerLocationId;
     }

     /**
      * This is the command handler for adding a customer location as defined in protobuf.
      * @param addCustomerLocationCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty addCustomerLocation(AddCustomerLocationCommand addCustomerLocationCommand, CommandContext ctx) {
         if (added) {
             ctx.fail("Customer location already added");
         }
         else if (!isAlphaNumeric(addCustomerLocationCommand.getCustomerLocationId())) {
             ctx.fail("Customer location id must be alphanumeric");
         }
         else if (!isAlphaNumeric(addCustomerLocationCommand.getAccessToken())) {
             ctx.fail("Access token must be alphanumeric");
         }
         else {
             CustomerLocationAdded event = CustomerLocationAdded.newBuilder()
                     .setCustomerLocationId(addCustomerLocationCommand.getCustomerLocationId())
                     .setAccessToken(addCustomerLocationCommand.getAccessToken())
                     .setEmail(addCustomerLocationCommand.getEmail())
                     .setZipcode(addCustomerLocationCommand.getZipcode())
                     .build();

             ctx.emit(event);
         }

         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for adding a customer location. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param customerLocationAdded the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void customerLocationAdded(CustomerLocationAdded customerLocationAdded) {
         this.added = true;
         this.removed = false;
         this.accessToken = customerLocationAdded.getAccessToken();
         this.email = customerLocationAdded.getEmail();
         this.zipcode = customerLocationAdded.getZipcode();
     }

     /**
      * This is the command handler for removing a customer location as defined in protobuf.
      * @param removeCustomerLocationCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty removeCustomerLocation(RemoveCustomerLocationCommand removeCustomerLocationCommand, CommandContext ctx) {
         if (!added) {
             ctx.fail("Customer location does not exist");
         }
         else if (removed) {
             ctx.fail("Customer location already removed");
         }
         else {
             CustomerLocationRemoved event = CustomerLocationRemoved.newBuilder()
                     .setCustomerLocationId(removeCustomerLocationCommand.getCustomerLocationId())
                     .setZipcode(zipcode)
                     .build();

             ctx.emit(event);
         }

         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for removing a customer location. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param customerLocationRemoved the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void customerLocationRemoved(CustomerLocationRemoved customerLocationRemoved) {
         this.removed = true;
         this.added = false;
         devices = new ArrayList<>();
     }

     /**
      * This is the command handler for activating a wirelessmesh device as defined in protobuf.
      * @param activateDeviceCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty activateDevice(ActivateDeviceCommand activateDeviceCommand, CommandContext ctx) {
         if (removed) {
             ctx.fail("customerLocation does not exist.");
         }
         else if (findDevice(activateDeviceCommand.getDeviceId()).isPresent()) {
             ctx.fail("Device already activated");
         }
         else if (!isAlphaNumeric(activateDeviceCommand.getDeviceId())) {
             ctx.fail("Device id must be alphanumeric");
         }
         else {
             DeviceActivated event = DeviceActivated.newBuilder()
                     .setDeviceId(activateDeviceCommand.getDeviceId())
                     .setCustomerLocationId(activateDeviceCommand.getCustomerLocationId())
                     .build();

             ctx.emit(event);
         }

         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for activating a wirelessmesh device. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param deviceActivated the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void deviceActivated(DeviceActivated deviceActivated) {
         devices.add(DeviceState.newBuilder()
                 .setDeviceId(deviceActivated.getDeviceId())
                 .setCustomerLocationId(customerLocationId)
                 .setActivated(true)
                 .setNightlightOn(false)
                 .build());
     }

     /**
      * This is the command handler for removing a wirelessmesh device as defined in protobuf.
      * @param removeDeviceCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty removeDevice(RemoveDeviceCommand removeDeviceCommand, CommandContext ctx) {
         if (!added || removed) {
             ctx.fail("customerLocation does not exist.");
         }
         else if (!findDevice(removeDeviceCommand.getDeviceId()).isPresent()) {
             ctx.fail("Device does not exist");
         }
         else {
             DeviceRemoved event = DeviceRemoved.newBuilder()
                     .setDeviceId(removeDeviceCommand.getDeviceId())
                     .setCustomerLocationId(removeDeviceCommand.getCustomerLocationId()).build();

             ctx.emit(event);
         }

         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for removing a wirelessmesh device. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param deviceRemoved the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void deviceRemoved(DeviceRemoved deviceRemoved) {
         devices = devices.stream().filter(d -> !d.getDeviceId().equals(deviceRemoved.getDeviceId()))
                 .collect(Collectors.toList());
     }

     /**
      * This is the command handler for assigning a wirelessmesh device to a room as defined in protobuf.
      * @param assignRoomCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty assignRoom(AssignRoomCommand assignRoomCommand, CommandContext ctx) {
         if (removed) {
             ctx.fail("customerLocation does not exist.");
         }
         else if (!findDevice(assignRoomCommand.getDeviceId()).isPresent()) {
             ctx.fail("Device does not exist");
         }
         else if (!isAlphaNumeric(assignRoomCommand.getRoom())) {
             ctx.fail("Room must be alphanumeric");
         }
         else {
             RoomAssigned event = RoomAssigned.newBuilder()
                     .setDeviceId(assignRoomCommand.getDeviceId())
                     .setCustomerLocationId(assignRoomCommand.getCustomerLocationId())
                     .setRoom(assignRoomCommand.getRoom()).build();

             ctx.emit(event);
         }

         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for assigning a wirelessmesh device to a room. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param roomAssigned the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void roomAssigned(RoomAssigned roomAssigned) {
         DeviceState old = findDevice(roomAssigned.getDeviceId()).get();
         DeviceState device = old.toBuilder()
                 .setRoom(roomAssigned.getRoom())
                 .build();

         replaceDevice(device);
     }

     /**
      * This is the command handler for toggling the wirelessmesh device nightlight as defined in protobuf.
      * @param toggleNightlightCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public Empty toggleNightlight(ToggleNightlightCommand toggleNightlightCommand, CommandContext ctx) throws IOException {
         if (removed) {
             ctx.fail("customerLocation does not exist.");
         }
         else {
             Optional<DeviceState> deviceMaybe = findDevice(toggleNightlightCommand.getDeviceId());

             if (!deviceMaybe.isPresent()) {
                 ctx.fail("Device does not exist");
             }
             else {
                 NightlightToggled event = NightlightToggled.newBuilder()
                         .setDeviceId(toggleNightlightCommand.getDeviceId())
                         .setCustomerLocationId(toggleNightlightCommand.getCustomerLocationId())
                         .setNightlightOn(!deviceMaybe.get().getNightlightOn())
                         .setAccessToken(accessToken)
                         .build();

                 ctx.emit(event);
             }
         }

         return Empty.getDefaultInstance();
     }

     /**
      * This is the event handler for toggling the wirelessmesh device nightlight. It is here we update current state due to
      * successful storage to the eventlog.
      *
      * @param nightlightToggled the event previously emitted in the command handler, now safely stored.
      */
     @EventHandler
     public void nightlightToggled(NightlightToggled nightlightToggled) {
         DeviceState old = findDevice(nightlightToggled.getDeviceId()).get();
         DeviceState device = old.toBuilder()
                 .setNightlightOn(nightlightToggled.getNightlightOn())
                 .build();

         replaceDevice(device);
     }

     /**
      * This is the command handler geting the current state of the devices as defined in protobuf.
      * @param getCustomerLocationCommand the command message from protobuf
      * @param ctx the application context
      * @return Empty (unused)
      */
     @CommandHandler
     public CustomerLocation getCustomerLocation(GetCustomerLocationCommand getCustomerLocationCommand, CommandContext ctx) {
         if (removed || !added) {
             ctx.fail("customerLocation does not exist.");
         }

         return CustomerLocation.newBuilder()
                 .setCustomerLocationId(customerLocationId)
                 .setAccessToken("[hidden]")
                 .setEmail(email)
                 .setZipcode(zipcode)
                 .setAdded(added)
                 .setRemoved(removed)
                 // Must convert domain to API, which are decoupled for the sake of independent evolution.
                 .addAllDevices(devices.stream()
                         .map(d ->
                             Device.newBuilder()
                                 .setCustomerLocationId(d.getCustomerLocationId())
                                 .setDeviceId(d.getDeviceId())
                                 .setActivated(d.getActivated())
                                 .setRoom(d.getRoom())
                                 .setNightlightOn(d.getNightlightOn())
                                 .build()
                         ).collect(Collectors.toList()))
                 .build();
     }

     /**
      * Return current state when called to snapshot.
      */
     @Snapshot
     public CustomerLocationState snapshot() {
         return CustomerLocationState.newBuilder()
                 .setCustomerLocationId(customerLocationId)
                 .setAccessToken("[hidden]")
                 .setEmail(email)
                 .setZipcode(zipcode)
                 .setAdded(added)
                 .setRemoved(removed)
                 .addAllDevices(devices).build();
     }

     /**
      * Set state to the most recent snapshot.
      */
     @SnapshotHandler
     public void handleSnapshot(CustomerLocationState state) {
         devices.clear();
         accessToken = state.getAccessToken();
         email = state.getEmail();
         zipcode = state.getZipcode();
         added = state.getAdded();
         removed = state.getRemoved();
         devices = state.getDevicesList();
     }

     /**
      * Helper function to find a device in the device collection.
      */
     private Optional<DeviceState> findDevice(String deviceId) {
         return devices.stream()
                 .filter(d -> d.getDeviceId().equals(deviceId))
                 .findFirst();
     }

     /**
      * Helper function to replace the state of a given device within the device collection.
      */
     private void replaceDevice(DeviceState device) {
         devices = Stream.concat(devices.stream()
                         .filter(d -> !d.getDeviceId().equals(device.getDeviceId())),
                 Stream.of(device))
                 .collect(Collectors.toList());
     }

     private boolean isAlphaNumeric(String id) {
         return id.matches("^[a-zA-Z0-9_-]*$");
     }
 }