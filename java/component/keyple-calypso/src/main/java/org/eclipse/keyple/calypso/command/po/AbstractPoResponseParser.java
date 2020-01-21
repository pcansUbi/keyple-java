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

import org.eclipse.keyple.calypso.PoData;
import org.eclipse.keyple.calypso.PoVoid;
import org.eclipse.keyple.core.command.AbstractApduResponseParser;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;

public abstract class AbstractPoResponseParser extends AbstractApduResponseParser {
    /**
     * the generic abstract constructor to build a parser of the APDU response.
     *
     * @param response response to parse
     */
    public AbstractPoResponseParser(ApduResponse response) {
        super(response);
    }

    /**
     * The method returns the data involved in the current command.
     * <p>
     * It must be overloaded by all commands that read or modify the PO's non-volatile memory.
     * 
     * @return an object implementing the {@link PoData} interface
     */
    public PoData getPoData() {
        return new PoVoid();
    }
}
