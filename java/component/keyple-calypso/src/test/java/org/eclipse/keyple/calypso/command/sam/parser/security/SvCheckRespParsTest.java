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
package org.eclipse.keyple.calypso.command.sam.parser.security;

import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Test;

public class SvCheckRespParsTest {
    @Test
    public void svCheckParse_sucessful() {
        ApduResponse apduResponse = new ApduResponse(ByteArrayUtil.fromHex("9000"), null);
        SvCheckRespPars svDebitRespPars = new SvCheckRespPars(apduResponse);
        Assert.assertEquals(true, svDebitRespPars.getApduResponse().isSuccessful());
    }

    @Test
    public void svCheckParse_failed() {
        ApduResponse apduResponse = new ApduResponse(ByteArrayUtil.fromHex("6988"), null);
        SvCheckRespPars svDebitRespPars = new SvCheckRespPars(apduResponse);
        Assert.assertEquals(false, svDebitRespPars.getApduResponse().isSuccessful());
    }
}
