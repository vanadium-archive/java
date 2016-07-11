# Syncbase Exceptions

The methods in the `io.v.syncbase` package can throw the following exceptions:

*   java.lang.Exception
    *   *io.v.syncbase.exception.SyncbaseException*
        *   *io.v.syncbase.exception.**SyncbaseEndOfFileException***
        *   *io.v.syncbase.exception.**SyncbaseNoServersException***
        *   *io.v.syncbase.exception.**SyncbaseRaceException***
        *   *io.v.syncbase.exception.**SyncbaseRestartException***
        *   *io.v.syncbase.exception.**SyncbaseRetryBackoffException***
        *   *io.v.syncbase.exception.**SyncbaseRetryFetchException***
        *   *io.v.syncbase.exception.**SyncbaseSecurityException***
    *   java.lang.RuntimeException
        *   *io.v.syncbase.exception.**SyncbaseInternalException***
        *   java.lang.**IllegalArgumentException**
        *   java.lang.**IllegalStateException**
            *   java.util.concurrent.**CancellationException**
        *   java.util.**NoSuchElementException**
        *   java.lang.**UnsupportedOperationException**


## Exceptions Detected in Java

Some particular method in the `io.v.syncbase` package can throw these exceptions:

| Method Thrown From | Exception |
| ------------------ |-----------|
| `Syncgroup.getAccessList` or `Collection.getAccessList` (various inconsistencies in permissions) or<br/>  `Syncbase.login` (unsupported authentication provider) | SyncbaseSecurityException |
| `BatchDatabase.collection` (opts.withoutSyncgroup parameter is false),<br/> `Collection.getSyncgroup` (Collection is in a batch),<br/>  `Database.syncgroup` (no collections or collection without creator), or<br/>  `Id.decode` (invalid encoded ID) | IllegalArgumentException |

## Exceptions Caused by Underlying Vanadium Errors

Any method in the `io.v.syncbase` package can throw any of these exceptions:

| Vanadium Error | Exception |
| -------------- | --------- |
| EndOfFile | SyncbaseEndOfFileException |
| NoServers SyncgroupJoinFailed | SyncbaseNoServersException |
| BadVersion ConcurrentBatch UnknownBatch | SyncbaseRaceException |
| CorruptDatabase | SyncbaseRestartException |
| Timeout | SyncbaseRetryBackoffException |
| NoAccess NotTrusted NoExistOrNoAccess UnauthorizedCreateId InferAppBlessingFailed InferUserBlessingFailed InferDefaultPermsFailed | SyncbaseSecurityException |
| Unknown Internal BadState UnknownMethod UnknownSuffix BadProtocol BadExecStreamHeader | SyncbaseInternalException |
| BadArg InvalidName NotBoundToBatch ReadOnlyBatch | IllegalArgumentException |
| Exist NotInDevMode BlobNotCommitted InvalidPermissionsChange Aborted | IllegalStateException |
| Canceled | CancellationException |
| NoExist | NoSuchElementException |
| NotImplemented | UnsupportedOperationException |
