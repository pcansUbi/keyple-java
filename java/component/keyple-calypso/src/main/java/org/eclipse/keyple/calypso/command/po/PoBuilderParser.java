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
package org.eclipse.keyple.calypso.command.po;

import org.eclipse.keyple.calypso.command.CalypsoBuilderParser;

/**
 * The PoBuilderParser class contains the builder of a {@link PoSendableInSession} command
 * <p>
 * A setter allows to associate the parser object.
 */
public class PoBuilderParser
        implements CalypsoBuilderParser<AbstractPoCommandBuilder, AbstractPoResponseParser> {
    private final AbstractPoCommandBuilder poCommandBuilder;
    private AbstractPoResponseParser poResponseParser;
    private boolean isSent;
    private final boolean isSplitCommand;
    private final SplitCommand splitCommand;

    public enum SplitCommand {
        NOT_SET, GET_CHALLENGE, VERIFY_PIN
    };

    public PoBuilderParser(AbstractPoCommandBuilder poCommandBuilder) {
        this.poCommandBuilder = poCommandBuilder;
        isSent = false;
        isSplitCommand = false;
        this.splitCommand = SplitCommand.NOT_SET;
    }

    public PoBuilderParser(AbstractPoCommandBuilder poCommandBuilder, SplitCommand splitCommand) {
        this.poCommandBuilder = poCommandBuilder;
        isSent = false;
        isSplitCommand = true;
        this.splitCommand = splitCommand;
    }

    public AbstractPoCommandBuilder getCommandBuilder() {
        return poCommandBuilder;
    }

    public AbstractPoResponseParser getResponseParser() {
        return poResponseParser;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent() {
        isSent = true;
    }

    public void setResponseParser(AbstractPoResponseParser poResponseParser) {
        this.poResponseParser = poResponseParser;
    }

    public boolean isSplitCommand() {
        return isSplitCommand;
    }

    public SplitCommand getSplitCommand() {
        return splitCommand;
    }
}
