# Sync user preferences across devices

This application illustrates how to implement user preferences with Syncbase.
The user logs into a trusted 3rd party -- we use Google auth, but it could
be any cloud or nearby service that uses identityd to provide a blessing.

Then when devices are offline and in physical proximity or connected via
any network path, the user preferences will sync.

# Building and running

From the command line, you can build and install
on the Android emulator or a device with the following command:

    ./gradlew installDebug
