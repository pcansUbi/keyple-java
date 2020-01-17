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
package org.eclipse.keyple.calypso.command.po.builder.security;



import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.parser.security.CloseSessionRespPars;
import org.eclipse.keyple.core.seproxy.message.ApduRequest;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloseSessionCmdBuildTest {

    @Test
    public void closeSessionCmdBuild() throws IllegalArgumentException {
        byte[] request2_4 = ByteArrayUtil.fromHex("948E000004A831C33E00");
        byte[] request3_1 = ByteArrayUtil.fromHex("008E800004A831C33E00");
        byte[] terminalSessionSignature = ByteArrayUtil.fromHex("A831C33E");
        CloseSessionCmdBuild closeSessionCmdBuild =
                new CloseSessionCmdBuild(PoClass.LEGACY, false, terminalSessionSignature);
        ApduRequest reqApdu = closeSessionCmdBuild.getApduRequest();

        Assert.assertArrayEquals(request2_4, reqApdu.getBytes());

        CloseSessionRespPars closeSessionCmdRespParser = closeSessionCmdBuild.createResponseParser(
                new ApduResponse(ByteArrayUtil.fromHex("04 001122 33445566 9000"), null));
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("001122"),
                closeSessionCmdRespParser.getPostponedData());
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("33445566"),
                closeSessionCmdRespParser.getSignatureLo());

        closeSessionCmdBuild =
                new CloseSessionCmdBuild(PoClass.ISO, true, terminalSessionSignature);
        reqApdu = closeSessionCmdBuild.getApduRequest();

        closeSessionCmdRespParser = closeSessionCmdBuild.createResponseParser(
                new ApduResponse(ByteArrayUtil.fromHex("04 001122 33445566 9000"), null));
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("001122"),
                closeSessionCmdRespParser.getPostponedData());
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("33445566"),
                closeSessionCmdRespParser.getSignatureLo());

        Assert.assertArrayEquals(request3_1, reqApdu.getBytes());
    }

    @Test
    public void closeSessionCmdBuild_abort() throws IllegalArgumentException {
        byte[] request2_4 = ByteArrayUtil.fromHex("948E000000");
        byte[] request3_1 = ByteArrayUtil.fromHex("008E000000");
        CloseSessionCmdBuild closeSessionCmdBuild = new CloseSessionCmdBuild(PoClass.LEGACY);
        ApduRequest reqApdu = closeSessionCmdBuild.getApduRequest();
        Assert.assertArrayEquals(request2_4, reqApdu.getBytes());
        closeSessionCmdBuild = new CloseSessionCmdBuild(PoClass.ISO);
        reqApdu = closeSessionCmdBuild.getApduRequest();
        Assert.assertArrayEquals(request3_1, reqApdu.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void closeSessionCmdBuild_bad_length_1() throws IllegalArgumentException {
        byte[] request3_1 = ByteArrayUtil.fromHex("008E800004A831C33E00");
        byte[] terminalSessionSignature = ByteArrayUtil.fromHex("A831C33E00");

        CloseSessionCmdBuild closeSessionCmdBuild =
                new CloseSessionCmdBuild(PoClass.ISO, true, terminalSessionSignature);
    }

    @Test(expected = IllegalArgumentException.class)
    public void closeSessionCmdBuild_bad_length_2() throws IllegalArgumentException {
        byte[] request3_1 = ByteArrayUtil.fromHex("008E800004A831C33E00");
        byte[] terminalSessionSignature = ByteArrayUtil.fromHex("A831C33E");

        CloseSessionCmdBuild closeSessionCmdBuild =
                new CloseSessionCmdBuild(PoClass.ISO, true, terminalSessionSignature);
        ApduRequest reqApdu = closeSessionCmdBuild.getApduRequest();

        CloseSessionRespPars closeSessionCmdRespParser = closeSessionCmdBuild.createResponseParser(
                new ApduResponse(ByteArrayUtil.fromHex("04 001122 33445566 77 9000"), null));
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("001122"),
                closeSessionCmdRespParser.getPostponedData());
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("33445566"),
                closeSessionCmdRespParser.getSignatureLo());

        Assert.assertArrayEquals(request3_1, reqApdu.getBytes());
    }
}
