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
package org.eclipse.keyple.calypso.command.po.builder;

import static org.junit.Assert.*;
import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.parser.DecreaseRespPars;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Test;

public class DecreaseCmdBuildTest {

    @Test
    public void decreaseCmdBuild_base() {
        byte sfi = (byte) 0x01;
        byte counter = (byte) 0x01;
        int decval = 1;
        DecreaseCmdBuild decreaseCmdBuild = new DecreaseCmdBuild(PoClass.ISO, sfi, counter, decval,
                new Exception().getStackTrace()[0].getMethodName());

        Assert.assertArrayEquals(ByteArrayUtil.fromHex("003001080300000100"),
                decreaseCmdBuild.getApduRequest().getBytes());
        DecreaseRespPars decreaseCmdRespParser = decreaseCmdBuild
                .createResponseParser(new ApduResponse(ByteArrayUtil.fromHex("001122 9000"), null));
        Assert.assertEquals(0x1122, decreaseCmdRespParser.getNewValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void decreaseCmdBuild_bad_counter() {
        byte sfi = (byte) 0x01;
        byte counter = (byte) 0x00;
        int decval = 1;
        DecreaseCmdBuild decreaseCmdBuild = new DecreaseCmdBuild(PoClass.ISO, sfi, counter, decval,
                new Exception().getStackTrace()[0].getMethodName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void decreaseCmdBuild_bad_value_1() {
        // negative dec value
        byte sfi = (byte) 0x01;
        byte counter = (byte) 0x01;
        int decval = -1;
        DecreaseCmdBuild decreaseCmdBuild = new DecreaseCmdBuild(PoClass.ISO, sfi, counter, decval,
                new Exception().getStackTrace()[0].getMethodName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void decreaseCmdBuild_bad_value_2() {
        // dec value > 0xFFFFFF
        byte sfi = (byte) 0x01;
        byte counter = (byte) 0x01;
        int decval = 0xFFFFFF + 1;
        DecreaseCmdBuild decreaseCmdBuild = new DecreaseCmdBuild(PoClass.ISO, sfi, counter, decval,
                new Exception().getStackTrace()[0].getMethodName());
    }
}
