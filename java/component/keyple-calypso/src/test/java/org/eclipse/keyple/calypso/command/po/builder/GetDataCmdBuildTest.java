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
package org.eclipse.keyple.calypso.command.po.builder;


import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.parser.GetDataFciRespPars;
import org.eclipse.keyple.core.command.AbstractApduResponseParser;
import org.eclipse.keyple.core.seproxy.message.ApduRequest;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GetDataCmdBuildTest {

    @Test
    public void getDataFCICmdBuild() {
        byte[] request = ByteArrayUtil.fromHex("94CA006F00");
        GetDataFciCmdBuild getDataFciCmdBuild = new GetDataFciCmdBuild(PoClass.LEGACY);
        ApduRequest apduReq = getDataFciCmdBuild.getApduRequest();
        Assert.assertArrayEquals(request, apduReq.getBytes());
        AbstractApduResponseParser abstractApduResponseParser =
                getDataFciCmdBuild.createResponseParser(new ApduResponse(ByteArrayUtil.fromHex(
                        "6F 24 84 0A 00112233445566778899 A5 16 BF0C 13 C7 08 AABBCCDDEEFF0011 53 07 0B55AA55AA55AA9000"),
                        null));
        Assert.assertTrue(abstractApduResponseParser instanceof GetDataFciRespPars);
    }


    @Test
    public void getDataFCICmdBuild2() {
        byte[] request2 = ByteArrayUtil.fromHex("00CA006F00");
        GetDataFciCmdBuild getDataFciCmdBuild = new GetDataFciCmdBuild(PoClass.ISO);
        ApduRequest apduReq = getDataFciCmdBuild.getApduRequest();
        Assert.assertArrayEquals(request2, apduReq.getBytes());
        AbstractApduResponseParser abstractApduResponseParser =
                getDataFciCmdBuild.createResponseParser(new ApduResponse(ByteArrayUtil.fromHex(
                        "6F 24 84 0A 00112233445566778899 A5 16 BF0C 13 C7 08 AABBCCDDEEFF0011 53 07 0B55AA55AA55AA9000"),
                        null));
        Assert.assertTrue(abstractApduResponseParser instanceof GetDataFciRespPars);
    }
}
