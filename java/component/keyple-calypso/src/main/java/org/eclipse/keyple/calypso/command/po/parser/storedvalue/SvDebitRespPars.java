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
package org.eclipse.keyple.calypso.command.po.parser.storedvalue;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.keyple.calypso.command.po.AbstractPoResponseParser;
import org.eclipse.keyple.core.command.AbstractApduResponseParser;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;

/**
 * SV Get (00BA) response parser. See specs: Calypso Stored Value balance (signed binaries' coding
 * based on the two's complement method)
 * 
 * <p>
 * balance - 3 bytes signed binary - Integer from -8,388,608 to 8,388,607
 * 
 * <pre>
    -8,388,608           %10000000.00000000.00000000
    -8,388,607           %10000000.00000000.00000001
    -8,388,606           %10000000.00000000.00000010

            -3           %11111111.11111111.11111101
            -2           %11111111.11111111.11111110
            -1           %11111111.11111111.11111111
             0           %00000000.00000000.00000000
             1           %00000000.00000000.00000001
             2           %00000000.00000000.00000010
             3           %00000000.00000000.00000011

     8,388,605           %01111111.11111111.11111101
     8,388,606           %01111111.11111111.11111110
     8,388,607           %01111111.11111111.11111111
 * </pre>
 * 
 * amount - 2 bytes signed binary
 * <p>
 * amount for debit - Integer 0..32768 =&gt; for negative value
 * 
 * <pre>
        -32768           %10000000.00000000
        -32767           %10000000.00000001
        -32766           %10000000.00000010
            -3           %11111111.11111101
            -2           %11111111.11111110
            -1           %11111111.11111111
             0           %00000000.00000000
 * </pre>
 */
public final class SvDebitRespPars extends AbstractPoResponseParser {

    private static final Map<Integer, StatusProperties> STATUS_TABLE;

    static {
        Map<Integer, StatusProperties> m =
                new HashMap<Integer, StatusProperties>(AbstractApduResponseParser.STATUS_TABLE);
        m.put(0x6400, new StatusProperties(false, "Session memory is full."));
        m.put(0x6700, new StatusProperties(false, "Lc value not supported."));
        m.put(0x6900, new StatusProperties(false,
                "Transaction counter is 0 or SV TNum is FFFEh or FFFFh."));
        m.put(0x6985, new StatusProperties(false, "Conditions for use not satisfied."));
        m.put(0x6988, new StatusProperties(false, "Incorrect SignatureHi."));
        m.put(0x6200, new StatusProperties(true,
                "Successful execution, response data postponed until session closing" + "."));
        m.put(0x9000, new StatusProperties(true, "Successful execution."));
        STATUS_TABLE = m;
    }

    /**
     * Constructor to build a parser of the SvDebit command response.
     *
     * @param response response to parse
     */
    public SvDebitRespPars(ApduResponse response) {
        super(response);
    }

    @Override
    protected Map<Integer, StatusProperties> getStatusTable() {
        return STATUS_TABLE;
    }


    @Override
    public String toString() {
        return String.format("SV Debit");
    }
}
