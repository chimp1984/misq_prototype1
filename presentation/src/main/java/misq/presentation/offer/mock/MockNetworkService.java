/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package misq.presentation.offer.mock;

import lombok.Getter;
import misq.account.FiatTransferType;
import misq.account.TransferType;
import misq.finance.Asset;
import misq.finance.contract.AssetTransfer;
import misq.finance.offer.*;
import misq.finance.swap.SwapProtocolType;
import misq.finance.swap.offer.SwapOffer;
import misq.p2p.Address;
import misq.p2p.NetworkId;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class MockNetworkService implements NetworkService {
    public interface Listener {
        void onOfferAdded(Offer offer);

        void onOfferRemoved(Offer offer);
    }

    private final Set<MockNetworkService.Listener> listeners = new CopyOnWriteArraySet<>();
    @Getter
    private final Map<String, Offer> data = new HashMap<>();

    public MockNetworkService() {
        for (int i = 0; i < 30; i++) {
            Offer offer = getRandomOffer();
            data.put(offer.getId(), offer);
        }

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int toggle = new Random().nextInt(2);
                if (toggle == 0) {
                    int iter = new Random().nextInt(3);
                    for (int i = 0; i < iter; i++) {
                        Offer offer = getRandomOffer();
                        data.put(offer.getId(), offer);
                        listeners.forEach(l -> l.onOfferAdded(offer));
                    }
                } else {
                    int iter2 = new Random().nextInt(2);
                    for (int i = 0; i < iter2; i++) {
                        if (!data.isEmpty()) {
                            Offer offerToRemove = getOfferToRemove();
                            data.remove(offerToRemove.getId());
                            listeners.forEach(l -> l.onOfferRemoved(offerToRemove));
                        }
                    }
                }
            }
        }, 0, 500);
    }

    @Override
    public void addListener(MockNetworkService.Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(MockNetworkService.Listener listener) {
        listeners.remove(listener);
    }

    private Offer getRandomOffer() {
        Asset askAsset;
        Asset bidAsset;
        Optional<Double> marketBasedPrice = Optional.empty();
        Optional<Double> minAmountAsPercentage = Optional.empty();
        String baseCurrency;
        //  int rand = new Random().nextInt(3);
        int rand = new Random().nextInt(2);
        // rand=1;
        if (rand == 0) {
            long usdAmount = new Random().nextInt(100000) + 500000000; // precision 4 / 50k usd
            long btcAmount = new Random().nextInt(10000000) + 100000000; // precision 8 / 1 btc
            usdAmount = 500000000; // precision 4 / 50k usd
            btcAmount = 100000000; // precision 8 / 1 btc
            askAsset = getRandomAsset("USD", usdAmount);
            bidAsset = getRandomAsset("BTC", btcAmount);
            baseCurrency = "BTC";
            marketBasedPrice = Optional.of(0.01);
            minAmountAsPercentage = Optional.of(0.1);
        } else if (rand == 1) {
            long usdAmount = 620000000; // precision 4 / 50k usd
            long btcAmount = 121000000; // precision 8 / 1 btc
            askAsset = getRandomAsset("BTC", btcAmount);
            bidAsset = getRandomAsset("USD", usdAmount);
            baseCurrency = "BTC";
            marketBasedPrice = Optional.of(0.03);
            minAmountAsPercentage = Optional.of(0.3);
        } else if (rand == 2) {
            long usdAmount = new Random().nextInt(100000) + 1200000; // precision 4 / 120 usd
            long eurAmount = new Random().nextInt(100000) + 1000000; // precision 4 / 100 eur
            askAsset = getRandomAsset("USD", usdAmount);
            bidAsset = getRandomAsset("EUR", eurAmount);
            baseCurrency = "USD";

        } else {
            // ignore for now as fiat/altcoins calculations not supported and only one market price
            long btcAmount = new Random().nextInt(10000000) + 100000000; // precision 8 / 1 btc //0.007144 BTC
            long xmrAmount = new Random().nextInt(10000000) + 13800000000L; // precision 8 / 138 xmr
            bidAsset = getRandomAsset("BTC", btcAmount);
            askAsset = getRandomAsset("XMR", xmrAmount);
            baseCurrency = "XMR";
            marketBasedPrice = Optional.of(-0.02);
            minAmountAsPercentage = Optional.of(0.8);
        }
        List<SwapProtocolType> protocolTypes = new ArrayList<>();
        rand = new Random().nextInt(3);
        for (int i = 0; i < rand; i++) {
            SwapProtocolType swapProtocolType = SwapProtocolType.values()[new Random().nextInt(SwapProtocolType.values().length)];
            protocolTypes.add(swapProtocolType);
        }
        NetworkId makerNetworkId = new NetworkId(Address.localHost(1000 + new Random().nextInt(1000)), null, "default");

        Optional<DisputeResolutionOptions> disputeResolutionOptions = Optional.empty();
        Optional<FeeOptions> feeOptions = Optional.empty();
        ReputationProof accountCreationDateProof = new AccountCreationDateProof("hashOfAccount", "otsProof)");
        Optional<ReputationOptions> reputationOptions = Optional.of(new ReputationOptions(Set.of(accountCreationDateProof)));
        Optional<TransferOptions> transferOptions = new Random().nextBoolean() ?
                Optional.of(new TransferOptions("USA", "HSBC")) :
                new Random().nextBoolean() ? Optional.of(new TransferOptions("DE", "N26")) :
                        Optional.empty();
        return new SwapOffer(bidAsset, askAsset, baseCurrency, protocolTypes, makerNetworkId,
                marketBasedPrice, minAmountAsPercentage,
                disputeResolutionOptions, feeOptions, reputationOptions, transferOptions);
    }

    private Asset getRandomAsset(String code, long amount) {
        AssetTransfer.Type assetTransferType = new Random().nextBoolean() ? AssetTransfer.Type.AUTOMATIC : AssetTransfer.Type.MANUAL;
        List<TransferType> transferTypes = List.of(FiatTransferType.values()[new Random().nextInt(FiatTransferType.values().length)]);
        return new Asset(code, amount, transferTypes, assetTransferType);
    }

    private Offer getOfferToRemove() {
        int index = new Random().nextInt(data.size());
        return new ArrayList<>(data.values()).get(index);
    }
}
