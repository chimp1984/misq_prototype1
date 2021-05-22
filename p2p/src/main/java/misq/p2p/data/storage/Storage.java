/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package misq.p2p.data.storage;


import misq.common.security.DigestUtil;
import misq.common.security.HybridEncryption;
import misq.common.security.Sealed;
import misq.common.security.SignatureUtil;
import misq.common.util.FileUtils;
import misq.p2p.NetworkData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;


public class Storage {
    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    // Class name is key
    final Map<String, ProtectedStorageService> storageServices = new ConcurrentHashMap<>();
    private final String storageDirPath;
    //  private final Set<StorageService> map = new ConcurrentHashMap<>();

    public Storage(String appDirPath) {
        storageDirPath = appDirPath + separator + "db" + separator + "network";
        try {
            FileUtils.makeDirs(new File(storageDirPath));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public AddProtectedDataRequest.Result addProtectedStorageEntry(AddProtectedDataRequest request) {
        return getService(request.getMetaData()).add(request);
    }

    public DataRequestResult removeProtectedStorageEntry(RemoveProtectedDataRequest request) {
        return getService(request.getMetaData()).remove(request);
    }

    public DataRequestResult refreshProtectedStorageEntry(RefreshProtectedDataRequest request) {
        return getService(request.getMetaData()).refresh(request);
    }

    public ProtectedStorageService getService(MetaData metaData) {
        String key = metaData.getFileName();
        if (!storageServices.containsKey(key)) {
            storageServices.put(key, new ProtectedStorageService(storageDirPath + separator, metaData));
        }
        return storageServices.get(key);
    }

    public AddProtectedDataRequest getAddProtectedDataRequest(NetworkData networkData, KeyPair keyPair)
            throws GeneralSecurityException {
        ProtectedStorageService service = getService(networkData.getMetaData());
        byte[] hashOfPublicKey = DigestUtil.sha256(keyPair.getPublic().getEncoded());
        ProtectedData protectedData = new ProtectedData(networkData, hashOfPublicKey);
        int newSequenceNumber = service.getSequenceNumber(networkData) + 1;
        ProtectedEntry entry = new ProtectedEntry(protectedData, newSequenceNumber, System.currentTimeMillis());
        byte[] serialized = entry.serialize();
        byte[] signature = SignatureUtil.sign(serialized, keyPair.getPrivate());
        return new AddProtectedDataRequest(entry, signature, keyPair.getPublic());
    }

    public RemoveProtectedDataRequest getRemoveProtectedDataRequest(NetworkData networkData, KeyPair keyPair)
            throws GeneralSecurityException {
        ProtectedStorageService service = getService(networkData.getMetaData());
        byte[] serialized = networkData.serialize();
        byte[] hash = DigestUtil.sha256(serialized);
        byte[] signature = SignatureUtil.sign(hash, keyPair.getPrivate());
        int newSequenceNumber = service.getSequenceNumber(networkData) + 1;
        return new RemoveProtectedDataRequest(networkData.getMetaData(), hash, keyPair.getPublic(), newSequenceNumber, signature);
    }

    public RefreshProtectedDataRequest getRefreshProtectedDataRequest(NetworkData networkData, KeyPair keyPair)
            throws GeneralSecurityException {
        ProtectedStorageService service = getService(networkData.getMetaData());
        byte[] serialized = networkData.serialize();
        byte[] hash = DigestUtil.sha256(serialized);
        byte[] signature = SignatureUtil.sign(hash, keyPair.getPrivate());
        int newSequenceNumber = service.getSequenceNumber(networkData) + 1;
        return new RefreshProtectedDataRequest(networkData.getMetaData(), hash, keyPair.getPublic(), newSequenceNumber, signature);
    }

    public boolean canAddMailboxMessage(SealedData sealedData) throws NoSuchAlgorithmException {
        ProtectedStorageService service = getService(sealedData.getMetaData());
        return service.getSequenceNumber(sealedData) < Integer.MAX_VALUE;
    }

    public SealedData getSealedData(MailboxMessage mailboxMessage,
                                    KeyPair senderKeyPair,
                                    PublicKey receiverPublicKey)
            throws GeneralSecurityException {
        Sealed sealed = HybridEncryption.encrypt(mailboxMessage.serialize(), receiverPublicKey, senderKeyPair);
        return new SealedData(sealed, mailboxMessage.getMetaData());
    }

    public AddMailboxDataRequest getAddMailboxDataRequest(SealedData sealedData,
                                                          KeyPair senderKeyPair,
                                                          PublicKey receiverPublicKey)
            throws GeneralSecurityException {
        ProtectedStorageService service = getService(sealedData.getMetaData());
        PublicKey senderPublicKey = senderKeyPair.getPublic();
        byte[] hashOfSendersPublicKey = DigestUtil.sha256(senderPublicKey.getEncoded());
        byte[] hashOfReceiversPublicKey = DigestUtil.sha256(receiverPublicKey.getEncoded());
        MailboxData mailboxData = new MailboxData(sealedData, hashOfSendersPublicKey, hashOfReceiversPublicKey);
        int sequenceNumberFromMap = service.getSequenceNumber(sealedData);
        if (sequenceNumberFromMap == Integer.MAX_VALUE) {
            throw new IllegalStateException("Existing sequenceNumber must be smaller than Integer.MAX_VALUE.");
        }
        int newSequenceNumber = sequenceNumberFromMap + 1;
        MailboxEntry entry = new MailboxEntry(mailboxData, newSequenceNumber, receiverPublicKey);
        byte[] serialized = entry.serialize();
        byte[] signature = SignatureUtil.sign(serialized, senderKeyPair.getPrivate());
        return new AddMailboxDataRequest(entry, signature, senderPublicKey);
    }

    public RemoveMailboxDataRequest getRemoveMailboxDataRequest(SealedData sealedData,
                                                                KeyPair receiverKeyPair)
            throws GeneralSecurityException {
        byte[] hash = DigestUtil.sha256(sealedData.serialize());
        byte[] signature = SignatureUtil.sign(hash, receiverKeyPair.getPrivate());
        int newSequenceNumber = Integer.MAX_VALUE; // Use max value for sequence number so that no other addData call is permitted.
        return new RemoveMailboxDataRequest(sealedData.getMetaData(), hash, receiverKeyPair.getPublic(), newSequenceNumber, signature);
    }

   /* public Inventory getInventory(ProtectedDataFilter dataFilter) {
        return getService(dataFilter.getDataType()).getInventory(dataFilter);
    }*/

/*
    public MapValue put(MapKey mapKey, MapValue mapValue) {
        return map.put(mapKey, mapValue);
    }

    public MapValue remove(MapKey mapKey) {
        return map.remove(mapKey);
    }

    public MapValue getInventory(MapKey mapKey) {
        return map.get(mapKey);
    }



    public Collection<MapValue> getAll() {
        return map.values();
    }


    public CompletableFuture<Inventory> add(Inventory inventory) {
        return CompletableFuture.supplyAsync(() -> {
            //inventory.getCollection().forEach(this::add);
            return inventory;
        });
    }*/

    public void shutdown() {

    }
}
