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
package org.eclipse.keyple.calypso.command.po.builder.security;

import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.AbstractPoCommandBuilder;
import org.eclipse.keyple.calypso.command.po.CalypsoPoCommands;
import org.eclipse.keyple.calypso.command.po.parser.security.VerifyPinRespPars;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;

public class VerifyPinCmdBuild extends AbstractPoCommandBuilder<VerifyPinRespPars> {
    private static final CalypsoPoCommands command = CalypsoPoCommands.VERIFY_PIN;

    /**
     * Verify the PIN
     *
     * @param poClass indicates which CLA byte should be used for the Apdu
     * @param pinOperation gives the type of operation: get the wrong presentation counter, send the
     *        PIN in clear or encrypted
     * @param pinData 0, 4 or 8 bytes of PIN data according to the {@link PinOperation} (can be null
     *        instead of an array of length 0)
     */
    public VerifyPinCmdBuild(PoClass poClass, PinOperation pinOperation, byte[] pinData) {
        super(command, null);

        if ((PinOperation.GET_COUNTER_ONLY.equals(pinOperation)
                && (pinData != null && pinData.length > 0))
                && (PinOperation.SEND_PLAIN_PIN.equals(pinOperation)
                        && (pinData == null || pinData.length != 4))
                && (PinOperation.SEND_PLAIN_PIN.equals(pinOperation)
                        && (pinData == null || pinData.length != 8))) {
            throw new IllegalArgumentException("Inconsistent PinOperation / PIN data.");
        }

        byte cla = poClass.getValue();
        byte p1 = (byte) 0x00;
        byte p2 = (byte) 0x00;

        this.request = setApduRequest(cla, command, p1, p2, pinData, null);
        this.addSubName("Verify PIN " + pinOperation);
    }

    @Override
    public VerifyPinRespPars createResponseParser(ApduResponse apduResponse) {
        return new VerifyPinRespPars(apduResponse);
    }
}
