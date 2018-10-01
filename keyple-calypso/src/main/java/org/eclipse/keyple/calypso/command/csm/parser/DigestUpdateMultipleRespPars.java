/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.calypso.command.csm.parser;


import org.eclipse.keyple.command.AbstractApduResponseParser;
import org.eclipse.keyple.seproxy.ApduResponse;

/**
 * Digest update multiple response parser. See specs: Calypso / page 54 / 7.4.2 - Session MAC
 * computation
 */
public class DigestUpdateMultipleRespPars extends AbstractApduResponseParser {
    /**
     * Instantiates a new DigestUpdateMultipleRespPars.
     *
     * @param response the response
     */
    public DigestUpdateMultipleRespPars(ApduResponse response) {
        super(response);
    }
}