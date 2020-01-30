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

import org.eclipse.keyple.calypso.command.CalypsoBuilderParser;
import org.eclipse.keyple.calypso.command.po.AbstractPoCommandBuilder;
import org.eclipse.keyple.calypso.command.po.AbstractPoResponseParser;
import org.eclipse.keyple.calypso.command.po.PoSendableInSession;

/**
 * The PoCommand class contains the builder of a {@link PoSendableInSession} command
 * <p>
 * A setter allows to associate the parser object.
 */
class PoCommand {
    private AbstractPoCommandBuilder poCommandBuilder;
    private AbstractPoResponseParser poResponseParser;
    private boolean isSent;

    /** Indicates the command that requires the request to be split. */
    public enum SplitCommandInfo {
        NOT_SET, VERIFY_PIN
    }

    /** Empty constructor */
    public PoCommand()  {
    }

    /**
     * Constructor for regular commands
     * 
     * @param poCommandBuilder the command builder to be stored
     */
    public PoCommand(AbstractPoCommandBuilder poCommandBuilder) {
        this.poCommandBuilder = poCommandBuilder;
        isSent = false;
    }

    /**
     * Constructor for splitting request commands
     * 
     * @param poCommandBuilder the command builder to be stored
     * @param splitCommandInfo the split command identifier
     */
    public PoCommand(AbstractPoCommandBuilder poCommandBuilder,
                     SplitCommandInfo splitCommandInfo) {
        this.poCommandBuilder = poCommandBuilder;
        isSent = false;
    }

    /**
     * @return the builder
     */
    public AbstractPoCommandBuilder getCommandBuilder() {
        return poCommandBuilder;
    }

    /**
     * @return the parser
     */
    public AbstractPoResponseParser getResponseParser() {
        return poResponseParser;
    }

    /**
     * @return true if the command has been sent
     */
    public boolean isSent() {
        return isSent;
    }

    /**
     * Sets the sent status
     */
    public void setSent() {
        isSent = true;
    }

    /**
     * Sets the response parser when available
     * 
     * @param poResponseParser the response parser
     */
    public void setResponseParser(AbstractPoResponseParser poResponseParser) {
        this.poResponseParser = poResponseParser;
    }

    /**
     * @return true if the current command requires the split of the request
     */
    public boolean isSplitCommand() {
        return false;
    }

    /**
     * @return the identification of the command that requires the split of the request
     */
    public SplitCommandInfo getSplitCommandInfo() {
        return SplitCommandInfo.NOT_SET;
    }
}
