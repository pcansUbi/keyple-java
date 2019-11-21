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
package org.eclipse.keyple.calypso.command.po.builder.storedvalue;

import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.PoRevision;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Test;

public class SvReloadCmdBuildTest {
    @Test
    public void svReloadCmdBuild_mode_compat_base() {
        /* */
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svReloadCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00B8556617771122F3AAEE0000013344AABBCCDD1234561122334455", apdu);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svReloadCmdBuild_mode_compat_overlimit_negative_amount() {
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ -8388609, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svReloadCmdBuild_mode_compat_overlimit_positive_amount() {
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ 8388608, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svReloadCmdBuild_mode_compat_bad_signature_length_1() {
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svReloadCmdBuild_mode_compat_bad_signature_length_2() {
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("112233"),
                new Exception().getStackTrace()[0].getMethodName());
    }

    @Test
    public void svReloadCmdBuild_mode_rev3_2_base() {
        /* */
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svReloadCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00B855661C771122F3AAEE0000013344AABBCCDD12345611223344556677889900",
                apdu);
    }

    @Test
    public void svReloadCmdBuild_mode_rev3_2_amout_256() {
        /* */
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 256, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svReloadCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00B855661C771122F3AAEE0001003344AABBCCDD12345611223344556677889900",
                apdu);
    }

    @Test
    public void svReloadCmdBuild_mode_rev3_2_amout_65536() {
        /* */
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 65536, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svReloadCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00B855661C771122F3AAEE0100003344AABBCCDD12345611223344556677889900",
                apdu);
    }

    @Test
    public void svReloadCmdBuild_mode_rev3_2_amout_m1() {
        /* */
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ -1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svReloadCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00B855661C771122F3AAEEFFFFFF3344AABBCCDD12345611223344556677889900",
                apdu);
    }

    @Test
    public void svReloadCmdBuild_mode_rev3_2_amout_m256() {
        /* */
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ -256, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svReloadCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00B855661C771122F3AAEEFFFF003344AABBCCDD12345611223344556677889900",
                apdu);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svReloadCmdBuild_mode_rev3_2_overlimit_negative_amount() {
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ -8388609, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svReloadCmdBuild_mode_rev3_2_overlimit_positive_amount() {
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 8388608, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svReloadCmdBuild_mode_rev3_2_bad_signature_length_1() {
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svReloadCmdBuild_mode_rev3_2_bad_signature_length_2() {
        SvReloadCmdBuild svReloadCmdBuild = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"), /* free */ ByteArrayUtil.fromHex("F3EE"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122"),
                new Exception().getStackTrace()[0].getMethodName());
    }
}
