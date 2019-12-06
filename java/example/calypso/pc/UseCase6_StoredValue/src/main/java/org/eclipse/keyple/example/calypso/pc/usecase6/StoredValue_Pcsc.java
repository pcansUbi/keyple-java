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


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvDebitRespPars;
import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvGetRespPars;
import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvReloadRespPars;
import org.eclipse.keyple.calypso.transaction.*;
import org.eclipse.keyple.core.selection.MatchingSelection;
import org.eclipse.keyple.core.selection.SeSelection;
import org.eclipse.keyple.core.selection.SelectionsResult;
import org.eclipse.keyple.core.seproxy.*;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.example.common.calypso.pc.transaction.CalypsoUtilities;
import org.eclipse.keyple.example.common.calypso.postructure.CalypsoClassicInfo;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1>Use Case ‘Calypso 6’ – Stored Value (PC/SC)</h1>
 */
public class StoredValue_Pcsc {
    private static final Logger logger = LoggerFactory.getLogger(StoredValue_Pcsc.class);
    private static SeReader poReader;
    private static SamResource samResource;
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

    /**
     * Get the Stored Value status (balance)
     * <p>
     * The underlying operation is SvGet for debit
     * 
     * @throws KeypleReaderException in case of reader communication failure
     * @return true if the operation succeeded
     */
    private static boolean svView() throws KeypleReaderException {
        if (selectPo()) {
            PoTransaction poTransaction = new PoTransaction(poResource, samResource,
                    CalypsoUtilities.getSecuritySettings());

            int svGetIndex =
                    poTransaction.prepareSvGet(SvOperation.DEBIT, "svView: SvGet for DEBIT");

            if (poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)) {
                SvGetRespPars svGetRespPars =
                        ((SvGetRespPars) poTransaction.getResponseParser(svGetIndex));
                logger.info("SV balance = {}", svGetRespPars.getBalance());
                logger.info("Last debit amount = {}", svGetRespPars.getDebitLog().getAmount());
                return true;
            }
        }
        logger.error("Getting the SV balance failed.");
        return false;
    }

    private static boolean svReload(int amount) throws KeypleReaderException {
        if (selectPo()) {
            PoTransaction poTransaction = new PoTransaction(poResource, samResource,
                    CalypsoUtilities.getSecuritySettings());

            int svGetIndex = poTransaction.prepareSvGet(SvOperation.RELOAD, "SvGet for RELOAD");

            if (!poTransaction.processPoCommands(ChannelControl.KEEP_OPEN)) {
                return false;
            } else {
                SvGetRespPars svGetRespPars =
                        ((SvGetRespPars) poTransaction.getResponseParser(svGetIndex));
                logger.info("SV balance = {}", svGetRespPars.getBalance());
                logger.info("Last reload amount = {}", svGetRespPars.getLoadLog().getAmount());
            }

            byte[] datenow;
            byte[] timenow;
            byte[] free;
            Date now = new Date();
            SimpleDateFormat dateFormater = new SimpleDateFormat("ddMM");
            SimpleDateFormat timeFormater = new SimpleDateFormat("hhmm");
            datenow = ByteArrayUtil.fromHex(dateFormater.format(now));
            timenow = ByteArrayUtil.fromHex(timeFormater.format(now));
            free = ByteArrayUtil.fromHex("0000");

            int svReloadIndex = poTransaction.prepareSvReload(amount, datenow, timenow, free,
                    "SvReload: +" + amount);

            if (poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)) {
                logger.info("Reload operation successful.");
                SvReloadRespPars svReloadRespPars =
                        ((SvReloadRespPars) poTransaction.getResponseParser(svReloadIndex));
                return true;
            }
        }
        logger.error("Getting the SV balance failed.");
        return false;
    }


    private static boolean svUnreload(int amount) throws KeypleReaderException {
        if (selectPo()) {
            PoTransaction poTransaction = new PoTransaction(poResource, samResource,
                    CalypsoUtilities.getSecuritySettings());

            int svGetIndex = poTransaction.prepareSvGet(SvOperation.RELOAD, "SvGet for RELOAD");

            if (!poTransaction.processPoCommands(ChannelControl.KEEP_OPEN)) {
                return false;
            } else {
                SvGetRespPars svGetRespPars =
                        ((SvGetRespPars) poTransaction.getResponseParser(svGetIndex));
                logger.info("SV balance = {}", svGetRespPars.getBalance());
                logger.info("Last reload amount = {}", svGetRespPars.getLoadLog().getAmount());
            }

            byte[] datenow;
            byte[] timenow;
            byte[] free;
            Date now = new Date();
            SimpleDateFormat dateFormater = new SimpleDateFormat("ddMM");
            SimpleDateFormat timeFormater = new SimpleDateFormat("hhmm");
            datenow = ByteArrayUtil.fromHex(dateFormater.format(now));
            timenow = ByteArrayUtil.fromHex(timeFormater.format(now));
            free = ByteArrayUtil.fromHex("0000");

            int svReloadIndex = poTransaction.prepareSvReload(amount, datenow, timenow, free,
                    "SvReload: +" + amount);

            if (poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)) {
                logger.info("Reload operation successful.");
                SvReloadRespPars svReloadRespPars =
                        ((SvReloadRespPars) poTransaction.getResponseParser(svReloadIndex));
                return true;
            }
        }
        logger.error("Getting the SV balance failed.");
        return false;
    }

    private static boolean svDebit(int amount) throws KeypleReaderException {
        if (selectPo()) {
            PoTransaction poTransaction = new PoTransaction(poResource, samResource,
                    CalypsoUtilities.getSecuritySettings());

            int svGetIndex = poTransaction.prepareSvGet(SvOperation.DEBIT, "SvGet for DEBIT");

            if (!poTransaction.processPoCommands(ChannelControl.KEEP_OPEN)) {
                return false;
            } else {
                SvGetRespPars svGetRespPars =
                        ((SvGetRespPars) poTransaction.getResponseParser(svGetIndex));
                logger.info("SV balance = {}", svGetRespPars.getBalance());
                logger.info("Last debit amount = {}", svGetRespPars.getDebitLog().getAmount());
            }

            byte[] datenow;
            byte[] timenow;
            Date now = new Date();
            SimpleDateFormat dateFormater = new SimpleDateFormat("ddMM");
            SimpleDateFormat timeFormater = new SimpleDateFormat("hhmm");
            datenow = ByteArrayUtil.fromHex(dateFormater.format(now));
            timenow = ByteArrayUtil.fromHex(timeFormater.format(now));

            int svDebitIndex = poTransaction.prepareSvDebit(amount, datenow, timenow, "SvDebit: +" + amount);

            if (poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)) {
                logger.info("Debit operation successful.");
                SvDebitRespPars svDebitRespPars =
                        ((SvDebitRespPars) poTransaction.getResponseParser(svDebitIndex));
                return true;
            }
        }
        logger.error("Getting the SV balance failed.");
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
        boolean loop = true;

        while (loop) {
            logger.info("== Calypso Usecase 6 Stored Value ==");
            logger.info(" 0) View the balance");
            logger.info(" 1) Reload");
            logger.info(" 2) Unreload");
            logger.info(" 3) Debit");
            logger.info(" 4) Undebit");
            logger.info(" 5) Exit");
            logger.info("Select an SV operation: ");
            Scanner keyboard = new Scanner(System.in);
            int operation = keyboard.nextInt();
            if (operation < 0 || operation > 5) {
                logger.error("Unavailable operation.");
                continue;
            }

            int amount;

            switch (operation) {
                case 0:
                    svView();
                    break;
                case 1:
                    logger.info("Enter the amount to reload: ");
                    amount = keyboard.nextInt();
                    logger.info("Reload amount = {}", amount);
                    svReload(amount);
                    break;
                case 2:
                    logger.info("Enter the amount to unreload: ");
                    amount = keyboard.nextInt();
                    logger.info("Unreload amount = {}", amount);
                    svReload(amount);
                    break;
                case 3:
                    logger.info("Enter the amount to debit: ");
                    amount = keyboard.nextInt();
                    logger.info("Debit amount = {}", amount);
                    svDebit(amount);
                    break;
                case 4:
                    logger.info("Enter the amount to undebit: ");
                    amount = keyboard.nextInt();
                    logger.info("Undebit amount = {}", amount);
                    break;
                case 5:
                    logger.warn("Exiting programm...");
                    loop = false;
                    break;
                default:
                    break;
            }
        }
        System.exit(0);
    }
}
