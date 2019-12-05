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
package org.eclipse.keyple.calypso.command.po.builder.storedvalue;


import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.*;
import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvGetRespPars;
import org.eclipse.keyple.calypso.transaction.SvOperation;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;

/**
 * The Class SvGetCmdBuild. This class provides the dedicated constructor to build the SV Get
 * command.
 */
public final class SvGetCmdBuild extends AbstractPoCommandBuilder<SvGetRespPars>
        implements PoSendableInSession, PoModificationCommand {

    /** The command. */
    private static final CalypsoPoCommands command = CalypsoPoCommands.SV_GET;

    private final byte[] header;

    /**
     * Instantiates a new SvGetCmdBuild.
     *
     * @param poClass the PO class
     * @param poRevision the PO revision
     * @param svOperation the desired SV operation
     * @param extraInfo extra information included in the logs (can be null or empty)
     * @throws IllegalArgumentException - if the command is inconsistent
     */
    public SvGetCmdBuild(PoClass poClass, PoRevision poRevision, SvOperation svOperation, String extraInfo) {
        super(command, null);
        byte cla = poClass.getValue();
        byte p1 = poRevision == PoRevision.REV3_2 ? (byte) 0x01 : (byte) 0x00;
        byte p2 = svOperation == SvOperation.RELOAD ? (byte) 0x07 : (byte) 0x09;

        this.request = setApduRequest(cla, command, p1, p2, null, (byte) 0x00);
        if (extraInfo != null) {
            this.addSubName(extraInfo);
        }
        header = new byte[4];
        header[0] = command.getInstructionByte();
        header[1] = p1;
        header[2] = p2;
        header[3] = (byte) 0x00;
    }

    @Override
    public SvGetRespPars createResponseParser(ApduResponse apduResponse) {
        return new SvGetRespPars(header, apduResponse);
    }
}
