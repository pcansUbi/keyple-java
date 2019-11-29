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
import org.eclipse.keyple.calypso.command.po.PoBuilderParser;
import org.eclipse.keyple.calypso.command.po.builder.*;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvGetCmdBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PO command manager is used to keep builders and parsers between the time the commands are
 * created and the time the parsers have been used.
 * <p>
 * A flag (preparedCommandsProcessed) is used to manage the reset of the command list. It allows the
 * builders to be kept until the application creates a new list of commands.
 */
public class PoCommandsManager {
    /* logger */
    private static final Logger logger = LoggerFactory.getLogger(PoCommandsManager.class);

    /** The list to contain the prepared commands and their parsers */
    private final List<PoBuilderParser> poBuilderParserList = new ArrayList<PoBuilderParser>();
    /** The command index, incremented each time a command is added */
    private int preparedCommandIndex;
    private boolean preparedCommandsProcessed;
    private SvOperation svOperation = SvOperation.NONE;

    PoCommandsManager() {
        preparedCommandsProcessed = true;
    }

    /**
     * Add a regular command to the builders and parsers list.
     * <p>
     * Handle the clearing of the list if needed.
     * 
     * @param commandBuilder the command builder
     * @return the index to retrieve the parser later
     */
    int addRegularCommand(AbstractPoCommandBuilder commandBuilder) {
        /*
         * Reset the list when preparing the first command after the last processing. The builders
         * have remained available until now.
         */
        if (preparedCommandsProcessed) {
            poBuilderParserList.clear();
            preparedCommandsProcessed = false;
            preparedCommandIndex = 0;
        }

        poBuilderParserList.add(new PoBuilderParser(commandBuilder));
        /* return and post-increment index */
        preparedCommandIndex++;
        return (preparedCommandIndex - 1);
    }


    /**
     * Add a StoredValue command to the builders and parsers list.
     * <p>
     * Handle the clearing of the list if needed.
     * 
     * @param commandBuilder the StoredValue command builder TODO // check the need for a specific
     *        interface
     * @param svOperation the type of SV operation
     * @return the index to retrieve the parser later
     */
    int addStoredValueCommand(AbstractPoCommandBuilder commandBuilder, SvOperation svOperation) {
        /*
         * Reset the list when preparing the first command after the last processing. The builders
         * have remained available until now.
         */
        // TODO find a way to efficiently mutualize this with the addRegularCommand method
        if (preparedCommandsProcessed) {
            poBuilderParserList.clear();
            preparedCommandsProcessed = false;
            preparedCommandIndex = 0;
        }

        // check some logic around the SV commands
        if (commandBuilder instanceof SvGetCmdBuild) {
            // SvGet
            if (this.svOperation != SvOperation.NONE) {
                throw new IllegalStateException("Only one SV operation per session is allowed.");
            }
        } else {
            // SvReload, SvDebit or SvUndebit
            if (!poBuilderParserList.isEmpty()) {
                throw new IllegalStateException(
                        "This SV command can only be placed in the first position in the list of prepared commands");
            }

            // TODO Improve this: here we expect that the builder and the SV operation are
            // consistent
            if (svOperation != this.svOperation) {
                logger.error("SvGet operation = {}, current command = {}", this.svOperation,
                        svOperation);
                throw new IllegalStateException("Inconsistent SV operation.");
            }
        }
        this.svOperation = svOperation;

        // TODO find a way to efficiently mutualize this with the addRegularCommand method
        poBuilderParserList.add(new PoBuilderParser(commandBuilder));
        /* return and post-increment index */
        preparedCommandIndex++;
        return (preparedCommandIndex - 1);
    }

    /**
     * Keeps information that commands have been processed.
     * <p>
     * Allows the delayed initialization of the command list.
     */
    void notifyCommandsProcessed() {
        preparedCommandsProcessed = true;
    }

    /**
     * @return the current PoBuilderParser list
     */
    public List<PoBuilderParser> getPoBuilderParserList() {
        return poBuilderParserList;
    }
}
