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

public class SvGetCmdBuildTest {
    @Test
    public void svGetCmdBuild_mode_rev32_reload() {
        SvGetCmdBuild svGetCmdBuild = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                SvGetCmdBuild.SvOperation.RELOAD, "Test 1");

        String cmdBytes = ByteArrayUtil.toHex(svGetCmdBuild.getApduRequest().getBytes());

        Assert.assertEquals("007C000700", cmdBytes);
    }

    @Test
    public void svGetCmdBuild_mode_rev32_debit() {
        SvGetCmdBuild svGetCmdBuild = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                SvGetCmdBuild.SvOperation.DEBIT, "Test 2");

        String cmdBytes = ByteArrayUtil.toHex(svGetCmdBuild.getApduRequest().getBytes());

        Assert.assertEquals("007C000900", cmdBytes);
    }

    @Test
    public void svGetCmdBuild_mode_rev32_undebit() {
        SvGetCmdBuild svGetCmdBuild = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                SvGetCmdBuild.SvOperation.UNDEBIT, "Test 2");

        String cmdBytes = ByteArrayUtil.toHex(svGetCmdBuild.getApduRequest().getBytes());

        Assert.assertEquals("007C000900", cmdBytes);
    }

    @Test
    public void svGetCmdBuild_mode_compat_reload() {
        SvGetCmdBuild svGetCmdBuild = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                SvGetCmdBuild.SvOperation.RELOAD, "Test 4");

        String cmdBytes = ByteArrayUtil.toHex(svGetCmdBuild.getApduRequest().getBytes());

        Assert.assertEquals("007C010700", cmdBytes);
    }

    @Test
    public void svGetCmdBuild_mode_compat_debit() {
        SvGetCmdBuild svGetCmdBuild = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                SvGetCmdBuild.SvOperation.DEBIT, "Test 5");

        String cmdBytes = ByteArrayUtil.toHex(svGetCmdBuild.getApduRequest().getBytes());

        Assert.assertEquals("007C010900", cmdBytes);
    }

    @Test
    public void svGetCmdBuild_mode_compat_undebit() {
        SvGetCmdBuild svGetCmdBuild = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_2,
                SvGetCmdBuild.SvOperation.UNDEBIT, "Test 6");

        String cmdBytes = ByteArrayUtil.toHex(svGetCmdBuild.getApduRequest().getBytes());

        Assert.assertEquals("007C010900", cmdBytes);
    }
}
