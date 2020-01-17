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

import static org.junit.Assert.*;
import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.parser.security.ChangeKeyRespPars;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Test;

public class ChangeKeyCmdBuildTest {
    @Test
    public void changeKeyCmdBuild_base() {
        ChangeKeyCmdBuild changeKeyCmdBuild = new ChangeKeyCmdBuild(PoClass.ISO, (byte) 0x01,
                ByteArrayUtil.fromHex("001122334455 001122334455 001122334455 001122334455"));
        Assert.assertArrayEquals(
                ByteArrayUtil
                        .fromHex("00D80001 18 001122334455 001122334455 001122334455 001122334455"),
                changeKeyCmdBuild.getApduRequest().getBytes());
        ChangeKeyRespPars createResponseParser = changeKeyCmdBuild
                .createResponseParser(new ApduResponse(ByteArrayUtil.fromHex("9000"), null));
        Assert.assertTrue(createResponseParser.isSuccessful());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeKeyCmdBuild_bad_length() {
        ChangeKeyCmdBuild changeKeyCmdBuild = new ChangeKeyCmdBuild(PoClass.ISO, (byte) 0x01,
                ByteArrayUtil.fromHex("001122334455 001122334455"));
    }
}
