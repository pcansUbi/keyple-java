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

import static org.junit.Assert.*;
import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.PoRevision;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Test;

public class SvDebitCmdBuildTest {
    @Test
    public void svDebitCmdBuild_mode_compat_base() {
        /* */
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00BA55661477FFFF11223344AAAABBCCDD1234561122334455", apdu);
    }

    @Test
    public void svDebitCmdBuild_mode_compat_4081() {
        /* */
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ 4081, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00BA55661477F00F11223344AAAABBCCDD1234561122334455", apdu);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svDebitCmdBuild_mode_compat_negative_amount() {
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ -1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svDebitCmdBuild_mode_compat_overlimit_amount() {
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ 32768, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svDebitCmdBuild_mode_compat_bad_signature_length_1() {
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svDebitCmdBuild_mode_compat_bad_signature_length_2() {
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("112233"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
    }

    @Test
    public void svDebitCmdBuild_mode_rev3_2_base() {
        /* */
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00BA55661977FFFF11223344AAAABBCCDD12345611223344556677889900", apdu);
    }

    @Test
    public void svDebitCmdBuild_mode_rev3_2_4081() {
        /* */
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 4081, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("11223344556677889900"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
        Assert.assertEquals("00BA55661977F00F11223344AAAABBCCDD12345611223344556677889900", apdu);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svDebitCmdBuild_mode_rev3_2_negative_amount() {
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ -1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svDebitCmdBuild_mode_rev3_2_overlimit_amount() {
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 32768, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svDebitCmdBuild_mode_rev3_2_bad_signature_length_1() {
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("1122334455"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void svDebitCmdBuild_mode_rev3_2_bad_signature_length_2() {
        SvDebitCmdBuild svDebitCmdBuild = new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                /* amount */ 1, /* date */ ByteArrayUtil.fromHex("1122"),
                /* time */ ByteArrayUtil.fromHex("3344"),
                /* challenge */ ByteArrayUtil.fromHex("556677"), /* KVC */ (byte) 0xAA,
                /* SAM ID */ ByteArrayUtil.fromHex("AABBCCDD"),
                /* SAM TNum */ ByteArrayUtil.fromHex("123456"),
                /* Signature Hi */ ByteArrayUtil.fromHex("112233"),
                new Exception().getStackTrace()[0].getMethodName());
        String apdu = ByteArrayUtil.toHex(svDebitCmdBuild.getApduRequest().getBytes());
    }
}
