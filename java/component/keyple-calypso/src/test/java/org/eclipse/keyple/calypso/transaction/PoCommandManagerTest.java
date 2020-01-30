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

import java.util.List;
import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.AbstractPoResponseParser;
import org.eclipse.keyple.calypso.command.po.PoRevision;
import org.eclipse.keyple.calypso.command.po.builder.AppendRecordCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.UpdateRecordCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvDebitCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvGetCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvReloadCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvUndebitCmdBuild;
import org.eclipse.keyple.calypso.command.po.parser.AppendRecordRespPars;
import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvGetRespPars;
import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvReloadRespPars;
import org.eclipse.keyple.core.command.AbstractApduResponseParser;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PoCommandManagerTest {
    private AppendRecordCmdBuild appendRecordCmdBuilder;
    private SvGetCmdBuild svGetCmdBuilder;
    private SvReloadCmdBuild svReloadCmdBuilder;
    private UpdateRecordCmdBuild updateRecordCmdBuilder;
    private SvDebitCmdBuild svDebitCmdBuilder;
    private SvUndebitCmdBuild svUndebitCmdBuilder;

    @Before
    public void setUp() {
        byte sfi = (byte) 0x07;
        byte[] rec = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        /* create a set of command builders */

        appendRecordCmdBuilder = new AppendRecordCmdBuild(PoClass.ISO, sfi, rec, "append record");
        updateRecordCmdBuilder =
                new UpdateRecordCmdBuild(PoClass.ISO, sfi, (byte) 0x01, rec, "update record");

        svGetCmdBuilder = new SvGetCmdBuild(PoClass.ISO, PoRevision.REV3_1,
                SvSettings.Operation.DEBIT, "sv get");

        byte[] date = {(byte) 0x01, (byte) 0x02};
        byte[] time = {(byte) 0x03, (byte) 0x04};
        byte[] free = {(byte) 0x05, (byte) 0x06};

        svReloadCmdBuilder = new SvReloadCmdBuild(PoClass.ISO, PoRevision.REV3_1, 1, (byte) 0x00,
                date, time, free);

        svDebitCmdBuilder =
                new SvDebitCmdBuild(PoClass.ISO, PoRevision.REV3_1, 1, (byte) 0x00, date, time);

        svUndebitCmdBuilder =
                new SvUndebitCmdBuild(PoClass.ISO, PoRevision.REV3_1, 1, (byte) 0x00, date, time);
    }

    @Test
    public void addRegularCommand() {
        /* nominal case */
        PoCommandManager poCommandManager = new PoCommandManager();
        /* empty list expected */
        List<PoCommand> parserList = poCommandManager.getPoCommandList();
        Assert.assertTrue(parserList.isEmpty());
        /* add 1st regular command */
        poCommandManager.addRegularCommand(appendRecordCmdBuilder);
        /* 1 expected element, the type is equal to the added element */
        Assert.assertEquals(1, parserList.size());
        Assert.assertTrue(parserList.get(0).getCommandBuilder() instanceof AppendRecordCmdBuild);
        /* add 2nd regular command */
        poCommandManager.addRegularCommand(updateRecordCmdBuilder);
        /* 2 expected elements, the types are equal to the added elements */
        Assert.assertEquals(2, parserList.size());
        Assert.assertTrue(parserList.get(0).getCommandBuilder() instanceof AppendRecordCmdBuild);
        Assert.assertTrue(parserList.get(1).getCommandBuilder() instanceof UpdateRecordCmdBuild);
    }

    @Test(expected = IllegalStateException.class)
    public void addRegularCommand_addSV() {
        /*
         * illegal state exception when an SV command is added through the regular command channel
         */
        PoCommandManager poCommandManager = new PoCommandManager();
        /* add SvGet builder: raises an exception */
        poCommandManager.addRegularCommand(svGetCmdBuilder);
    }

    @Test
    public void addStoredValueCommand() {
        /* nominal case */
        PoCommandManager poCommandManager = new PoCommandManager();
        /* DO as default action expected */
        Assert.assertEquals(SvSettings.Action.DO, poCommandManager.getSvAction());
        /* empty list expected */
        List<PoCommand> parserList = poCommandManager.getPoCommandList();
        Assert.assertTrue(parserList.isEmpty());
        /* add SvGet */
        poCommandManager.addStoredValueCommand(svGetCmdBuilder, SvSettings.Operation.RELOAD,
                SvSettings.Action.DO);
        /* 1 element expected */
        Assert.assertEquals(1, parserList.size());
        Assert.assertTrue(parserList.get(0).getCommandBuilder() instanceof SvGetCmdBuild);
        Assert.assertEquals(SvSettings.Action.DO, poCommandManager.getSvAction());
        /* no SV operation at the moment */
        Assert.assertFalse(poCommandManager.isSvOperationPending());
        /* simulate the processing of SvGet to allow the addition of an SV operation */
        poCommandManager.notifyCommandsProcessed();
        /* add SV reload */
        poCommandManager.addStoredValueCommand(svReloadCmdBuilder, SvSettings.Operation.RELOAD,
                SvSettings.Action.DO);
        /* SV operation pending expected */
        Assert.assertTrue(poCommandManager.isSvOperationPending());

        /* Get the parser and check its output */
        PoCommand poCommand = poCommandManager.getPoCommandList().get(0);
        poCommand.setResponseParser((AbstractPoResponseParser) (poCommand
                .getCommandBuilder().createResponseParser(
                        new ApduResponse(ByteArrayUtil.fromHex("1122339000"), null))));

        AbstractPoResponseParser svOperationParser;
        svOperationParser = poCommandManager.getSvOperationResponseParser();
        Assert.assertTrue(svOperationParser instanceof SvReloadRespPars);
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("112233"),
                ((SvReloadRespPars) svOperationParser).getSignatureLo());
    }

    @Test
    public void addStoredValueCommand_undebit() {
        /* nominal case */
        PoCommandManager poCommandManager = new PoCommandManager();
        /* DO as default action expected */
        Assert.assertEquals(SvSettings.Action.DO, poCommandManager.getSvAction());
        /* empty list expected */
        List<PoCommand> parserList = poCommandManager.getPoCommandList();
        Assert.assertTrue(parserList.isEmpty());
        /* add SvGet */
        poCommandManager.addStoredValueCommand(svGetCmdBuilder, SvSettings.Operation.DEBIT,
                SvSettings.Action.UNDO);
        /* 1 element expected */
        Assert.assertEquals(1, parserList.size());
        Assert.assertTrue(parserList.get(0).getCommandBuilder() instanceof SvGetCmdBuild);
        Assert.assertEquals(SvSettings.Action.UNDO, poCommandManager.getSvAction());
        /* simulate the processing of SvGet to allow the addition of an SV operation */
        poCommandManager.notifyCommandsProcessed();
        /* add SV reload */
        poCommandManager.addStoredValueCommand(svUndebitCmdBuilder, SvSettings.Operation.DEBIT,
                SvSettings.Action.UNDO);
    }

    @Test(expected = IllegalStateException.class)
    public void addStoredValueCommand_illegalPosition_1() {
        /* SV get is not the last executed command before SV operation */
        PoCommandManager poCommandManager = new PoCommandManager();
        Assert.assertEquals(SvSettings.Action.DO, poCommandManager.getSvAction());
        List<PoCommand> parserList = poCommandManager.getPoCommandList();
        Assert.assertTrue(parserList.isEmpty());
        poCommandManager.addStoredValueCommand(svGetCmdBuilder, SvSettings.Operation.RELOAD,
                SvSettings.Action.DO);
        /* add append record after SV Get */
        poCommandManager.addRegularCommand(appendRecordCmdBuilder);
        poCommandManager.notifyCommandsProcessed();
        poCommandManager.addStoredValueCommand(svReloadCmdBuilder, SvSettings.Operation.RELOAD,
                SvSettings.Action.DO);
    }

    @Test(expected = IllegalStateException.class)
    public void addStoredValueCommand_illegalPosition_2() {
        /* SV operation is not the first command in the command list */
        PoCommandManager poCommandManager = new PoCommandManager();
        Assert.assertEquals(SvSettings.Action.DO, poCommandManager.getSvAction());
        List<PoCommand> parserList = poCommandManager.getPoCommandList();
        Assert.assertTrue(parserList.isEmpty());
        poCommandManager.addStoredValueCommand(svGetCmdBuilder, SvSettings.Operation.RELOAD,
                SvSettings.Action.DO);
        poCommandManager.notifyCommandsProcessed();

        /* add append record before SV Reload */
        poCommandManager.addRegularCommand(appendRecordCmdBuilder);
        poCommandManager.addStoredValueCommand(svReloadCmdBuilder, SvSettings.Operation.RELOAD,
                SvSettings.Action.DO);
    }

    @Test(expected = IllegalStateException.class)
    public void addStoredValueCommand_inconsistent_operation() {
        /* SV operation is not the first command in the command list */
        PoCommandManager poCommandManager = new PoCommandManager();
        Assert.assertEquals(SvSettings.Action.DO, poCommandManager.getSvAction());
        List<PoCommand> parserList = poCommandManager.getPoCommandList();
        Assert.assertTrue(parserList.isEmpty());
        poCommandManager.addStoredValueCommand(svGetCmdBuilder, SvSettings.Operation.DEBIT,
                SvSettings.Action.DO);
        poCommandManager.notifyCommandsProcessed();

        /* SV Reload while SV Get targets SV Debit */
        poCommandManager.addStoredValueCommand(svReloadCmdBuilder, SvSettings.Operation.RELOAD,
                SvSettings.Action.DO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getResponseParser_invalid() {
        /* no command */
        PoCommandManager poCommandManager = new PoCommandManager();
        poCommandManager.getResponseParser(0);
    }


    @Test
    public void getResponseParser() {
        /* add a command */
        PoCommandManager poCommandManager = new PoCommandManager();
        /* add append record before SV Reload */
        int index = poCommandManager.addRegularCommand(appendRecordCmdBuilder);
        Assert.assertEquals(0, index);
        AbstractApduResponseParser commandParser = poCommandManager.getResponseParser(0);
        // the parser is null until we provide an ApduResponse
        Assert.assertNull(commandParser);
        PoCommand poCommand = poCommandManager.getPoCommandList().get(0);
        poCommand.setResponseParser((AbstractPoResponseParser) (poCommand
                .getCommandBuilder()
                .createResponseParser(new ApduResponse(ByteArrayUtil.fromHex("9000"), null))));
        commandParser = poCommandManager.getResponseParser(0);
        Assert.assertTrue(commandParser instanceof AppendRecordRespPars);
    }

    @Test(expected = IllegalStateException.class)
    public void getSvGetResponseParser_invalid() {
        /* no command */
        PoCommandManager poCommandManager = new PoCommandManager();
        poCommandManager.getSvGetResponseParserIndex();
    }

    @Test
    public void getSvGetResponseParser() {
        /* nominal case */
        PoCommandManager poCommandManager = new PoCommandManager();
        poCommandManager.addStoredValueCommand(svGetCmdBuilder, SvSettings.Operation.DEBIT,
                SvSettings.Action.DO);
        AbstractPoResponseParser commandParser = (AbstractPoResponseParser) poCommandManager
                .getResponseParser(poCommandManager.getSvGetResponseParserIndex());
        Assert.assertNull(commandParser);
        /* Create the parser */
        PoCommand poCommand = poCommandManager.getPoCommandList().get(0);
        poCommand.setResponseParser((AbstractPoResponseParser) (poCommand
                .getCommandBuilder().createResponseParser(new ApduResponse(
                        ByteArrayUtil.fromHex(/* Challenge (8) */ "0011223344556677" +
                        /* Current KVC (1) */ "55" +
                        /* SV TNum (2) */ "A55A" +
                        /* Previous SignatureLo (6) */ "665544332211" +
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
                        /* Debit Amount (2) */ "0001" +
                        /* Debit Date (2) */ "1235" +
                        /* Debit Time (2) */ "6789" +
                        /* Debit KVC (1) */ "BB" +
                        /* Debit SAM ID (4) */ "BBCCDDEE" +
                        /* Debit SAM TNum (3) */ "A34567" +
                        /* Debit Balance (3) */ "001120" +
                        /* Debit SV TNum (2) */ "F568" +
                        /* Successful execution status word */ "9000"), null))));
        commandParser = (AbstractPoResponseParser) poCommandManager
                .getResponseParser(poCommandManager.getSvGetResponseParserIndex());
        Assert.assertTrue(commandParser instanceof SvGetRespPars);
    }

    @Test(expected = IllegalStateException.class)
    public void getSvOperationResponseParser_1() {
        /* illegal state exception when no parser is available */
        PoCommandManager poCommandManager = new PoCommandManager();
        poCommandManager.getSvOperationResponseParser();
    }

    @Test(expected = IllegalStateException.class)
    public void getSvOperationResponseParser_2() {
        /*
         * illegal state exception when a regular parser is available but no SV operation parser is
         * available
         */
        PoCommandManager poCommandManager = new PoCommandManager();
        poCommandManager.addRegularCommand(appendRecordCmdBuilder);
        poCommandManager.addStoredValueCommand(svGetCmdBuilder, SvSettings.Operation.DEBIT,
                SvSettings.Action.DO);
        poCommandManager.getSvOperationResponseParser();
    }

    @Test
    public void getSvOperationResponseParser_3() {
        /* nominal operation */
        PoCommandManager poCommandManager = new PoCommandManager();
        /*
         * add SV Get to comply with the expected operation order: SVGet -> SVOperation
         * (load/debit/undebit)
         */
        poCommandManager.addStoredValueCommand(svGetCmdBuilder, SvSettings.Operation.DEBIT,
                SvSettings.Action.DO);
        /* simulate processing to ensure that the SV Operation is in first position */
        poCommandManager.notifyCommandsProcessed();
        /* Add the SV operation builder */
        poCommandManager.addStoredValueCommand(svReloadCmdBuilder, SvSettings.Operation.DEBIT,
                SvSettings.Action.DO);
        /* Create the parser */
        PoCommand poCommand = poCommandManager.getPoCommandList().get(0);
        poCommand.setResponseParser((AbstractPoResponseParser) (poCommand
                .getCommandBuilder().createResponseParser(
                        new ApduResponse(ByteArrayUtil.fromHex("1122339000"), null))));
        /* Get the parser */
        AbstractPoResponseParser svOperationParser =
                poCommandManager.getSvOperationResponseParser();
        /* Check consistency */
        Assert.assertTrue(svOperationParser instanceof SvReloadRespPars);
        Assert.assertArrayEquals(ByteArrayUtil.fromHex("112233"),
                ((SvReloadRespPars) svOperationParser).getSignatureLo());
    }
}
