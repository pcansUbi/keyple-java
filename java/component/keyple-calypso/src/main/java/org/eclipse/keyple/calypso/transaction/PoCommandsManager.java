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
package org.eclipse.keyple.calypso.transaction;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.keyple.calypso.command.po.AbstractPoCommandBuilder;
import org.eclipse.keyple.calypso.command.po.AbstractPoResponseParser;
import org.eclipse.keyple.calypso.command.po.PoBuilderParser;
import org.eclipse.keyple.calypso.command.po.PoSvCommand;
import org.eclipse.keyple.calypso.command.po.builder.security.PinOperation;
import org.eclipse.keyple.calypso.command.po.builder.security.PoGetChallengeCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.security.VerifyPinCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvGetCmdBuild;
import org.eclipse.keyple.core.command.AbstractApduResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The PO command manager is used to keep builders and parsers between the time the commands are
 * created and the time the parsers are going to be used.
 * <p>
 * A flag (preparedCommandsProcessed) is used to manage the reset of the command list. It allows the
 * builders to be kept until the application creates a new list of commands. This flag is reset by
 * calling the method notifyCommandsProcessed.
 */
class PoCommandsManager {
    /* logger */
    private static final Logger logger = LoggerFactory.getLogger(PoCommandsManager.class);

    /** The list to contain the prepared commands and their parsers */
    private final List<PoBuilderParser> poBuilderParserList = new ArrayList<PoBuilderParser>();
    /** The command index, incremented each time a command is added */
    private int preparedCommandIndex;
    private boolean preparedCommandsProcessed;
    private boolean secureSessionIsOpen;
    private boolean lastCommandIsSvGet;
    private int svGetIndex = -1;
    private int svOpIndex = -1;
    private SvSettings.Operation svOperation;
    private SvSettings.Action svAction = SvSettings.Action.DO;
    private boolean svOperationPending;

    PoCommandsManager() {
        preparedCommandsProcessed = true;
        svOperationPending = false;
        secureSessionIsOpen = false;
    }

    /**
     * Resets the list of builders/parsers (only if it has already been processed).
     * <p>
     * Clears the processed flag.
     */
    private void updateBuilderParserList() {
        if (preparedCommandsProcessed) {
            poBuilderParserList.clear();
            preparedCommandIndex = 0;
            preparedCommandsProcessed = false;
        }
    }


    /**
     * Indicates whether a secure session is open or not.
     * 
     * @param secureSessionIsOpen true if a secure session is open
     */
    public void setSecureSessionIsOpen(boolean secureSessionIsOpen) {
        this.secureSessionIsOpen = secureSessionIsOpen;
    }

    /**
     * Add a regular command (all but the StoredValue commands) to the builders and parsers list.
     * <p>
     * Handle the clearing of the list if needed.
     *
     * @param commandBuilder the command builder
     * @return the index to retrieve the parser later
     */
    int addRegularCommand(AbstractPoCommandBuilder commandBuilder) {
        if (commandBuilder instanceof PoSvCommand) {
            throw new IllegalStateException("An SV command cannot be added with this method.");
        }

        /**
         * Reset the list if when preparing the first command after the last processing.
         * <p>
         * However, the parsers have remained available until now.
         */
        updateBuilderParserList();

        poBuilderParserList.add(new PoBuilderParser(commandBuilder));
        /* return and post-increment index */
        preparedCommandIndex++;
        /* not an SV Get command */
        lastCommandIsSvGet = false;
        return (preparedCommandIndex - 1);
    }

    /**
     * Add a Verify Pin encrypted command to the builders and parsers list.
     * <p>
     * Handle the clearing of the list if needed.
     * <p>
     * Two commands will result: Get Challenge and Verify Pin. Verify Pin is not build here since we
     * need data from the SAM to build it. Get Challenge is inserted with the VERIFY_PIN split
     * attribute to handle the case later.
     *
     * @param calypsoPo the calypsoPo to handle the building of the Get Challenge command
     * @param pin a 4-byte byte array containing the PIN value
     * @return the index to retrieve the parser later
     */
    int addVerifyPinEncryptedCommand(CalypsoPo calypsoPo, byte[] pin) {
        /**
         * Reset the list if when preparing the first command after the last processing.
         * <p>
         * However, the parsers have remained available until now.
         */
        updateBuilderParserList();

        /*
         * insert Get Challenge into the command list, mark it as a split command, the next command
         * is VERIFY_PIN
         */
        poBuilderParserList
                .add(new PoBuilderParser(new PoGetChallengeCmdBuild(calypsoPo.getPoClass()),
                        PoBuilderParser.SplitCommandInfo.VERIFY_PIN));
        /*
         * insert intermediate Verify Pin command builder in the list of commands that will later be
         * replaced by the real "verify pin" command
         */
        poBuilderParserList.add(new PoBuilderParser(new VerifyPinCmdBuild(calypsoPo.getPoClass(),
                PinOperation.SEND_ENCRYPTED_PIN, pin)));

        /* return and post-increment index */
        preparedCommandIndex += 2;
        /* not an SV Get command */
        lastCommandIsSvGet = false;
        return (preparedCommandIndex - 1);
    }

    /**
     * Add a StoredValue command to the builders and parsers list.
     * <p>
     * Handle the clearing of the list if needed.
     * <p>
     * Set up a mini state machine to manage the scheduling of Stored Value commands keeping the
     * position of the last SvGet/Operation in the command list.
     * <p>
     * The {@link SvSettings.Operation} and {@link SvSettings.Action} are also used to check the
     * consistency of the SV process.
     * <p>
     * The svOperationPending flag is set when an SV operation (Reload/Debit/Undebit) command is
     * added.
     *
     * @param commandBuilder the StoredValue command builder
     * @param svOperation the type of the current SV operation (Realod/Debit/Undebit)
     * @param svAction the SV action (do/undo)
     * @return the index to retrieve the parser later
     */
    int addStoredValueCommand(PoSvCommand commandBuilder, SvSettings.Operation svOperation,
            SvSettings.Action svAction) {
        /**
         * Reset the list if when preparing the first command after the last processing.
         * <p>
         * However, the parsers have remained available until now.
         */
        updateBuilderParserList();

        // check some logic around the SV commands:
        // SvGet Debit/Undo is
        if (commandBuilder instanceof SvGetCmdBuild) {
            // SvGet
            svGetIndex = preparedCommandIndex;
            this.svOperation = svOperation;
            this.svAction = svAction;
            /* not an SV Get command */
            lastCommandIsSvGet = true;
        } else {
            // SvReload, SvDebit or SvUndebit
            if (!poBuilderParserList.isEmpty()) {
                throw new IllegalStateException(
                        "This SV command can only be placed in the first position in the list of prepared commands");
            }

            if (!lastCommandIsSvGet) {
                /** @see Calypso Layer ID 8.07/8.08 (200108) */
                throw new IllegalStateException("This SV command must follow an SV Get command");
            }

            // here we expect that the builder and the SV operation are consistent
            if (svOperation != this.svOperation) {
                logger.error("Sv operation = {}, current command = {}", this.svOperation,
                        svOperation);
                throw new IllegalStateException("Inconsistent SV operation.");
            }
            if (secureSessionIsOpen && svOpIndex != -1) {
                /** @see Calypso Layer ID 8.03 (200108) */
                logger.error("Only one SV operation is allowed in a secure session");
                throw new IllegalStateException(
                        "Only one SV operation is allowed in a secure session");
            }
            svOpIndex = preparedCommandIndex;
            svOperationPending = true;
            this.svOperation = svOperation;
            lastCommandIsSvGet = false;
        }

        poBuilderParserList.add(new PoBuilderParser((AbstractPoCommandBuilder) commandBuilder));
        /* return and post-increment index */
        preparedCommandIndex++;
        return (preparedCommandIndex - 1);
    }

    /**
     * Informs that the commands have been processed.
     * <p>
     * Just record the information. The initialization of the list of commands will be done only the
     * next time a command is added, this allows access to the parsers contained in the list..
     */
    void notifyCommandsProcessed() {
        preparedCommandsProcessed = true;
    }

    /**
     * @return the current {@link SvSettings.Action} (default is DO)
     */
    public SvSettings.Action getSvAction() {
        return svAction;
    }

    /**
     * Indicates whether an SV (Reload/Debit) operation has been requested
     * 
     * @return true if a reload or debit command has been requested
     */
    public boolean isSvOperationPending() {
        return svOperationPending;
    }

    /**
     * @return the current PoBuilderParser list
     */
    public List<PoBuilderParser> getPoBuilderParserList() {
        /* here we make sure to clear the list if it has already been processed */
        updateBuilderParserList();
        return poBuilderParserList;
    }

    /**
     * Returns the parser positioned at the indicated index
     * 
     * @param commandIndex the index of the wanted parser
     * @return the parser
     */
    public AbstractApduResponseParser getResponseParser(int commandIndex) {
        if (commandIndex < 0 || commandIndex >= poBuilderParserList.size()) {
            throw new IllegalArgumentException(
                    String.format("Bad command index: index = %d, number of commands = %d",
                            commandIndex, poBuilderParserList.size()));
        }
        return poBuilderParserList.get(commandIndex).getResponseParser();
    }

    /**
     * Returns the SvGet parser index
     * 
     * @return the parser index as an int
     */
    public int getSvGetResponseParserIndex() {
        if (svGetIndex < 0 || svGetIndex != poBuilderParserList.size() - 1) {
            throw new IllegalStateException("No SvGet index is available");
        }
        return svGetIndex;
    }

    /**
     * Returns the Sv operation parser (reload, debit, undebit)
     *
     * @return the parser
     */
    public AbstractPoResponseParser getSvOperationResponseParser() {
        if (svOpIndex < 0 || svOpIndex >= poBuilderParserList.size()) {
            throw new IllegalStateException("Illegal SV operation parser index: " + svOpIndex);
        }
        return poBuilderParserList.get(svOpIndex).getResponseParser();
    }
}
