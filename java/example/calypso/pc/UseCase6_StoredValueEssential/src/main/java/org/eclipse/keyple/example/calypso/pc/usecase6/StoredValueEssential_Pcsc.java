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
package org.eclipse.keyple.example.calypso.pc.usecase6;


import java.util.Scanner;
import org.eclipse.keyple.calypso.command.po.parser.ReadDataStructure;
import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvGetRespPars;
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
 * <h1>Use Case ‘Calypso 6’ – Stored Value (PC/SC)</h1>
 */
public class StoredValueEssential_Pcsc {
    private static final Logger logger = LoggerFactory.getLogger(StoredValueEssential_Pcsc.class);
    private static SeReader poReader;
    private static SamResource samResource;
    private static PoResource poResource;
    private static PoTransaction poTransaction;

    /**
     * Selects the PO
     * 
     * @return true if the PO is selected
     * @throws KeypleReaderException in case of reader communication failure
     */
    private static boolean waitAndSelectPo() throws KeypleReaderException {
        logger.info("Please present a card!");
        /* Wait for a PO */
        while (!poReader.isSePresent()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("= 1st PO exchange: AID based selection.         =");

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
        PoSelectionRequest poSelectionRequest =
                new PoSelectionRequest(new PoSelector(SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                        new PoSelector.PoAidSelector(
                                new SeSelector.AidSelector.IsoAid(CalypsoClassicInfo.AID),
                                PoSelector.InvalidatedPo.REJECT),
                        "AID: " + CalypsoClassicInfo.AID));

        seSelection.prepareSelection(poSelectionRequest);

        /*
         * Actual PO communication: operate through a single request the Calypso PO selection and
         * the file read
         */
        SelectionsResult selectionsResult = seSelection.processExplicitSelection(poReader);

        if (selectionsResult.hasActiveSelection()) {
            MatchingSelection matchingSelection = selectionsResult.getActiveSelection();

            CalypsoPo calypsoPo = (CalypsoPo) matchingSelection.getMatchingSe();
            poResource = new PoResource(poReader, calypsoPo);
            logger.info("The selection of the PO has succeeded.");
            return true;
        }
        return false;
    }

    /**
     * Get the Stored Value status (balance)
     * <p>
     * The underlying operation is SvGet for debit
     * 
     * @throws KeypleReaderException in case of reader communication failure
     * @return true if the operation succeeded
     */
    private static boolean svView() throws KeypleReaderException {
        int svGetIndexDebit = poTransaction.prepareSvGet(SvOperation.DEBIT, SvAction.DO);

        if (poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)) {
            SvGetRespPars svGetRespPars =
                    ((SvGetRespPars) poTransaction.getResponseParser(svGetIndexDebit));
            logger.warn("| SV balance = {}", svGetRespPars.getBalance());
            return true;
        }
        logger.error("Getting the SV balance failed.");
        return false;
    }

    /**
     * Reload the SV with the provided amount outside a Calypso Session
     *
     * @param amount the reloading amount
     * @return true if the operation is successful, else false
     * @throws KeypleReaderException
     */
    private static boolean svReload(int amount) throws KeypleReaderException {
        int svGetIndex = poTransaction.prepareSvGet(SvOperation.RELOAD, SvAction.DO);

        if (!poTransaction.processPoCommands(ChannelControl.KEEP_OPEN)) {
            return false;
        } else {
            SvGetRespPars svGetRespPars =
                    ((SvGetRespPars) poTransaction.getResponseParser(svGetIndex));
            logger.warn("SV balance = {}", svGetRespPars.getBalance());
            logger.warn("Last reload amount = {}", svGetRespPars.getLoadLog().getAmount());
        }

        int svReloadIndex = poTransaction.prepareSvReload(amount);

        if (poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)) {
            if (poTransaction.isSuccessful()) {
                logger.warn("Reload operation successful.");
            } else {
                logger.error("Reload operation failed: ", poTransaction.getLastError());
            }
            return true;
        }
        logger.error("Reloading the SV balance failed.");
        return false;
    }

    /**
     * Reload the SV with the provided amount inside a Calypso Session
     *
     * @param amount the reloading amount inside a Calypso Session
     * @return true if the operation is successful, else false
     * @throws KeypleReaderException
     */
    private static boolean svReloadInSession(int amount) throws KeypleReaderException {
        int readRecordIndex =
                poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_EnvironmentAndHolder,
                        ReadDataStructure.SINGLE_RECORD_DATA, CalypsoClassicInfo.RECORD_NUMBER_1,
                        29, String.format("EnvironmentAndHolder (SFI=%02X))",
                                CalypsoClassicInfo.SFI_EnvironmentAndHolder));

        int svGetIndex = poTransaction.prepareSvGet(SvOperation.RELOAD, SvAction.DO);



        logger.warn("Open session.");
        if (!poTransaction.processOpening(PoTransaction.ModificationMode.ATOMIC,
                PoTransaction.SessionAccessLevel.SESSION_LVL_LOAD, (byte) 0, (byte) 0)) {
            return false;
        } else {
            SvGetRespPars svGetRespPars =
                    ((SvGetRespPars) poTransaction.getResponseParser(svGetIndex));
            logger.warn("SV balance = {}", svGetRespPars.getBalance());
            logger.warn("Last reload amount = {}", svGetRespPars.getLoadLog().getAmount());
        }

        int svReloadIndex = poTransaction.prepareSvReload(amount);

        logger.warn("Close session.");
        if (poTransaction.processClosing(ChannelControl.CLOSE_AFTER)) {
            if (poTransaction.isSuccessful()) {
                logger.warn("Reload operation in session successful.");
            } else {
                logger.error("Reload operation failed: ", poTransaction.getLastError());
            }
            return true;
        }
        logger.error("Reloading the SV balance failed.");
        return false;
    }

    /**
     * Debit the SV with the provided amount
     *
     * @param amount the debiting amount outside a Calypso Session
     * @return true if the operation is successful, else false
     * @throws KeypleReaderException
     */
    private static boolean svDebit(int amount) throws KeypleReaderException {
        int svGetIndex = poTransaction.prepareSvGet(SvOperation.DEBIT, SvAction.DO);

        if (!poTransaction.processPoCommands(ChannelControl.KEEP_OPEN)) {
            return false;
        } else {
            SvGetRespPars svGetRespPars =
                    ((SvGetRespPars) poTransaction.getResponseParser(svGetIndex));
            logger.warn("SV balance = {}", svGetRespPars.getBalance());
            logger.warn("Last debit amount = {}", svGetRespPars.getDebitLog().getAmount());
        }

        int svDebitIndex = poTransaction.prepareSvDebit(amount);

        if (poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)) {
            if (poTransaction.isSuccessful()) {
                logger.warn("Debit operation successful.");
            } else {
                logger.error("Debit operation failed: ", poTransaction.getLastError());
            }
            return true;
        }
        logger.error("Debiting the SV balance failed.");
        return false;
    }

    /**
     * Debit the SV with the provided amount inside a Calypso Session
     *
     * @param amount the debiting amount
     * @return true if the operation is successful, else false
     * @throws KeypleReaderException
     */
    private static boolean svDebitInSession(int amount) throws KeypleReaderException {
        // int readRecordIndex =
        // poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_EnvironmentAndHolder,
        // ReadDataStructure.SINGLE_RECORD_DATA, CalypsoClassicInfo.RECORD_NUMBER_1,
        // 29, String.format("EnvironmentAndHolder (SFI=%02X))",
        // CalypsoClassicInfo.SFI_EnvironmentAndHolder));

        int svGetIndex = poTransaction.prepareSvGet(SvOperation.DEBIT, SvAction.DO);

        logger.warn("Open session.");
        if (!poTransaction.processOpening(PoTransaction.ModificationMode.ATOMIC,
                PoTransaction.SessionAccessLevel.SESSION_LVL_DEBIT, (byte) 0, (byte) 0)) {
            return false;
        } else {
            SvGetRespPars svGetRespPars =
                    ((SvGetRespPars) poTransaction.getResponseParser(svGetIndex));
            logger.warn("SV balance = {}", svGetRespPars.getBalance());
            logger.warn("Last debit amount = {}", svGetRespPars.getDebitLog().getAmount());
        }

        int svReloadIndex = poTransaction.prepareSvDebit(amount);

        logger.warn("Close session.");
        if (poTransaction.processClosing(ChannelControl.CLOSE_AFTER)) {
            if (poTransaction.isSuccessful()) {
                logger.warn("Debit operation in session successful.");
            } else {
                logger.error("Debit operation failed: ", poTransaction.getLastError());
            }
            return true;
        }
        logger.error("Debiting the SV balance failed.");
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
        samResource = CalypsoUtilities.getDefaultSamResource();

        /* Check if the readers exists */
        if (poReader == null || samResource == null) {
            throw new IllegalStateException("Bad PO or SAM reader setup");
        }

        logger.info("=============== UseCase Calypso #6: Stored Value  ==================");
        logger.info("= PO Reader  NAME = {}", poReader.getName());
        logger.info("= SAM Reader  NAME = {}", samResource.getSeReader().getName());

        Scanner keyboard = new Scanner(System.in);
        boolean loop = true;

        while (loop) {

            logger.info("== Calypso Usecase 6 Stored Value ==");
            logger.info(" 0) View the balance");
            logger.info(" 1) Reload");
            logger.info(" 2) Debit");
            logger.info(" 3) Reload in Calypso Secure session");
            logger.info(" 4) Debit in Calypso Secure session");
            logger.info(" 5) Exit");
            logger.info("Select an SV operation: ");
            int operation = keyboard.nextInt();
            if (operation < 0 || operation > 5) {
                logger.error("Unavailable operation.");
                continue;
            }

            if (operation == 5) {
                logger.warn("Exit on user request.");
                System.exit(0);
            }

            if (waitAndSelectPo()) {
                poTransaction = new PoTransaction(poResource, samResource,
                        CalypsoUtilities.getSecuritySettings());
                int amount;

                switch (operation) {
                    case 0:
                        svView();
                        break;
                    case 1:
                        logger.info("Enter the amount to reload: ");
                        amount = keyboard.nextInt();
                        logger.info("Reload amount = {}", amount);
                        try {
                            svReload(amount);
                        } catch (KeypleReaderException e) {
                            logger.error("DO SvReload raised an exception: {}", e.getMessage());
                        }
                        break;
                    case 2:
                        logger.info("Enter the amount to debit: ");
                        amount = keyboard.nextInt();
                        logger.info("Debit amount = {}", amount);
                        try {
                            svDebit(amount);
                        } catch (KeypleReaderException e) {
                            logger.error("DO SvDebit raised an exception: {}", e.getMessage());
                        }
                        break;
                    case 3:
                        logger.info("Enter the amount to reload in session: ");
                        amount = keyboard.nextInt();
                        logger.info("Reload amount = {}", amount);
                        try {
                            svReloadInSession(amount);
                        } catch (KeypleReaderException e) {
                            logger.error("DO SvReload raised an exception: {}", e.getMessage());
                        }
                        break;
                    case 4:
                        logger.info("Enter the amount to debit in session: ");
                        amount = keyboard.nextInt();
                        logger.info("Debit amount = {}", amount);
                        try {
                            svDebitInSession(amount);
                        } catch (KeypleReaderException e) {
                            logger.error("DO SvDebit raised an exception: {}", e.getMessage());
                        }
                        break;
                    case 5:
                        logger.warn("Exiting programm...");
                        loop = false;
                        break;
                    default:
                        break;
                }
                // logger.info("Please remove the card.");
                // while (poReader.isSePresent()) {
                // try {
                // Thread.sleep(200);
                // } catch (InterruptedException e) {
                // e.printStackTrace();
                // }
                // }
            }
        }
    }
}
