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
import org.eclipse.keyple.calypso.command.po.parser.security.OpenSession10RespPars;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Test;

public class OpenSession10CmdBuildTest {
    @Test
    public void openSession10CmdBuild() {
        OpenSession10CmdBuild openSession10CmdBuild = new OpenSession10CmdBuild((byte) 0x01,
                ByteArrayUtil.fromHex("11223344"), (byte) 0x02, (byte) 0x03,
                new Exception().getStackTrace()[0].getMethodName());

        byte[] arr = openSession10CmdBuild.getApduRequest().getBytes();
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("948A1910041122334400"),
                openSession10CmdBuild.getApduRequest().getBytes());

        OpenSession10RespPars openSession10CmdRespParser =
                openSession10CmdBuild.createResponseParser(
                        new ApduResponse(ByteArrayUtil.fromHex("44332211 9000"), null));
        // TODO add more checks here
    }
}
