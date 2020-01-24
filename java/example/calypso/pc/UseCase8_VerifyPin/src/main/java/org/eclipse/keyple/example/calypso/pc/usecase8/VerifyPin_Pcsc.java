/********************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.example.calypso.pc.usecase8;



import org.eclipse.keyple.calypso.command.po.parser.ReadDataStructure;
import org.eclipse.keyple.calypso.transaction.*;
import org.eclipse.keyple.core.selection.MatchingSelection;
import org.eclipse.keyple.core.selection.SeSelection;
import org.eclipse.keyple.core.selection.SelectionsResult;
import org.eclipse.keyple.core.seproxy.*;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols;
import org.eclipse.keyple.example.common.calypso.pc.transaction.CalypsoUtilities;
import org.eclipse.keyple.example.common.calypso.postructure.CalypsoClassicInfo;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1>Use Case ‘Calypso 7’ – Stored Value Debit (in secure session) (PC/SC)</h1>
 */
public class VerifyPin_Pcsc {
    private static final Logger logger = LoggerFactory.getLogger(VerifyPin_Pcsc.class);
    private static SeReader poReader;
    private static PoResource poResource;

    /**
     * Selects the PO
     *
     * @return true if the PO is selected
     * @throws KeypleReaderException in case of reader communication failure
     */
    private static boolean selectPo() throws KeypleReaderException {
        /* Check if a PO is present in the reader */
        if (poReader.isSePresent()) {

            logger.info(
                    "= 1st PO exchange: AID based selection with reading of Environment file.         =");

            /*
             * Prepare a Calypso PO selection
             */
            SeSelection seSelection = new SeSelection();

            /*
             * Setting of an AID based selection of a Calypso REV3 PO
             *
             * Select the first application matching the selection AID
             */

            /*
             * Calypso selection: configures a PoSelectionRequest with all the desired attributes to
             * make the selection and read additional information afterwards
             */
            PoSelectionRequest poSelectionRequest = new PoSelectionRequest(new PoSelector(
                    SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                    new PoSelector.PoAidSelector(
                            new SeSelector.AidSelector.IsoAid(CalypsoClassicInfo.AID),
                            PoSelector.InvalidatedPo.REJECT),
                    "AID: " + CalypsoClassicInfo.AID));

            seSelection.prepareSelection(poSelectionRequest);

            /*
             * Actual PO communication: operate through a single request the Calypso PO selection
             * and the file read
             */
            SelectionsResult selectionsResult = seSelection.processExplicitSelection(poReader);

            if (selectionsResult.hasActiveSelection()) {
                MatchingSelection matchingSelection = selectionsResult.getActiveSelection();

                CalypsoPo calypsoPo = (CalypsoPo) matchingSelection.getMatchingSe();
                poResource = new PoResource(poReader, calypsoPo);
                logger.info("The selection of the PO has succeeded.");
                return true;
            }
        } else {
            logger.error("No PO were detected.");
        }
        return false;
    }

    public static void main(String[] args) throws KeypleBaseException {

        /* Get the instance of the SeProxyService (Singleton pattern) */
        SeProxyService seProxyService = SeProxyService.getInstance();

        /* Assign PcscPlugin to the SeProxyService */
        seProxyService.registerPlugin(new PcscPluginFactory());

        /*
         * Get a PO reader ready to work with Calypso PO. Use the getReader helper method from the
         * CalypsoUtilities class.
         */
        poReader = CalypsoUtilities.getDefaultPoReader();


        /*
         * Get a SAM reader ready to work with Calypso PO. Use the getReader helper method from the
         * CalypsoUtilities class.
         */
        SamResource samResource = CalypsoUtilities.getDefaultSamResource();

        /* Check if the readers exists */
        if (poReader == null || samResource == null) {
            throw new IllegalStateException("Bad PO or SAM reader setup");
        }

        logger.info("=============== UseCase Calypso #8: Verify PIN  ==================");
        logger.info("= PO Reader  NAME = {}", poReader.getName());
        logger.info("= SAM Reader  NAME = {}", samResource.getSeReader().getName());

        if (selectPo()) {
            PoTransaction poTransaction = new PoTransaction(poResource, samResource,
                    CalypsoUtilities.getSecuritySettings());
            int verifyPinIndex = 0;

            try {
                byte[] pin = {(byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30};
                poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_EventLog,
                        ReadDataStructure.SINGLE_RECORD_DATA, CalypsoClassicInfo.RECORD_NUMBER_1,
                        String.format("EventLog (SFI=%02X, recnbr=%d))",
                                CalypsoClassicInfo.SFI_EventLog,
                                CalypsoClassicInfo.RECORD_NUMBER_1));
                poTransaction.prepareVerifyPinPlain(pin);
                poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_ContractList,
                        ReadDataStructure.SINGLE_RECORD_DATA, CalypsoClassicInfo.RECORD_NUMBER_1,
                        String.format("ContractList (SFI=%02X))",
                                CalypsoClassicInfo.SFI_ContractList));
                poTransaction.prepareVerifyPinEncrypted(pin);
                poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_EventLog,
                        ReadDataStructure.SINGLE_RECORD_DATA, CalypsoClassicInfo.RECORD_NUMBER_1,
                        String.format("EventLog (SFI=%02X, recnbr=%d))",
                                CalypsoClassicInfo.SFI_EventLog,
                                CalypsoClassicInfo.RECORD_NUMBER_1));
                poTransaction.prepareVerifyPinPlain(pin);
                poTransaction.prepareVerifyPinEncrypted(pin);
                poTransaction.prepareVerifyPinPlain(pin);
                poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_EventLog,
                        ReadDataStructure.SINGLE_RECORD_DATA, CalypsoClassicInfo.RECORD_NUMBER_1,
                        String.format("EventLog (SFI=%02X, recnbr=%d))",
                                CalypsoClassicInfo.SFI_EventLog,
                                CalypsoClassicInfo.RECORD_NUMBER_1));

                logger.warn("Open session.");

                if (poTransaction.processOpening(PoTransaction.ModificationMode.ATOMIC,
                        PoTransaction.SessionAccessLevel.SESSION_LVL_DEBIT, (byte) 0, (byte) 0)) {

                    poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_EventLog,
                            ReadDataStructure.SINGLE_RECORD_DATA,
                            CalypsoClassicInfo.RECORD_NUMBER_1,
                            String.format("EventLog (SFI=%02X, recnbr=%d))",
                                    CalypsoClassicInfo.SFI_EventLog,
                                    CalypsoClassicInfo.RECORD_NUMBER_1));
                    poTransaction.prepareVerifyPinPlain(pin);
                    poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_ContractList,
                            ReadDataStructure.SINGLE_RECORD_DATA,
                            CalypsoClassicInfo.RECORD_NUMBER_1,
                            String.format("ContractList (SFI=%02X))",
                                    CalypsoClassicInfo.SFI_ContractList));
                    poTransaction.prepareVerifyPinEncrypted(pin);
                    poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_EventLog,
                            ReadDataStructure.SINGLE_RECORD_DATA,
                            CalypsoClassicInfo.RECORD_NUMBER_1,
                            String.format("EventLog (SFI=%02X, recnbr=%d))",
                                    CalypsoClassicInfo.SFI_EventLog,
                                    CalypsoClassicInfo.RECORD_NUMBER_1));
                    poTransaction.prepareVerifyPinPlain(pin);
                    poTransaction.prepareVerifyPinEncrypted(pin);
                    poTransaction.prepareVerifyPinPlain(pin);
                    poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_EventLog,
                            ReadDataStructure.SINGLE_RECORD_DATA,
                            CalypsoClassicInfo.RECORD_NUMBER_1,
                            String.format("EventLog (SFI=%02X, recnbr=%d))",
                                    CalypsoClassicInfo.SFI_EventLog,
                                    CalypsoClassicInfo.RECORD_NUMBER_1));

                    poTransaction.processPoCommandsInSession();

                    logger.warn("Close session.");
                    if (poTransaction.processClosing(ChannelControl.CLOSE_AFTER)) {
                        if (poTransaction.isSuccessful()) {
                            logger.warn("Verify PIN operation in session successful.");
                        } else {
                            logger.error("Verify PIN operation failed: ",
                                    poTransaction.getLastError());
                        }
                    }
                } else {

                    logger.error("Verify PIN operation or secure session opening failed: {}",
                            poTransaction.getLastError());
                }
            } catch (KeypleReaderException ex) {
                logger.error(
                        "Attempt counter: " + poTransaction.getPinAttemptCounter(verifyPinIndex));
            }
        } else {
            logger.error("The PO selection failed");
        }

        System.exit(0);
    }
}
