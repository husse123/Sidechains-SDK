package com.horizen.examples;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.horizen.SidechainSettings;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.box.Box;
import com.horizen.box.BoxSerializer;
import com.horizen.box.NoncedBox;
import com.horizen.box.data.NoncedBoxData;
import com.horizen.box.data.NoncedBoxDataSerializer;
import com.horizen.companion.SidechainBoxesDataCompanion;
import com.horizen.companion.SidechainProofsCompanion;
import com.horizen.companion.SidechainTransactionsCompanion;
import com.horizen.examples.car.api.CarApi;
import com.horizen.examples.car.box.CarBoxSerializer;
import com.horizen.examples.car.box.CarRegistryBoxesIdsEnum;
import com.horizen.examples.car.box.CarSellOrderBoxSerializer;
import com.horizen.examples.car.box.data.CarBoxDataSerializer;
import com.horizen.examples.car.box.data.CarRegistryBoxesDataIdsEnum;
import com.horizen.examples.car.box.data.CarSellOrderBoxDataSerializer;
import com.horizen.examples.car.proof.CarRegistryProofsIdsEnum;
import com.horizen.examples.car.proof.SellOrderSpendingProofSerializer;
import com.horizen.examples.car.transaction.BuyCarTransactionSerializer;
import com.horizen.examples.car.transaction.CarDeclarationTransactionSerializer;
import com.horizen.examples.car.transaction.CarRegistryTransactionsIdsEnum;
import com.horizen.examples.car.transaction.SellCarTransactionSerializer;
import com.horizen.proof.Proof;
import com.horizen.proof.ProofSerializer;
import com.horizen.proposition.Proposition;
import com.horizen.secret.Secret;
import com.horizen.secret.SecretSerializer;
import com.horizen.settings.SettingsReader;
import com.horizen.state.ApplicationState;
import com.horizen.storage.IODBStorageUtil;
import com.horizen.storage.Storage;
import com.horizen.transaction.BoxTransaction;
import com.horizen.transaction.TransactionSerializer;
import com.horizen.utils.Pair;
import com.horizen.wallet.ApplicationWallet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class CarRegistryAppModule
    extends AbstractModule
{
    private SettingsReader settingsReader;

    public CarRegistryAppModule(String userSettingsFileName) {
        this.settingsReader = new SettingsReader(userSettingsFileName, Optional.empty());
    }

    @Override
    protected void configure() {
        // Get sidechain settings
        SidechainSettings sidechainSettings = this.settingsReader.getSidechainSettings();


        // Define custom serializers:
        HashMap<Byte, BoxSerializer<Box<Proposition>>> customBoxSerializers = new HashMap<>();
        customBoxSerializers.put(CarRegistryBoxesIdsEnum.CarBoxId.id(), (BoxSerializer) CarBoxSerializer.getSerializer());
        customBoxSerializers.put(CarRegistryBoxesIdsEnum.CarSellOrderBoxId.id(), (BoxSerializer) CarSellOrderBoxSerializer.getSerializer());

        HashMap<Byte, NoncedBoxDataSerializer<NoncedBoxData<Proposition, NoncedBox<Proposition>>>> customBoxDataSerializers = new HashMap<>();
        customBoxDataSerializers.put(CarRegistryBoxesDataIdsEnum.CarBoxDataId.id(), (NoncedBoxDataSerializer) CarBoxDataSerializer.getSerializer());
        customBoxDataSerializers.put(CarRegistryBoxesDataIdsEnum.CarSellOrderBoxDataId.id(), (NoncedBoxDataSerializer) CarSellOrderBoxDataSerializer.getSerializer());

        HashMap<Byte, SecretSerializer<Secret>> customSecretSerializers = new HashMap<>();

        HashMap<Byte, ProofSerializer<Proof<Proposition>>> customProofSerializers = new HashMap<>();
        customProofSerializers.put(CarRegistryProofsIdsEnum.SellOrderSpendingProofId.id(), (ProofSerializer) SellOrderSpendingProofSerializer.getSerializer());

        HashMap<Byte, TransactionSerializer<BoxTransaction<Proposition, Box<Proposition>>>> customTransactionSerializers = new HashMap<>();
        customTransactionSerializers.put(CarRegistryTransactionsIdsEnum.CarDeclarationTransactionId.id(), (TransactionSerializer) CarDeclarationTransactionSerializer.getSerializer());
        customTransactionSerializers.put(CarRegistryTransactionsIdsEnum.SellCarTransactionId.id(), (TransactionSerializer) SellCarTransactionSerializer.getSerializer());
        customTransactionSerializers.put(CarRegistryTransactionsIdsEnum.BuyCarTransactionId.id(), (TransactionSerializer) BuyCarTransactionSerializer.getSerializer());

        SidechainBoxesDataCompanion sidechainBoxesDataCompanion = new SidechainBoxesDataCompanion(customBoxDataSerializers);
        SidechainProofsCompanion sidechainProofsCompanion = new SidechainProofsCompanion(customProofSerializers);
        SidechainTransactionsCompanion transactionsCompanion = new SidechainTransactionsCompanion(
                customTransactionSerializers, sidechainBoxesDataCompanion, sidechainProofsCompanion);


        // Define Application state and wallet logic:
        ApplicationWallet defaultApplicationWallet = new CarRegistryApplicationWallet();
        ApplicationState defaultApplicationState = new CarRegistryApplicationState();


        // Define the path to storages:
        String dataDirPath = sidechainSettings.scorexSettings().dataDir().getAbsolutePath();
        File secretStore = new File( dataDirPath + "/secret");
        File walletBoxStore = new File(dataDirPath + "/wallet");
        File walletTransactionStore = new File(dataDirPath + "/walletTransaction");
        File walletForgingBoxesInfoStorage = new File(dataDirPath + "/walletForgingStake");
        File stateStore = new File(dataDirPath + "/state");
        File historyStore = new File(dataDirPath + "/history");
        File consensusStore = new File(dataDirPath + "/consensusData");


        // Add car registry specific API endpoints:
        List<ApplicationApiGroup> customApiGroups = new ArrayList<>();
        customApiGroups.add(new CarApi(transactionsCompanion, sidechainBoxesDataCompanion, sidechainProofsCompanion));


        // No core API endpoints to be disabled:
        List<Pair<String, String>> rejectedApiPaths = new ArrayList<>();


        // Inject custom objects:
        bind(SidechainSettings.class)
                .annotatedWith(Names.named("SidechainSettings"))
                .toInstance(sidechainSettings);

        bind(new TypeLiteral<HashMap<Byte, BoxSerializer<Box<Proposition>>>>() {})
                .annotatedWith(Names.named("CustomBoxSerializers"))
                .toInstance(customBoxSerializers);
        bind(new TypeLiteral<HashMap<Byte, NoncedBoxDataSerializer<NoncedBoxData<Proposition, NoncedBox<Proposition>>>>>() {})
                .annotatedWith(Names.named("CustomBoxDataSerializers"))
                .toInstance(customBoxDataSerializers);
        bind(new TypeLiteral<HashMap<Byte, SecretSerializer<Secret>>>() {})
                .annotatedWith(Names.named("CustomSecretSerializers"))
                .toInstance(customSecretSerializers);
        bind(new TypeLiteral<HashMap<Byte, ProofSerializer<Proof<Proposition>>>>() {})
                .annotatedWith(Names.named("CustomProofSerializers"))
                .toInstance(customProofSerializers);
        bind(new TypeLiteral<HashMap<Byte, TransactionSerializer<BoxTransaction<Proposition, Box<Proposition>>>>>() {})
                .annotatedWith(Names.named("CustomTransactionSerializers"))
                .toInstance(customTransactionSerializers);

        bind(ApplicationWallet.class)
                .annotatedWith(Names.named("ApplicationWallet"))
                .toInstance(defaultApplicationWallet);

        bind(ApplicationState.class)
                .annotatedWith(Names.named("ApplicationState"))
                .toInstance(defaultApplicationState);

        bind(Storage.class)
                .annotatedWith(Names.named("SecretStorage"))
                .toInstance(IODBStorageUtil.getStorage(secretStore));
        bind(Storage.class)
                .annotatedWith(Names.named("WalletBoxStorage"))
                .toInstance(IODBStorageUtil.getStorage(walletBoxStore));
        bind(Storage.class)
                .annotatedWith(Names.named("WalletTransactionStorage"))
                .toInstance(IODBStorageUtil.getStorage(walletTransactionStore));
        bind(Storage.class)
                .annotatedWith(Names.named("WalletForgingBoxesInfoStorage"))
                .toInstance(IODBStorageUtil.getStorage(walletForgingBoxesInfoStorage));
        bind(Storage.class)
                .annotatedWith(Names.named("StateStorage"))
                .toInstance(IODBStorageUtil.getStorage(stateStore));
        bind(Storage.class)
                .annotatedWith(Names.named("HistoryStorage"))
                .toInstance(IODBStorageUtil.getStorage(historyStore));
        bind(Storage.class)
                .annotatedWith(Names.named("ConsensusStorage"))
                .toInstance(IODBStorageUtil.getStorage(consensusStore));

        bind(new TypeLiteral<List<ApplicationApiGroup>> () {})
                .annotatedWith(Names.named("CustomApiGroups"))
                .toInstance(customApiGroups);

        bind(new TypeLiteral<List<Pair<String, String>>> () {})
                .annotatedWith(Names.named("RejectedApiPaths"))
                .toInstance(rejectedApiPaths);
    }
}
