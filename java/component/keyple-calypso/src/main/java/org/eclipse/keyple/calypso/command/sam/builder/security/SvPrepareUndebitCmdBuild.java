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
package org.eclipse.keyple.calypso.command.sam.builder.security;

import org.eclipse.keyple.calypso.command.sam.AbstractSamCommandBuilder;
import org.eclipse.keyple.calypso.command.sam.CalypsoSamCommands;

/**
 * Builder for the SAM SV Undebit APDU command.
 */
public class SvPrepareUndebitCmdBuild extends AbstractSamCommandBuilder {
    /** The command reference. */
    private static final CalypsoSamCommands command = CalypsoSamCommands.SV_PREPARE_UNDEBIT;

    /**
     * Instantiates a new SvPrepareUndebitCmdBuild to prepare a transaction to cancel a previous
     * debit transaction.
     *
     */
    public SvPrepareUndebitCmdBuild() {
        super(command, null);

        byte cla = this.defaultRevision.getClassByte();

        byte p1, p2;
        byte[] data = null;

        p1 = (byte) 0x00;
        p2 = (byte) 0x00;

        request = setApduRequest(cla, command, p1, p2, data, null);
    }
}
