/********************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.integration.experimental.samresourcemanager;



import static org.eclipse.keyple.core.seproxy.ChannelControl.CLOSE_AFTER;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.eclipse.keyple.calypso.command.po.parser.ReadDataStructure;
import org.eclipse.keyple.calypso.command.po.parser.ReadRecordsRespPars;
import org.eclipse.keyple.calypso.command.sam.SamRevision;
import org.eclipse.keyple.calypso.transaction.*;
import org.eclipse.keyple.core.selection.SeSelection;
import org.eclipse.keyple.core.selection.SelectionsResult;
import org.eclipse.keyple.core.seproxy.*;
import org.eclipse.keyple.core.seproxy.event.ObservablePlugin;
import org.eclipse.keyple.core.seproxy.event.ObservablePlugin.PluginObserver;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.event.PluginEvent;
import org.eclipse.keyple.core.seproxy.event.ReaderEvent;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPlugin;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactory;
import org.eclipse.keyple.plugin.pcsc.PcscProtocolSetting;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

/**
 * <h1>Use Case ‘Calypso 5’ – SAM Resource Manager (PC/SC)</h1>
 */
public final class UseCase_Calypso6_SamResourceManager_Pcsc implements PluginObserver {
    private static final Logger logger =
            LoggerFactory.getLogger(UseCase_Calypso6_SamResourceManager_Pcsc.class);
    private PoReaderObserver poReaderObserver;
    private static final String AID = "315449432E49434131";
    public static final byte RECORD_NUMBER_1 = 1;
    public static final byte RECORD_NUMBER_2 = 2;
    public static final byte RECORD_NUMBER_3 = 3;
    public static final byte RECORD_NUMBER_4 = 4;

    public static final byte SFI_EnvironmentAndHolder = (byte) 0x07;
    public static final byte SFI_EventLog = (byte) 0x08;
    public static final byte SFI_ContractList = (byte) 0x1E;
    public static final byte SFI_Contracts = (byte) 0x09;

    private SamResourceManager samResourceManager;
    private ReaderPluginSlave poPlugin;
    private SeSelection seSelection;

    /**
     * This object is used to freeze the main thread while card operations are handle through the
     * observers callbacks. A call to the notify() method would end the program (not demonstrated
     * here).
     */
    private UseCase_Calypso6_SamResourceManager_Pcsc() throws KeypleBaseException {
        /* Get the instance of the SeProxyService (Singleton pattern) */
        SeProxyService seProxyService = SeProxyService.getInstance();

        /* Assign PcscPlugin to the SeProxyService */
        seProxyService.registerPlugin(new PcscPluginFactory());

        /* Get the instance of the PCSC plugin */
        ReaderPlugin pcscPlugin = SeProxyService.getInstance().getPlugin(PcscPlugin.PLUGIN_NAME);

        /* Create a meta plugin with the PCSC plugin as base plugin */
        MetaPlugin metaPlugin = new MetaPlugin(pcscPlugin);

        /* Create two slave plugins dedicated to SAM and PO */
        ReaderPluginSlave samPlugin = new ReaderPluginSlave("SamPlugin");
        poPlugin = new ReaderPluginSlave("PoPlugin");

        metaPlugin.registerSlave(samPlugin,
                ".*(Cherry TC|SCM Microsystems|Identive|HID|Generic).*");

        metaPlugin.registerSlave(poPlugin, ".*(ASK|ACS|SpringCard).*");

        samResourceManager = new SamResourceManager(samPlugin, ".*"); // TODO check if filter is
                                                                      // still useful

        /* create a PoReaderObserver ready for the SE selection */
        poReaderObserver = new PoReaderObserver(getSeSelection());

        /* Add an observer to the PO plugin to handle reader connections/disconnections */
        ((ObservablePlugin) poPlugin).addObserver(this);

        /* start reader observation */
        metaPlugin.startObservation();


        logger.info(
                "=============== UseCase Calypso #6: SAM resource manager =========================");
        logger.info(
                "= Wait for a PO reader.                                                          =");
        logger.info(
                "==================================================================================");

        // wait for Enter key to exit.
        logger.info("Press Enter to exit");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            int c = 0;
            try {
                c = br.read();
            } catch (IOException e) {
                logger.error("IOException: {}", e);
            }
            if (c == 0x0A) {
                logger.info("Exiting...");
                System.exit(0);
            }
        }

    }

    private SeSelection getSeSelection() {
        if (seSelection == null) {
            /*
             * Prepare a Calypso PO selection
             */
            seSelection = new SeSelection();

            /*
             * Setting up of an AID based selection of a Calypso REV3 PO
             *
             * Select the first application matching the selection AID, keep the logical channel
             * open after the selection
             */

            /*
             * Calypso selection: configures a PoSelectionRequest with all the desired attributes to
             * make the selection and read additional information afterwards
             */
            PoSelectionRequest poSelectionRequest = new PoSelectionRequest(
                    new PoSelector(SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                            new PoSelector.PoAidSelector(new SeSelector.AidSelector.IsoAid(AID),
                                    PoSelector.InvalidatedPo.REJECT),
                            "AID: " + AID));

            /*
             * Prepare the reading order and keep the associated parser for later use once the
             * selection has been made.
             */
            int readEnvironmentParser = poSelectionRequest.prepareReadRecordsCmd(
                    SFI_EnvironmentAndHolder, ReadDataStructure.SINGLE_RECORD_DATA, RECORD_NUMBER_1,
                    String.format("EnvironmentAndHolder (SFI=%02X))", SFI_EnvironmentAndHolder));

            /*
             * Add the selection case to the current selection (we could have added other cases
             * here)
             */
            seSelection.prepareSelection(poSelectionRequest);
        }
        return seSelection;
    }

    @Override
    public void update(PluginEvent event) {
        for (String readerName : event.getReaderNames()) {
            SeReader reader = null;
            logger.info("PluginEvent: PLUGINNAME = {}, READERNAME = {}, EVENTTYPE = {}",
                    event.getPluginName(), readerName, event.getEventType());

            // TODO Check if filtering is needed?
            // /* ignore non contactless readers */
            // if (ReaderUtilities
            // .getReaderType(readerName) != ReaderUtilities.ReaderType.CONTACTLESS_READER) {
            // continue;
            // }

            /* We retrieve the reader object from its name. */
            try {
                reader = poPlugin.getReader(readerName);
            } catch (KeypleReaderNotFoundException e) {
                logger.error("KeypleReaderNotFoundException: {}", e);
            }
            if (reader == null) {
                throw new IllegalStateException("Reader is null");
            }
            switch (event.getEventType()) {
                case READER_CONNECTED:
                    logger.info("New reader! READERNAME = {}", reader.getName());

                    /*
                     * We are informed here of a disconnection of a reader.
                     *
                     * We add an observer to this reader if this is possible.
                     */
                    if (reader instanceof ObservableReader) {
                        if (poReaderObserver != null) {
                            logger.info("Add observer READERNAME = {}", reader.getName());

                            ((ObservableReader) reader).addObserver(poReaderObserver);

                            try {
                                /* Enable logging */
                                reader.setParameter(PcscReader.SETTING_KEY_LOGGING, "true");

                                /* Contactless SE works with T1 protocol */
                                reader.setParameter(PcscReader.SETTING_KEY_PROTOCOL,
                                        PcscReader.SETTING_PROTOCOL_T1);

                                /*
                                 * PC/SC card access mode:
                                 *
                                 * The SAM is left in the SHARED mode (by default) to avoid
                                 * automatic resets due to the limited time between two consecutive
                                 * exchanges granted by Windows.
                                 *
                                 * The PO reader is set to EXCLUSIVE mode to avoid side effects
                                 * during the selection step that may result in session failures.
                                 *
                                 * These two points will be addressed in a coming release of the
                                 * Keyple PcSc reader plugin.
                                 */
                                reader.setParameter(PcscReader.SETTING_KEY_MODE,
                                        PcscReader.SETTING_MODE_SHARED);

                                /* Set the PO reader protocol flag */
                                reader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_ISO14443_4,
                                        PcscProtocolSetting.PCSC_PROTOCOL_SETTING
                                                .get(SeCommonProtocols.PROTOCOL_ISO14443_4));
                            } catch (KeypleBaseException e) {
                                logger.error("KeypleBaseException: {}", e);
                            }

                            ((ObservableReader) reader).setDefaultSelectionRequest(
                                    getSeSelection().getSelectionOperation(),
                                    ObservableReader.NotificationMode.MATCHED_ONLY);
                            ((ObservableReader) reader)
                                    .startSeDetection(ObservableReader.PollingMode.REPEATING);
                        } else {
                            logger.info("No observer to add READERNAME = {}", reader.getName());
                        }
                    }
                    break;
                case READER_DISCONNECTED:
                    /*
                     * We are informed here of a disconnection of a reader.
                     *
                     * The reader object still exists but will be removed from the reader list right
                     * after. Thus, we can properly remove the observer attached to this reader
                     * before the list update.
                     */
                    logger.info("Reader removed. READERNAME = {}", readerName);
                    if (reader instanceof ObservableReader) {
                        if (poReaderObserver != null) {
                            logger.info("Remove observer READERNAME = {}", readerName);
                            ((ObservableReader) reader).removeObserver(poReaderObserver);
                        } else {
                            logger.info("Unplugged reader READERNAME = {} wasn't observed.",
                                    readerName);
                        }
                    }
                    break;
                default:
                    logger.info("Unexpected reader event. EVENT = {}",
                            event.getEventType().getName());
                    break;
            }
        }
    }


    /**
     * This method is called whenever a Reader event occurs (SE insertion/removal)
     */
    class PoReaderObserver implements ObservableReader.ReaderObserver {
        private final SeSelection seSelection;

        PoReaderObserver(SeSelection seSelection) {
            super();
            this.seSelection = seSelection;
        }

        /**
         * Method invoked in the case of a reader event
         *
         * @param event the reader event
         */
        @Override
        public void update(ReaderEvent event) {
            SeReader poReader = null;
            SamResource samResource = null;
            try {
                poReader = ReaderUtilities.getReaderByName(SeProxyService.getInstance(),
                        event.getReaderName().substring(0, 5) + ".*");
            } catch (KeypleReaderException e) {
                logger.error("KeypleReaderException: {}", e);
            }
            switch (event.getEventType()) {
                case SE_MATCHED:
                    Profiler profiler = new Profiler("Entire transaction");
                    SelectionsResult selectionsResult = seSelection
                            .processDefaultSelection(event.getDefaultSelectionsResponse());
                    if (selectionsResult.hasActiveSelection()) {
                        CalypsoPo calypsoPo =
                                (CalypsoPo) selectionsResult.getActiveSelection().getMatchingSe();

                        logger.info(
                                "Observer notification: the selection of the PO has succeeded.");

                        try {
                            samResource = samResourceManager.allocateSamResource(
                                    SamResourceManager.AllocationMode.BLOCKING,
                                    new SamIdentifier(SamRevision.AUTO, "", "0"));
                            if (samResource == null) {
                                logger.error("No SAM resource available.");
                                return;
                            }
                        } catch (KeypleReaderException e) {
                            logger.error("KeypleReaderException: {}", e);
                        }


                        PoTransaction poTransaction =
                                new PoTransaction(new PoResource(poReader, calypsoPo), samResource,
                                        new SecuritySettings());

                        try {

                            /*
                             * Open Session for the debit key
                             */
                            boolean poProcessStatus;
                            poProcessStatus = poTransaction.processOpening(
                                    PoTransaction.ModificationMode.ATOMIC,
                                    PoTransaction.SessionAccessLevel.SESSION_LVL_DEBIT, (byte) 0,
                                    (byte) 0);

                            if (!poProcessStatus) {
                                throw new IllegalStateException("processingOpening failure.");
                            }

                            int readEventLogParserIndex =
                                    poTransaction.prepareReadRecordsCmd(SFI_EventLog,
                                            ReadDataStructure.SINGLE_RECORD_DATA, RECORD_NUMBER_1,
                                            String.format("EventLog (SFI=%02X, recnbr=%d))",
                                                    SFI_EventLog, RECORD_NUMBER_1));

                            if (poTransaction.processPoCommandsInSession()) {
                                logger.info("The reading of the EventLog has succeeded.");

                                byte[] eventLog = ((ReadRecordsRespPars) (poTransaction
                                        .getResponseParser(readEventLogParserIndex))).getRecords()
                                                .get((int) RECORD_NUMBER_1);

                                if (logger.isInfoEnabled()) {
                                    logger.info("EventLog file data: {}",
                                            ByteArrayUtil.toHex(eventLog));
                                }
                            }

                            /*
                             * Close the Secure Session.
                             */
                            if (logger.isInfoEnabled()) {
                                logger.info(
                                        "========= PO Calypso session ======= Closing ============================");
                            }

                            /*
                             * A ratification command will be sent (CONTACTLESS_MODE).
                             */
                            poProcessStatus = poTransaction.processClosing(CLOSE_AFTER);

                            profiler.stop();
                            logger.warn(System.getProperty("line.separator") + "{}", profiler);

                            if (!poProcessStatus) {
                                throw new IllegalStateException("processClosing failure.");
                            }
                        } catch (KeypleReaderException e) {
                            logger.error("KeypleReaderException: {}", e);
                        }
                        logger.info(
                                "==================================================================================");
                        logger.info(
                                "= End of the Calypso PO processing.                                              =");
                        logger.info(
                                "==================================================================================");
                    } else {
                        logger.error(
                                "The selection of the PO has failed. Should not have occurred due to the MATCHED_ONLY selection mode.");
                    }
                    samResourceManager.freeSamResource(samResource);
                    break;
                case SE_INSERTED:
                    logger.error(
                            "SE_INSERTED event: should not have occurred due to the MATCHED_ONLY selection mode.");
                    break;
                case SE_REMOVED:
                    logger.info("The PO has been removed.");
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * main program entry
     */
    public static void main(String[] args) throws KeypleBaseException {
        /* Create the observable object to handle the PO processing */
        new UseCase_Calypso6_SamResourceManager_Pcsc();
    }
}
