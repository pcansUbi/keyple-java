**Folder content**
---

* Package `org.eclipse.keyple.example.common.generic`

|File|Description|
|:---|---|
|`CustomProtocools.java`|Custom protocol definition|
|`CustomProtocolSetting.java`|Custom protocols list|
|`Demo_ObservableReaderNotificationEngine.java`|This class provides all the mechanisms to implement to perform the plugin/reader events handling.|
|`Demo_SeProtocolDetectionEngine.java`|This class provides all the mechanisms to implement to perform the protocol detection.|
|`DemoHelpers.java`|Helper class (reader management method)|

* Package `org.eclipse.keyple.example.common.calypso`

|File|Description|
|:---|---|
|`CalypsoBasicInfoAndSampleCommands.java`|This class provides Calypso data elements (files definitions, commands).|
|`Demo_CalypsoBasicTransactionEngine.java`|This class provides all the mechanisms to implement to perform a ticketing scenario with a basic Calypso Secure Session.<br>It is independent of the platform.|
|`HoplinkInfoAndSampleCommands.java`|This class provides Hoplink data elements (files definitions, commands).|
|`Demo_HoplinkTransactionEngine.java`|This class provides all the mechanisms to implement to perform a ticketing scenario with an Hoplink Secure Session.<br>It is independent of the platform.|

* Package `org.eclipse.keyple.example.pc.generic`

|File|Description|
|:---|---|
|`Demo_ObservableReaderNotification_Pcsc.java`|Main class for the plugin/reader observability example (PC/SC)|
|`Demo_ObservableReaderNotification_Stub.java`|Main class for the plugin/reader observability example (stub)|
|`Demo_SeProtocolDetection_Pcsc.java`|Main class for the protocol detection example (PC/SC)|
|`Demo_SeProtocolDetection_Stub.java`|Main class for the protocol detection example (stub)|
|`PcscReadersSettings.java`|Interface defining PC/SC readers identifiers (for PO / CSM reader role assignment)|

* Package `org.eclipse.keyple.example.pc.generic.stub.se`

|File|Description|
|:---|---|
|`StubISO14443_4.java`|ISO 14443-4 stub SE|
|`StubCalypsoBPrime.java`|Calypso B Prime stub SE|
|`StubMemoryST25.java`|ST25 Memory stub SE|
|`StubMifareClassic.java`|Mifare Classic stub SE|
|`StubMifareDesfire.java`|Mifare Desfire stub SE|
|`StubMifareUL.java`|Mifare Ultralight stub SE|
|`StubSe1.java`|Generic stub SE 1|
|`StubSe2.java`|Generic stub SE 2|

* Package `org.eclipse.keyple.example.pc.calypso`

|File|Description|
|:---|---|
|`Demo_CalypsoBasic_Pcsc.java`|Contains the main class for the Calypso PC/SC demo|
|`Demo_CalypsoBasic_Stub.java`|Contains the main class for the Calypso basic without the need of hardware readers|
|`Demo_Hoplink_Pcsc.java`|Contains the main class for the Hoplink PC/SC demo|
|`Demo_Hoplink_Stub.java`|Contains the main class for the Hoplink basic without the need of hardware readers|

* Package `org.eclipse.keyple.example.pc.calypso.stub.se`

|File|Description|
|:---|---|
|`StubCalypsoBasic.java`|Calypso PO stub SE (`StubSecureElement`)|
|`StubCsm.java`|Calypso CSM stub SE (`StubSecureElement`)|
|`StubHoplink.java`|Hoplink PO stub SE (`StubSecureElement`)|

**Build configuration**
---
### Gradle
    TBD
### Maven
    TBD

**Log configuration**
---
The application log output format is configurable in the properties files
`resources/simplelogger.properties`.
The user will mainly be interested in setting the log level with the `org.slf4j.simpleLogger.defaultLogLevel` field (see the documentation inside the file).