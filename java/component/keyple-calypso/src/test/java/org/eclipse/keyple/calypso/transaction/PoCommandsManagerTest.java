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
import org.eclipse.keyple.calypso.command.po.builder.UpdateRecordCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvDebitCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvGetCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvReloadCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvUndebitCmdBuild;
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
    private UpdateRecordCmdBuild updateRecordCmdBuilder;
    private SvDebitCmdBuild svDebitCmdBuilder;
    private SvUndebitCmdBuild svUndebitCmdBuilder;

    @Before
    public void setUp() throws Exception {
        byte sfi = (byte) 0x07;
        byte[] rec = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        /* create a set of command builders */

        appendRecordCmdBuilder = new AppendRecordCmdBuild(PoClass.ISO, sfi, rec, "append record");
        updateRecordCmdBuilder = new UpdateRecordCmdBuild(PoClass.ISO, sfi, (byte)0x01, rec, "update record");

        svGetCmdBuilder = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                SvOperation.DEBIT, "sv get");

        byte[] date = {(byte)0x01, (byte)0x02};
        byte[] time = {(byte)0x03, (byte)0x04};
        byte[] free = {(byte)0x05, (byte)0x06};

        svReloadCmdBuilder = new SvReloadCmdBuild(PoClass.ISO,
                PoRevision.REV3_1, 1,
                (byte)0x00, date,
                time, free);

        svDebitCmdBuilder = new SvDebitCmdBuild(PoClass.ISO,
                PoRevision.REV3_1, 1,
                (byte)0x00, date,
                time);

        svUndebitCmdBuilder = new SvUndebitCmdBuild(PoClass.ISO,
                PoRevision.REV3_1, 1,
                (byte)0x00, date,
                time);
    }

    @Test
    public void addRegularCommand() {
        /* nominal case */
        PoCommandsManager poCommandsManager = new PoCommandsManager();
        /* empty list expected  */
        List<PoBuilderParser> parserList = poCommandsManager.getPoBuilderParserList();
        Assert.assertTrue(parserList.isEmpty());
        /* add 1st regular command */
        poCommandsManager.addRegularCommand(appendRecordCmdBuilder);
        /* 1 expected element, the type is equal to the added element */
        Assert.assertEquals(1, parserList.size());
        Assert.assertTrue(parserList.get(0).getCommandBuilder() instanceof AppendRecordCmdBuild);
        /* add 2nd regular command */
        poCommandsManager.addRegularCommand(updateRecordCmdBuilder);
        /* 2 expected elements, the types are equal to the added elements */
        Assert.assertEquals(2, parserList.size());
        Assert.assertTrue(parserList.get(0).getCommandBuilder() instanceof AppendRecordCmdBuild);
        Assert.assertTrue(parserList.get(1).getCommandBuilder() instanceof UpdateRecordCmdBuild);
    }

    @Test(expected = IllegalStateException.class)
    public void addRegularCommand_addSV() {
        /* illegal state exception when an SV command is added through the regular command channel */
        PoCommandsManager poCommandsManager = new PoCommandsManager();
        List<PoBuilderParser> parserList = poCommandsManager.getPoBuilderParserList();
        /* add SvGet builder: raises an exception */
        poCommandsManager.addRegularCommand(svGetCmdBuilder);
    }

    @Test
    public void addStoredValueCommand() {
        /* nominal case */
        PoCommandsManager poCommandsManager = new PoCommandsManager();
        /* DO as default action expected */
        Assert.assertEquals(SvAction.DO,poCommandsManager.getSvAction());
        /* empty list expected */
        List<PoBuilderParser> parserList = poCommandsManager.getPoBuilderParserList();
        Assert.assertTrue(parserList.isEmpty());
        /* add SvGet */
        poCommandsManager.addStoredValueCommand(svGetCmdBuilder, SvOperation.RELOAD, SvAction.DO);
        /* 1 element expected */
        Assert.assertEquals(1, parserList.size());
        Assert.assertTrue(parserList.get(0).getCommandBuilder() instanceof SvGetCmdBuild);
        Assert.assertEquals(SvAction.DO, poCommandsManager.getSvAction());
        /* no SV operation at the moment */
        Assert.assertFalse(poCommandsManager.isSvOperationPending());
        /* simulate the processing of SvGet to allow the addition of an SV operation */
        poCommandsManager.notifyCommandsProcessed();
        /* add SV reload */
        poCommandsManager.addStoredValueCommand(svReloadCmdBuilder, SvOperation.RELOAD, SvAction.DO);
        /* SV operation pending expected */
        Assert.assertTrue(poCommandsManager.isSvOperationPending());

        /* Get the parser and check its output */
        PoBuilderParser poBuilderParser = poCommandsManager.getPoBuilderParserList().get(0);
        poBuilderParser.setResponseParser((AbstractPoResponseParser) (poBuilderParser
                .getCommandBuilder().createResponseParser(new ApduResponse(ByteArrayUtil.fromHex("1122339000"), null))));

        AbstractPoResponseParser svOperationParser = null;
        try {
            svOperationParser = poCommandsManager.getSvOperationResponseParser();
        } catch (KeypleCalypsoSvException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(svOperationParser instanceof SvReloadRespPars);
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("112233"), ((SvReloadRespPars)svOperationParser).getSignatureLo());
    }

    @Test
    public void addStoredValueCommand_undebit() {
        /* nominal case */
        PoCommandsManager poCommandsManager = new PoCommandsManager();
        /* DO as default action expected */
        Assert.assertEquals(SvAction.DO,poCommandsManager.getSvAction());
        /* empty list expected */
        List<PoBuilderParser> parserList = poCommandsManager.getPoBuilderParserList();
        Assert.assertTrue(parserList.isEmpty());
        /* add SvGet */
        poCommandsManager.addStoredValueCommand(svGetCmdBuilder, SvOperation.DEBIT, SvAction.UNDO);
        /* 1 element expected */
        Assert.assertEquals(1, parserList.size());
        Assert.assertTrue(parserList.get(0).getCommandBuilder() instanceof SvGetCmdBuild);
        Assert.assertEquals(SvAction.UNDO, poCommandsManager.getSvAction());
        /* simulate the processing of SvGet to allow the addition of an SV operation */
        poCommandsManager.notifyCommandsProcessed();
        /* add SV reload */
        poCommandsManager.addStoredValueCommand(svUndebitCmdBuilder, SvOperation.UNDEBIT, SvAction.UNDO);
    }

    @Test
    public void addStoredValueCommand_illegalPosition_1() {
        /* nominal case */
        PoCommandsManager poCommandsManager = new PoCommandsManager();
        Assert.assertEquals(SvAction.DO,poCommandsManager.getSvAction());
        List<PoBuilderParser> parserList = poCommandsManager.getPoBuilderParserList();
        Assert.assertTrue(parserList.isEmpty());
        poCommandsManager.addStoredValueCommand(svGetCmdBuilder, SvOperation.RELOAD, SvAction.DO);
        poCommandsManager.addRegularCommand(appendRecordCmdBuilder);
        poCommandsManager.notifyCommandsProcessed();
        poCommandsManager.addStoredValueCommand(svReloadCmdBuilder, SvOperation.RELOAD, SvAction.DO);
    }

    @Test
    public void notifyCommandsProcessed() {}

    @Test
    public void getSvAction() {}

    @Test
    public void isSvOperationPending() {}

    @Test
    public void getPoBuilderParserList() {}

    @Test //(expected = IllegalStateException.class)
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
