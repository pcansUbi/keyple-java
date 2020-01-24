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

    private final byte cla;
    private final byte[] pin = new byte[4];

    /**
     * Verify the PIN
     *
     * @param poClass indicates which CLA byte should be used for the Apdu
     * @param pinOperation gives the type of operation: get the wrong presentation counter, send the
     *        PIN in clear or encrypted
     * @param pin 0, 4 or of PIN data according to the {@link PinOperation}. When provided, the PIN
     *        is always 4-byte long here, even in the case of a encrypted transmission (@see
     *        setCipheredPinData). (pin can also be null instead of an array of length 0)
     */
    public VerifyPinCmdBuild(PoClass poClass, PinOperation pinOperation, byte[] pin) {
        super(command, null);

        if (PinOperation.GET_COUNTER_ONLY.equals(pinOperation) && (pin != null && pin.length > 0)) {
            throw new IllegalArgumentException("No PIN data is required to get the counter");
        }

        if ((PinOperation.SEND_PLAIN_PIN.equals(pinOperation)
                || PinOperation.SEND_ENCRYPTED_PIN.equals(pinOperation))
                && (pin == null || pin.length != 4)) {
            throw new IllegalArgumentException("The PIN must be 4 bytes long");
        }

        cla = poClass.getValue();

        if (PinOperation.SEND_ENCRYPTED_PIN.equals(pinOperation)) {
            // only keep the pin
            System.arraycopy(pin, 0, this.pin, 0, 4);
        } else {
            byte p1 = (byte) 0x00;
            byte p2 = (byte) 0x00;

            this.request = setApduRequest(cla, command, p1, p2, pin, null);
            this.addSubName(pinOperation.toString());
        }
    }

    /**
     * @return the value of the PIN stored at the time of construction
     */
    public byte[] getPin() {
        return pin;
    }

    /**
     * Finalizes the builder in the case of an encrypted transmission
     * 
     * @param pinData the encrypted PIN
     */
    public void setCipheredPinData(byte[] pinData) {
        if (pinData == null || pinData.length != 8) {
            throw new IllegalArgumentException("Wrong length of the PIN encrypted data");
        }
        byte p1 = (byte) 0x00;
        byte p2 = (byte) 0x00;

        this.request = setApduRequest(cla, command, p1, p2, pinData, null);
        this.addSubName(PinOperation.SEND_ENCRYPTED_PIN.toString());
    }

    @Override
    public VerifyPinRespPars createResponseParser(ApduResponse apduResponse) {
        return new VerifyPinRespPars(apduResponse);
    }
}
