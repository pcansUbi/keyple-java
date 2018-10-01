/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.example.pc.generic.stub.se;

import java.nio.ByteBuffer;
import org.eclipse.keyple.plugin.stub.StubSecureElement;
import org.eclipse.keyple.util.ByteBufferUtils;

/**
 * Simple contact stub SE (no command)
 */
public class StubMifareUL extends StubSecureElement {

    final static String seProtocol = "PROTOCOL_MIFARE_UL";
    final String ATR_HEX = "3B8F8001804F0CA0000003060300030000000068";

    public StubMifareUL() {
        /* Get data */
        addHexCommand("FFCA 000000", "223344556677889000");
    }

    @Override
    public ByteBuffer getATR() {
        return ByteBufferUtils.fromHex(ATR_HEX);
    }

    @Override
    public String getSeProcotol() {
        return seProtocol;
    }


}