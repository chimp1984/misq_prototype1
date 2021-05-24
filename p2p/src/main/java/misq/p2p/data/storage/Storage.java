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
import misq.p2p.data.NetworkData;
import misq.p2p.data.filter.ProtectedDataFilter;
import misq.p2p.data.inventory.Inventory;
import misq.p2p.data.storage.auth.*;
import misq.p2p.data.storage.auth.mailbox.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;

public class Storage {
    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    // Class name is key
    final Map<String, AuthenticatedDataStore> protectedStorageServices = new ConcurrentHashMap<>();
    private final String storageDirPath;

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

    public Result addProtectedStorageEntry(AddRequest request)
            throws NoSuchAlgorithmException {
        return getService(request.getMetaData()).add(request);
    }

    public Result removeProtectedStorageEntry(RemoveRequest request) {
        return getService(request.getMetaData()).remove(request);
    }

    public Result refreshProtectedStorageEntry(RefreshRequest request) {
        return getService(request.getMetaData()).refresh(request);
    }

    public AddRequest getAddProtectedDataRequest(AuthenticatedPayload authenticatedPayload, KeyPair keyPair)
            throws GeneralSecurityException {
        AuthenticatedDataStore service = getService(authenticatedPayload.getMetaData());
        byte[] hash = DigestUtil.sha256(authenticatedPayload.serialize());
        byte[] hashOfPublicKey = DigestUtil.sha256(keyPair.getPublic().getEncoded());
        int newSequenceNumber = service.getSequenceNumber(hash) + 1;
        AuthenticatedData entry = new AuthenticatedData(authenticatedPayload, newSequenceNumber, hashOfPublicKey, System.currentTimeMillis());

        byte[] serialized = entry.serialize();
        byte[] signature = SignatureUtil.sign(serialized, keyPair.getPrivate());

        return new AddRequest(entry, signature, keyPair.getPublic());
    }

    public RemoveRequest getRemoveProtectedDataRequest(NetworkData networkData, KeyPair keyPair)
            throws GeneralSecurityException {
        AuthenticatedDataStore service = getService(networkData.getMetaData());
        byte[] hash = DigestUtil.sha256(networkData.serialize());
        byte[] signature = SignatureUtil.sign(hash, keyPair.getPrivate());
        int newSequenceNumber = service.getSequenceNumber(hash) + 1;
        return new RemoveRequest(networkData.getMetaData(), hash, keyPair.getPublic(), newSequenceNumber, signature);
    }

    public RefreshRequest getRefreshProtectedDataRequest(NetworkData networkData, KeyPair keyPair)
            throws GeneralSecurityException {
        AuthenticatedDataStore service = getService(networkData.getMetaData());
        byte[] hash = DigestUtil.sha256(networkData.serialize());
        byte[] signature = SignatureUtil.sign(hash, keyPair.getPrivate());
        int newSequenceNumber = service.getSequenceNumber(hash) + 1;
        return new RefreshRequest(networkData.getMetaData(), hash, keyPair.getPublic(), newSequenceNumber, signature);
    }

    public boolean canAddMailboxMessage(MailboxPayload mailboxPayload) throws NoSuchAlgorithmException {
        AuthenticatedDataStore service = getService(mailboxPayload.getMetaData());
        byte[] hash = DigestUtil.sha256(mailboxPayload.serialize());
        return service.getSequenceNumber(hash) < Integer.MAX_VALUE;
    }

    public MailboxPayload getSealedData(MailboxMessage mailboxMessage,
                                        KeyPair senderKeyPair,
                                        PublicKey receiverPublicKey)
            throws GeneralSecurityException {
        Sealed sealed = HybridEncryption.encrypt(mailboxMessage.serialize(), receiverPublicKey, senderKeyPair);
        return new MailboxPayload(sealed, mailboxMessage.getMetaData());
    }

    public AddMailboxRequest getAddMailboxDataRequest(MailboxPayload mailboxPayload,
                                                      KeyPair senderKeyPair,
                                                      PublicKey receiverPublicKey)
            throws GeneralSecurityException {
        AuthenticatedDataStore service = getService(mailboxPayload.getMetaData());

        PublicKey senderPublicKey = senderKeyPair.getPublic();


        byte[] hash = DigestUtil.sha256(mailboxPayload.serialize());
        int sequenceNumberFromMap = service.getSequenceNumber(hash);
        if (sequenceNumberFromMap == Integer.MAX_VALUE) {
            throw new IllegalStateException("Item was already removed in service map as sequenceNumber is marked with Integer.MAX_VALUE");
        }
        int newSequenceNumber = sequenceNumberFromMap + 1;
        byte[] hashOfSendersPublicKey = DigestUtil.sha256(senderPublicKey.getEncoded());
        byte[] hashOfReceiversPublicKey = DigestUtil.sha256(receiverPublicKey.getEncoded());
        Mailbox entry = new Mailbox(mailboxPayload, newSequenceNumber, hashOfSendersPublicKey,
                hashOfReceiversPublicKey, receiverPublicKey);
        byte[] serialized = entry.serialize();
        byte[] signature = SignatureUtil.sign(serialized, senderKeyPair.getPrivate());
        return new AddMailboxRequest(entry, signature, senderPublicKey);
    }

    public RemoveMailboxRequest getRemoveMailboxDataRequest(MailboxPayload mailboxPayload, KeyPair receiverKeyPair)
            throws GeneralSecurityException {
        byte[] hash = DigestUtil.sha256(mailboxPayload.serialize());
        byte[] signature = SignatureUtil.sign(hash, receiverKeyPair.getPrivate());
        int newSequenceNumber = Integer.MAX_VALUE; // Use max value for sequence number so that no other addData call is permitted.
        return new RemoveMailboxRequest(mailboxPayload.getMetaData(), hash, receiverKeyPair.getPublic(), newSequenceNumber, signature);
    }

    public Optional<Inventory> getInventory(ProtectedDataFilter dataFilter) {
        if (protectedStorageServices.containsKey(dataFilter.getDataType())) {
            return Optional.of(protectedStorageServices.get(dataFilter.getDataType()).getInventory(dataFilter));
        } else {
            return Optional.empty();
        }
    }

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

    public AuthenticatedDataStore getService(MetaData metaData) {
        String key = metaData.getFileName();
        if (!protectedStorageServices.containsKey(key)) {
            protectedStorageServices.put(key, new AuthenticatedDataStore(storageDirPath, metaData));
        }
        return protectedStorageServices.get(key);
    }
}
