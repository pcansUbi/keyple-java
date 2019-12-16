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
package org.eclipse.keyple.calypso.transaction;

import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.AbstractPoResponseParser;
import org.eclipse.keyple.calypso.command.po.PoBuilderParser;
import org.eclipse.keyple.calypso.command.po.PoRevision;
import org.eclipse.keyple.calypso.command.po.builder.AppendRecordCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvGetCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvReloadCmdBuild;
import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvReloadRespPars;
import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoSvException;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PoCommandsManagerTest {
    /* logger */
    private static final Logger logger = LoggerFactory.getLogger(PoCommandsManagerTest.class);

    private AppendRecordCmdBuild appendRecordCmdBuilder;
    private SvGetCmdBuild svGetCmdBuilder;
    private SvReloadCmdBuild svReloadCmdBuilder;

    @Before
    public void setUp() throws Exception {
        byte sfi = (byte) 0x07;
        byte[] rec = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        appendRecordCmdBuilder = new AppendRecordCmdBuild(PoClass.ISO, sfi, rec, "append record");
        svGetCmdBuilder = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                SvOperation.DEBIT, "sv get");
        svGetCmdBuilder = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                SvOperation.DEBIT, "sv get");
        byte[] date = {(byte)0x01, (byte)0x02};
        byte[] time = {(byte)0x03, (byte)0x04};
        byte[] free = {(byte)0x05, (byte)0x06};
        svReloadCmdBuilder = new SvReloadCmdBuild(PoClass.ISO,
                PoRevision.REV3_1, 1,
                (byte)0x00, date,
                time, free);
    }

    @Test
    public void addRegularCommand() {
        /* illegal state exception when a regular parser is available but no parser is available */
        PoCommandsManager poCommandsManager = new PoCommandsManager();
        List<PoBuilderParser> parserList = poCommandsManager.getPoBuilderParserList();
        Assert.assertTrue(parserList.isEmpty());
        poCommandsManager.addRegularCommand(appendRecordCmdBuilder);
        Assert.assertTrue(parserList.size() == 1);
        poCommandsManager.addRegularCommand(appendRecordCmdBuilder);
        Assert.assertTrue(parserList.size() == 2);
    }

    @Test
    public void addStoredValueCommand() {}

    @Test
    public void notifyCommandsProcessed() {}

    @Test
    public void getSvAction() {}

    @Test
    public void isSvOperationPending() {}

    @Test
    public void getPoBuilderParserList() {}

    @Test(expected = IllegalStateException.class)
    public void getResponseParser() {}

    @Test
    public void getSvGetResponseParser() {}

    @Test(expected = IllegalStateException.class)
    public void getSvOperationResponseParser_1() throws KeypleCalypsoSvException {
        /* illegal state exception when no parser is available */
        PoCommandsManager poCommandsManager = new PoCommandsManager();
        AbstractPoResponseParser svOperationParser =
                poCommandsManager.getSvOperationResponseParser();
    }

    @Test(expected = IllegalStateException.class)
    public void getSvOperationResponseParser_2() throws KeypleCalypsoSvException {
        /* illegal state exception when a regular parser is available but no SV operation parser is available */
        PoCommandsManager poCommandsManager = new PoCommandsManager();
        poCommandsManager.addRegularCommand(appendRecordCmdBuilder);
        poCommandsManager.addStoredValueCommand(svGetCmdBuilder, SvOperation.DEBIT, SvAction.DO);
        AbstractPoResponseParser svOperationParser =
                poCommandsManager.getSvOperationResponseParser();
    }

    @Test
    public void getSvOperationResponseParser_3() throws KeypleCalypsoSvException {
        /* nominal operation */
        PoCommandsManager poCommandsManager = new PoCommandsManager();
        /* add SV Get to comply with the expected operation order: SVGet -> SVOperation (load/debit/undebit) */
        poCommandsManager.addStoredValueCommand(svGetCmdBuilder, SvOperation.DEBIT, SvAction.DO);
        /* simulate processing to ensure that the SV Operation is in first position */
        poCommandsManager.notifyCommandsProcessed();
        /* Add the SV operation builder */
        poCommandsManager.addStoredValueCommand(svReloadCmdBuilder, SvOperation.DEBIT, SvAction.DO);
        /* Create the parser */
        PoBuilderParser poBuilderParser = poCommandsManager.getPoBuilderParserList().get(0);
        poBuilderParser.setResponseParser((AbstractPoResponseParser) (poBuilderParser
                .getCommandBuilder().createResponseParser(new ApduResponse(ByteArrayUtil.fromHex("1122339000"), null))));
        /* Get the parser */
        AbstractPoResponseParser svOperationParser =
                poCommandsManager.getSvOperationResponseParser();
        /* Check consistency */
        Assert.assertTrue(svOperationParser instanceof SvReloadRespPars);
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("112233"), ((SvReloadRespPars)svOperationParser).getSignatureLo());
    }
}
