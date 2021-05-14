# Akka Serverless - Wireless Mesh Example App

A Java-based example app for [Akka Serverless](https://developer.lightbend.com/docs/akka-serverless/)

Features include:

* Customer locations with wireless mesh devices

## What is this example?

To help you get started with Akka Serverless, we've built some example apps that showcase the capabilities of the platform. This example application mimics a company that uses Akka Serverless to keep track of the wireless mesh devices their customers have installed and the devices connected to the meshes.

In this example app you can interact with the devices, assign them to different rooms in the house, and turn them on or off. To make this example even more interactive, you can add an actual nightlight and switch the lights on or off.

We used the following akkaserverless capabilities: event sourced entity (wirelessmeshdomain.CustomerLocationEntity),
using an Action to publish to google pubsub (wirelessmesh.PublishingAction and publishing.proto) and using a View (wirelessmesh.CustomerLocationView and customerlocationview.proto) and finally
an Action to connect to the Lifx bulb (wirelessmesh.ToggleNightlightAction and devicecontrol.proto).

## Prerequisites

To build and deploy this example application, you'll need to have:

* An [Akka Serverless account](https://docs.cloudstate.com/getting-started/lightbend-account.html)
* Java 11 or higher installed
* Maven 3.x or higher installed
* The Docker CLI installed
* A [service account](https://cloud.google.com/docs/authentication/production) that can connect to your Google Cloud Pubsub

## Build, Deploy, and Test

### Prepare your Google Could Pubsub

#### Step 1: create a Google Could Pubsub topic
Create a Pub/Sub topic called 'wirelessmesh' under your own Google Cloud account.
For example,
```
# login to google cloud
gcloud auth login

# list all projects
gcloud projects list

# set to the Google Cloud Pub/Sub project
gcloud config set project  <PROJECT_ID>

# create project "wirelessmesh"
gcloud pubsub topics create wirelessmesh
```

After it, you can validate if the "wirelessmesh" topic exists by command
```
gcloud pubsub topics list
```
Or check it in Google Cloud Console https://console.cloud.google.com/cloudpubsub/topic/list

#### Step 2: set up service account key file
To connect Akka Serverless to your Google Cloud Pub/Sub you must authenticate using a service account. To create your [service account](https://cloud.google.com/docs/authentication/production#cloud-console). After creating your service account, you need to download the service account key as a JSON file.

Now use the [akkasls](https://developer.lightbend.com/docs/akka-serverless/getting-started/set-up-development-env.html) command-line tool to give Akka Serverless access to your broker:

```
akkasls project set broker --broker-service gcp-pubsub --gcp-key-file testing-pubsub-310212-fec7d0612927.json
```

### LIFX integration for toggling nightlight

If you have an LIFX bulb and would like it to stand in for a wirelessmesh device and have it light on/off when you toggle the device nightlight, you simply have to:
* Have an operational bulb
* When you create your customer location, be sure to set the access token to the authorizaton token you requested with LIFX.
* When you activate the device in this app, make sure it has the same device id as your bulb.
* More information [here][https://api.developer.lifx.com]

### Build your container

To build your own container, run `mvn -Dnamespace=<namespace> clean install`, substituting `<namespace>` for a docker registry namespace you have write access to (for example your dockerhub username).

This command will create a new Docker image.

The result of the command should be:

```bash
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:31 min
[INFO] Finished at: 2021-01-20T16:20:29-08:00
[INFO] ------------------------------------------------------------------------
```

### Deploy your container

To deploy the container as a service in Akka Serverless, you'll need to:

1. Push the container to a container registry: `docker push -t <registry url>/<registry namespace>/akkaserverless-wirelessmesh-java:latest`
1. Deploy the service in Akka Serverless: `akkasls svc deploy wirelessmesh <registry url>/<registry username>/akkaserverless-wirelessmesh-java:latest`

_The above command will deploy your container to your default project with the name `wirelessmesh`. If you want to have a different name, you can change that._

### The Google Cloud Pub/Sub code explain

In the proto file,
- `eventing.in.topic`: to define which topic to subscribe for reading data. (Code will create a description for the topic)
- `eventing.out.topic`: to define which topic to write data.

In this example, there is no `eventing.in.topic`. So we don't generate any view from Google Cloud Pub/Sub.
In the other hand, we define `eventing.out.topic` in proto file `src/main/proto/publishing.proto`. The following is the code snap, which means whenever we trigger the action `PublishCustomerLocationAdded`, it writes data to topic "wirelessmesh".


```
service PublishingService {
    rpc PublishCustomerLocationAdded(wirelessmeshdomain.CustomerLocationAdded) returns (wirelessmeshdomain.CustomerLocationAdded) {
        option (akkaserverless.method).eventing = {
            in: {
                event_log: "customer-location-entity";
            }
            out: {
                topic: "wirelessmesh";
            }
        };
    }
```

### How to check if data is written to Google Cloud Pub/Sub
Since we don't have any eventing.in.topic in this app, our view is not generate from Google Cloud Pub/Sub. Even there is no data written to Google Cloud Pub/Sub, the service test is still passed since all view are not from Google Cloud Pub/Sub.

To check if data is written to Google Cloud Pub/Sub, we need to manually create a "subscription" associated with the topic "wirelessmesh", and see if we can pull data from it. We assume in the real scenario, other apps consume data from the subscription.

To do it, we need to
```
# create a subscription for the topic
gcloud pubsub subscriptions create wirelessmesh-feed --topic=wirelessmesh

# list subscriptions associate with the topic
gcloud pubsub topics list-subscriptions wirelessmesh
```

### Testing your service with restful api

To test using Postman(or curl).
* First install Postman, [found here](https://www.postman.com)
* Assuming you have deployed to akkaserverless and exposed your service to 'winter-mountain-2372.us-east1.apps.akkaserverless.com'...
* **Test 0:** Make sure if there is any data in wirelessmesh-feed
```
gcloud pubsub subscriptions pull wirelessmesh-feed --auto-ack
```
Check if there is any data in the feed.
* **Test 1:** Create a Postman POST request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/add-customer-location' with the json body '{"customer_location_id": "my-first-location", "access_token": "my life access token if applicable in alphanumeric", "email": "some@email.com", "zipcode": "11111"}'

Or using Curl (NOTE: Assume access_token is "abcd1234", you can set it to any alphanumeric):
```
# setup env variable for exposed route 
export AS_HOST=winter-mountain-2372.us-east1.apps.akkaserverless.com
```
and run
```
curl -X POST -H "Content-Type: application/json"  --data '{"customer_location_id": "my-first-location", "access_token": "abcd1234", "email": "some@email.com", "zipcode": "11111"}' https://${AS_HOST}/wirelessmesh/add-customer-location
```
* You should see a response of '200(OK) {}', this will be the response of any POST
* Ideally it should trigger PublishCustomerLocationAdded action, which write data to topic "wirelessmesh", we can check it by command
```
gcloud pubsub subscriptions pull wirelessmesh-feed --auto-ack
```
NOTE: sometimes you need to wait for like 30-60 seconds for data ready.
If it does not data, just retry several times.

It will show a data like
```
┌────────────────────────────┬──────────────────┬──────────────────────────────────────────────────┬────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│            DATA            │    MESSAGE_ID    │                    ATTRIBUTES                    │                                                                                               ACK_ID                                                                                               │
├────────────────────────────┼──────────────────┼──────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                            │ 2257681511091503 │ ce-datacontenttype=application/protobuf          │ XXXXXXXXXXXXXXXXXXXXXXXXXXXKEUWAwgUBXx9cF5bdV1YdGhRDRlyfWByY1wTAgMWW3pWURsHXXXXXXXXXXXXXXXXXXXXXXXXXXXeWhsEaVlcdi-XXXXXXXXXXXXXXXXXXXXXXXXXXXtvZiM9XxJLLD5-MSpFQV5AEkw6H0RJUytDCypYEU4EISE-XXXXXX │
│ my-3rd-locatioabcd1234 │                  │ ce-id=8aac4009-03f8-4562-9564-00e67fbxxxxx       │                                                                                                                                                                                                    │
│                            │                  │ ce-source=publishing.PublishingService           │                                                                                                                                                                                                    │
│                            │                  │ ce-specversion=1.0                               │                                                                                                                                                                                                    │
│                            │                  │ ce-subject=my-3rd-location                       │                                                                                                                                                                                                    │
│                            │                  │ ce-time=2021-05-12T10:49:04.013137Z              │                                                                                                                                                                                                    │
│                            │                  │ ce-type=wirelessmeshdomain.CustomerLocationAdded │                                                                                                                                                                                                    │
└────────────────────────────┴──────────────────┴──────────────────────────────────────────────────┴────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

* **Test 2:** You can now create a GET request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/get-customer-location?customer_location_id=my-first-location'

Or using Curl:
```
curl https://${AS_HOST}/wirelessmesh/get-customer-location?customer_location_id=my-first-location
```
* You should see a json response containing your customer location and no devices.
* **Test 3:** Create a POST request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/activate-device' with the body '{"customer_location_id": "my-first-location", "device_id": "my-first-device"}'

Or using Curl:
```
curl -X POST -H "Content-Type: application/json"  --data '{"customer_location_id": "my-first-location", "device_id": "my-first-device"}' https://${AS_HOST}/wirelessmesh/activate-device
```
* **Test 4:** Create a POST request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/assign-room' with the body '{"customer_location_id": "my-first-location", "device_id": "my-first-device", "room": "office"}'

Or using Curl:
```
curl -X POST -H "Content-Type: application/json"  --data '{"customer_location_id": "my-first-location", "device_id": "my-first-device", "room": "office"}' https://${AS_HOST}/wirelessmesh/assign-room
```
* **Test 5:** Create a POST requset to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/toggle-nightlight' with the body '{"customer_location_id": "my-first-location", "device_id": "my-first-device"}'

Or using Curl:
```
curl -X POST -H "Content-Type: application/json"  --data '{"customer_location_id": "my-first-location", "device_id": "my-first-device"}' https://${AS_HOST}/wirelessmesh/toggle-nightlight
```
* **Test 6:** Rerun your get-customer-location request

Or using Curl:
```
curl https://${AS_HOST}/wirelessmesh/get-customer-location?customer_location_id=my-first-location
```
* You should see a json response with your customer location and a collection of your single device with the room assigned and the nightlight on
* **Test 7:** Create a POST request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/remove-device' with the body '{"customer_location_id": "my-first-location", "device_id": "my-first-device"}'

Or using Curl:
```
curl -X POST -H "Content-Type: application/json"  --data '{"customer_location_id": "my-first-location", "device_id": "my-first-device"}' https://${AS_HOST}/wirelessmesh/remove-device
```
* You should see a json response no longer containing any devices
* **Test 8:** Create a POST request to 'https://winter-mountain-2372.us-east1.apps.akkaserverless.com/wirelessmesh/remove-customer-location' with the body '{"customer_location_id": "my-first-location"}'

Or using Curl:
```
curl -X POST -H "Content-Type: application/json"  --data '{"customer_location_id": "my-first-location"}' https://${AS_HOST}/wirelessmesh/remove-customer-location
```
* **Test 9:** Rerun your get-customer-location request and you will see a server error since it no longer exists.

Or using Curl:
```
curl https://${AS_HOST}/wirelessmesh/get-customer-location?customer_location_id=my-first-location
```

## Tips to debug Google Cloud Pub/Sub

We can debug Google Cloud Pub/Sub with GUI https://console.cloud.google.com/cloudpubsub

or by using the following CLI commands
```
# list topics
gcloud pubsub topics list

# list subscriptions
gcloud pubsub subscriptions list

# list subscriptions associate with the topic
gcloud pubsub topics list-subscriptions <topic>

# create a topic
gcloud pubsub topics create <topic>

# create a subscription for the topic
gcloud pubsub subscriptions create <subscription> --topic=<topic>

# pull data from subscriptions
gcloud pubsub subscriptions pull <subscription> --auto-ack
```

## Contributing

We welcome all contributions! [Pull requests](https://github.com/lightbend-labs/akkaserverless-wirelessmesh-java/pulls) are the preferred way to share your contributions. For major changes, please open [an issue](https://github.com/lightbend-labs/akkaserverless-wirelessmesh-java/issues) first to discuss what you would like to change.

## Support

This project is provided on an as-is basis and is not covered by the Lightbend Support policy.

## License

See the [LICENSE](./LICENSE).
