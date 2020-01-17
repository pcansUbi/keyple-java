/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.calypso.transaction;

import static org.junit.Assert.*;
import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvGetRespPars;
import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoSvException;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SvGetPoResponseTest {
    byte[] header;

    @Before
    public void SetUp() {
        header = ByteArrayUtil.fromHex("7C000721");
    }

    @Test
    public void singleLoadParser() throws KeypleCalypsoSvException {
        ApduResponse apduResponse =
                new ApduResponse(ByteArrayUtil.fromHex(/* Current KVC (1) */ "55" +
                /* SV TNum (2) */ "A55A" +
                /* Previous SignatureLo (6) */ "665544" +
                /* Challenge out */ "1122" +
                /* SV Balance (3) */ "000000" +
                /* Load Date (2) */ "1234" +
                /* Load Free1 (1) */ "22" +
                /* Load KVC (1) */ "AA" +
                /* Load Free2 (1) */ "33" +
                /* Load Balance (3) */ "001121" +
                /* Load Amount (3) */ "000001" +
                /* Load Time (2) */ "5678" +
                /* Load SAM ID (4) */ "AABBCCDD" +
                /* Load SAM TNum (3) */ "D23456" +
                /* Load SV TNum (2) */ "E567" +
                /* Successful execution status word */ "9000"), null);

        SvGetRespPars svGetLoadRespPars = new SvGetRespPars(header, apduResponse);

        SvGetPoResponse svGetPoResponse = new SvGetPoResponse(svGetLoadRespPars);
        Assert.assertTrue(svGetPoResponse.isLoadLogAvailable());
        Assert.assertNotNull(svGetPoResponse.getLoadLog());
        Assert.assertFalse(svGetPoResponse.isDebitLogAvailable());
        Assert.assertEquals(0, svGetPoResponse.getBalance());
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("AABBCCDD"),
                svGetPoResponse.getLoadLog().getSamID());
        Assert.assertEquals(0x1121, svGetPoResponse.getLoadLog().getBalance());
    }

    @Test
    public void singleDebitParser() throws KeypleCalypsoSvException {
        ApduResponse apduResponse =
                new ApduResponse(ByteArrayUtil.fromHex(/* Current KVC (1) */ "55" +
                /* SV TNum (2) */ "A55A" +
                /* Previous SignatureLo (6) */ "665544" +
                /* Challenge out */ "1122" +
                /* SV Balance (3) */ "000000" +
                /* Debit Amount (2) */ "0001" +
                /* Debit Date (2) */ "1235" +
                /* Debit Time (2) */ "6789" +
                /* Debit KVC (1) */ "BB" +
                /* Debit SAM ID (4) */ "BBCCDDEE" +
                /* Debit SAM TNum (3) */ "A34567" +
                /* Debit Balance (3) */ "001120" +
                /* Debit SV TNum (2) */ "F568" +
                /* Successful execution status word */ "9000"), null);

        SvGetRespPars svGetDebitRespPars = new SvGetRespPars(header, apduResponse);

        SvGetPoResponse svGetPoResponse = new SvGetPoResponse(svGetDebitRespPars);
        Assert.assertTrue(svGetPoResponse.isDebitLogAvailable());
        Assert.assertNotNull(svGetPoResponse.getDebitLog());
        Assert.assertFalse(svGetPoResponse.isLoadLogAvailable());
        Assert.assertEquals(0, svGetPoResponse.getBalance());
        Assert.assertEquals((byte) 0x55, svGetPoResponse.getCurrentKVC());
        Assert.assertEquals(0xA55A, svGetPoResponse.getTransactionNumber());
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("BBCCDDEE"),
                svGetPoResponse.getDebitLog().getSamID());
        Assert.assertEquals(0x1120, svGetPoResponse.getDebitLog().getBalance());
    }

    @Test
    public void doubleLoadDebitParser() throws KeypleCalypsoSvException {
        ApduResponse apduResponse =
                new ApduResponse(ByteArrayUtil.fromHex(/* Current KVC (1) */ "55" +
                /* SV TNum (2) */ "A55A" +
                /* Previous SignatureLo (6) */ "665544" +
                /* Challenge out */ "1122" +
                /* SV Balance (3) */ "000000" +
                /* Load Date (2) */ "1234" +
                /* Load Free1 (1) */ "22" +
                /* Load KVC (1) */ "AA" +
                /* Load Free2 (1) */ "33" +
                /* Load Balance (3) */ "001121" +
                /* Load Amount (3) */ "000001" +
                /* Load Time (2) */ "5678" +
                /* Load SAM ID (4) */ "AABBCCDD" +
                /* Load SAM TNum (3) */ "D23456" +
                /* Load SV TNum (2) */ "E567" +
                /* Successful execution status word */ "9000"), null);

        SvGetRespPars svGetLoadRespPars = new SvGetRespPars(header, apduResponse);

        apduResponse = new ApduResponse(ByteArrayUtil.fromHex(/* Current KVC (1) */ "55" +
        /* SV TNum (2) */ "A55A" +
        /* Previous SignatureLo (6) */ "665544" +
        /* Challenge out */ "1122" +
        /* SV Balance (3) */ "000000" +
        /* Debit Amount (2) */ "0001" +
        /* Debit Date (2) */ "1235" +
        /* Debit Time (2) */ "6789" +
        /* Debit KVC (1) */ "BB" +
        /* Debit SAM ID (4) */ "BBCCDDEE" +
        /* Debit SAM TNum (3) */ "A34567" +
        /* Debit Balance (3) */ "001120" +
        /* Debit SV TNum (2) */ "F568" +
        /* Successful execution status word */ "9000"), null);

        SvGetRespPars svGetDebitRespPars = new SvGetRespPars(header, apduResponse);

        SvGetPoResponse svGetPoResponse =
                new SvGetPoResponse(svGetLoadRespPars, svGetDebitRespPars);
        Assert.assertTrue(svGetPoResponse.isLoadLogAvailable());
        Assert.assertTrue(svGetPoResponse.isDebitLogAvailable());
        Assert.assertNotNull(svGetPoResponse.getLoadLog());
        Assert.assertNotNull(svGetPoResponse.getDebitLog());
        Assert.assertEquals(0, svGetPoResponse.getBalance());
        Assert.assertEquals((byte) 0x55, svGetPoResponse.getCurrentKVC());
        Assert.assertEquals(0xA55A, svGetPoResponse.getTransactionNumber());
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("AABBCCDD"),
                svGetPoResponse.getLoadLog().getSamID());
        Assert.assertEquals(0x1121, svGetPoResponse.getLoadLog().getBalance());
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("BBCCDDEE"),
                svGetPoResponse.getDebitLog().getSamID());
        Assert.assertEquals(0x1120, svGetPoResponse.getDebitLog().getBalance());
    }

    @Test
    public void doubleDebitLoadParser() throws KeypleCalypsoSvException {
        ApduResponse apduResponse =
                new ApduResponse(ByteArrayUtil.fromHex(/* Current KVC (1) */ "55" +
                /* SV TNum (2) */ "A55A" +
                /* Previous SignatureLo (6) */ "665544" +
                /* Challenge out */ "1122" +
                /* SV Balance (3) */ "000000" +
                /* Load Date (2) */ "1234" +
                /* Load Free1 (1) */ "22" +
                /* Load KVC (1) */ "AA" +
                /* Load Free2 (1) */ "33" +
                /* Load Balance (3) */ "001121" +
                /* Load Amount (3) */ "000001" +
                /* Load Time (2) */ "5678" +
                /* Load SAM ID (4) */ "AABBCCDD" +
                /* Load SAM TNum (3) */ "D23456" +
                /* Load SV TNum (2) */ "E567" +
                /* Successful execution status word */ "9000"), null);

        SvGetRespPars svGetLoadRespPars = new SvGetRespPars(header, apduResponse);

        apduResponse = new ApduResponse(ByteArrayUtil.fromHex(/* Current KVC (1) */ "55" +
        /* SV TNum (2) */ "A55A" +
        /* Previous SignatureLo (6) */ "665544" +
        /* Challenge out */ "1122" +
        /* SV Balance (3) */ "000000" +
        /* Debit Amount (2) */ "0001" +
        /* Debit Date (2) */ "1235" +
        /* Debit Time (2) */ "6789" +
        /* Debit KVC (1) */ "BB" +
        /* Debit SAM ID (4) */ "BBCCDDEE" +
        /* Debit SAM TNum (3) */ "A34567" +
        /* Debit Balance (3) */ "001120" +
        /* Debit SV TNum (2) */ "F568" +
        /* Successful execution status word */ "9000"), null);

        SvGetRespPars svGetDebitRespPars = new SvGetRespPars(header, apduResponse);

        SvGetPoResponse svGetPoResponse =
                new SvGetPoResponse(svGetDebitRespPars, svGetLoadRespPars);
        Assert.assertTrue(svGetPoResponse.isLoadLogAvailable());
        Assert.assertTrue(svGetPoResponse.isDebitLogAvailable());
        Assert.assertNotNull(svGetPoResponse.getLoadLog());
        Assert.assertNotNull(svGetPoResponse.getDebitLog());
        Assert.assertEquals(0, svGetPoResponse.getBalance());
        Assert.assertEquals((byte) 0x55, svGetPoResponse.getCurrentKVC());
        Assert.assertEquals(0xA55A, svGetPoResponse.getTransactionNumber());
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("AABBCCDD"),
                svGetPoResponse.getLoadLog().getSamID());
        Assert.assertEquals(0x1121, svGetPoResponse.getLoadLog().getBalance());
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("BBCCDDEE"),
                svGetPoResponse.getDebitLog().getSamID());
        Assert.assertEquals(0x1120, svGetPoResponse.getDebitLog().getBalance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_Parser1_null_1() {
        SvGetPoResponse svGetPoResponse = new SvGetPoResponse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_Parser1_null_2() {
        SvGetPoResponse svGetPoResponse = new SvGetPoResponse(null, null);
    }

    @Test(expected = KeypleCalypsoSvException.class)
    public void loadLog_absent() throws KeypleCalypsoSvException {
        ApduResponse apduResponse =
                new ApduResponse(ByteArrayUtil.fromHex(/* Current KVC (1) */ "55" +
                /* SV TNum (2) */ "A55A" +
                /* Previous SignatureLo (6) */ "665544" +
                /* Challenge out */ "1122" +
                /* SV Balance (3) */ "000000" +
                /* Debit Amount (2) */ "0001" +
                /* Debit Date (2) */ "1235" +
                /* Debit Time (2) */ "6789" +
                /* Debit KVC (1) */ "BB" +
                /* Debit SAM ID (4) */ "BBCCDDEE" +
                /* Debit SAM TNum (3) */ "A34567" +
                /* Debit Balance (3) */ "001120" +
                /* Debit SV TNum (2) */ "F568" +
                /* Successful execution status word */ "9000"), null);

        SvGetRespPars svGetDebitRespPars = new SvGetRespPars(header, apduResponse);
        SvGetPoResponse svGetPoResponse = new SvGetPoResponse(svGetDebitRespPars);
        Assert.assertTrue(svGetPoResponse.isDebitLogAvailable());
        Assert.assertFalse(svGetPoResponse.isLoadLogAvailable());
        Assert.assertNotNull(svGetPoResponse.getLoadLog());
    }

    @Test(expected = KeypleCalypsoSvException.class)
    public void debitLog_absent() throws KeypleCalypsoSvException {
        ApduResponse apduResponse =
                new ApduResponse(ByteArrayUtil.fromHex(/* Current KVC (1) */ "55" +
                /* SV TNum (2) */ "A55A" +
                /* Previous SignatureLo (6) */ "665544" +
                /* Challenge out */ "1122" +
                /* SV Balance (3) */ "000000" +
                /* Load Date (2) */ "1234" +
                /* Load Free1 (1) */ "22" +
                /* Load KVC (1) */ "AA" +
                /* Load Free2 (1) */ "33" +
                /* Load Balance (3) */ "001121" +
                /* Load Amount (3) */ "000001" +
                /* Load Time (2) */ "5678" +
                /* Load SAM ID (4) */ "AABBCCDD" +
                /* Load SAM TNum (3) */ "D23456" +
                /* Load SV TNum (2) */ "E567" +
                /* Successful execution status word */ "9000"), null);

        SvGetRespPars svGetLoadRespPars = new SvGetRespPars(header, apduResponse);

        SvGetPoResponse svGetPoResponse = new SvGetPoResponse(svGetLoadRespPars);
        Assert.assertFalse(svGetPoResponse.isDebitLogAvailable());
        Assert.assertTrue(svGetPoResponse.isLoadLogAvailable());
        Assert.assertNotNull(svGetPoResponse.getDebitLog());
    }
}
