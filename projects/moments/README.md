# moments - a cloudless photo sharing app

This app is an example of running, advertising and scanning for
multiple Vanadium services on Android.

A user runs the app, using it to take photos, building up a list of
photos to share.  The UX presents each photo with an _advertise_
switch.  The app itself has an overall _scan_ switch.  When scanning,
the app sees and displays photos advertised by other nearby devices.

A photo's advertisement travels over wifi and/or BLE, and contains
minimal information about the photo, e.g. a caption, some hint of who
is advertising the photo, and the address of a _service_ to use to get
the actual photo via RPC.  A photo, even a thumbnail, is too large to
send as an advertisement payload, hence the need for the service and
RPC.

Every photo is served by its own service.  This facilitates creation
of multiple scan targets to exercise discovery code.  One can turn
scanning on/off, and turn advertising on/off on a per photo basis to
see the impact on the UX of the devices involved.

## Build and install

Connect developer enabled device via USB.

Then, for example:
```
cd ~/vanadium/release/java/projects/moments
./gradlew installDebug
```

Click on __Moments__ to launch.
