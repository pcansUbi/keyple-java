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
package org.eclipse.keyple.calypso.transaction;



import java.util.*;
import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.builder.ReadRecordsCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.SelectFileCmdBuild;
import org.eclipse.keyple.calypso.command.po.parser.ReadDataStructure;
import org.eclipse.keyple.calypso.command.po.parser.ReadRecordsRespPars;
import org.eclipse.keyple.core.command.AbstractApduResponseParser;
import org.eclipse.keyple.core.selection.AbstractSeSelectionRequest;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.seproxy.message.SeResponse;
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized selection request to manage the specific characteristics of Calypso POs
 */
public final class PoSelectionRequest extends AbstractSeSelectionRequest {
    private static final Logger logger = LoggerFactory.getLogger(PoSelectionRequest.class);

    private final List<AbstractCommandData> commandParametersList;

    private final PoClass poClass;

    /**
     * Constructor.
     * 
     * @param poSelector the selector to target a particular SE
     */
    public PoSelectionRequest(PoSelector poSelector) {

        super(poSelector);

        commandParametersList = new ArrayList<AbstractCommandData>();

        /* No AID selector for a legacy Calypso PO */
        if (seSelector.getAidSelector() == null) {
            poClass = PoClass.LEGACY;
        } else {
            poClass = PoClass.ISO;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Calypso {} selector", poClass);
        }
    }

    /**
     * Prepare one or more read record ApduRequest based on the target revision to be executed
     * following the selection.
     * <p>
     * In the case of a mixed target (rev2 or rev3) two commands are prepared. The first one in rev3
     * format, the second one in rev2 format (mainly class byte)
     * 
     * @param sfi the sfi top select
     * @param readDataStructureEnum read mode enum to indicate a SINGLE, MULTIPLE or COUNTER read
     * @param firstRecordNumber the record number to read (or first record to read in case of
     *        several records)
     * @param expectedLength the expected length of the record(s)
     * @param extraInfo extra information included in the logs (can be null or empty)
     */
    private void prepareReadRecordsCmdInternal(byte sfi, ReadDataStructure readDataStructureEnum,
            byte firstRecordNumber, int expectedLength, String extraInfo) {

        /*
         * the readJustOneRecord flag is set to false only in case of multiple read records, in all
         * other cases it is set to true
         */
        boolean readJustOneRecord =
                !(readDataStructureEnum == ReadDataStructure.MULTIPLE_RECORD_DATA);

        addApduRequest(
                new ReadRecordsCmdBuild(poClass, sfi, readDataStructureEnum, firstRecordNumber,
                        readJustOneRecord, (byte) expectedLength, extraInfo).getApduRequest());

        if (logger.isTraceEnabled()) {
            logger.trace("ReadRecords: SFI = {}, RECNUMBER = {}, JUSTONE = {}, EXPECTEDLENGTH = {}",
                    sfi, firstRecordNumber, readJustOneRecord, expectedLength);
        }

        /* keep read record parameters in the dedicated Maps */
        commandParametersList
                .add(new ReadRecordsParameters(sfi, firstRecordNumber, readDataStructureEnum));
    }

    /**
     * Prepare one or more read record ApduRequest based on the target revision to be executed
     * following the selection.
     * <p>
     * The expected length is provided and its value is checked between 1 and 250.
     * <p>
     * In the case of a mixed target (rev2 or rev3) two commands are prepared. The first one in rev3
     * format, the second one in rev2 format (mainly class byte)
     *
     * @param sfi the sfi top select
     * @param readDataStructureEnum read mode enum to indicate a SINGLE, MULTIPLE or COUNTER read
     * @param firstRecordNumber the record number to read (or first record to read in case of
     *        several records)
     * @param expectedLength the expected length of the record(s)
     * @param extraInfo extra information included in the logs (can be null or empty)
     */
    public void prepareReadRecordsCmd(byte sfi, ReadDataStructure readDataStructureEnum,
            byte firstRecordNumber, int expectedLength, String extraInfo) {
        if (expectedLength < 1 || expectedLength > 250) {
            throw new IllegalArgumentException("Bad length.");
        }
        prepareReadRecordsCmdInternal(sfi, readDataStructureEnum, firstRecordNumber, expectedLength,
                extraInfo);
    }

    /**
     * Prepare one or more read record ApduRequest based on the target revision to be executed
     * following the selection. No expected length is specified, the record output length is handled
     * automatically.
     * <p>
     * In the case of a mixed target (rev2 or rev3) two commands are prepared. The first one in rev3
     * format, the second one in rev2 format (mainly class byte)
     *
     * @param sfi the sfi top select
     * @param readDataStructureEnum read mode enum to indicate a SINGLE, MULTIPLE or COUNTER read
     * @param firstRecordNumber the record number to read (or first record to read in case of
     *        several records)
     * @param extraInfo extra information included in the logs (can be null or empty)
     */
    public void prepareReadRecordsCmd(byte sfi, ReadDataStructure readDataStructureEnum,
            byte firstRecordNumber, String extraInfo) {
        if (seSelector.getSeProtocol() == SeCommonProtocols.PROTOCOL_ISO7816_3) {
            throw new IllegalArgumentException(
                    "In contacts mode, the expected length must be specified.");
        }
        prepareReadRecordsCmdInternal(sfi, readDataStructureEnum, firstRecordNumber, 0, extraInfo);
    }

    /**
     * Prepare a select file ApduRequest to be executed following the selection.
     * <p>
     * 
     * @param path path from the CURRENT_DF (CURRENT_DF identifier excluded)
     * @param extraInfo extra information included in the logs (can be null or empty)
     */
    public void prepareSelectFileCmd(byte[] path, String extraInfo) {
        addApduRequest(new SelectFileCmdBuild(poClass, path).getApduRequest());
        if (logger.isTraceEnabled()) {
            logger.trace("Select File: PATH = {}", ByteArrayUtil.toHex(path));
        }

        /* keep selection parameters in the dedicated Maps */
        commandParametersList.add(new SelectFilePathParameters(path));
    }

    /**
     * Prepare a select file ApduRequest to be executed following the selection.
     * <p>
     *
     * @param selectControl provides the navigation case: FIRST, NEXT or CURRENT
     * @param extraInfo extra information included in the logs (can be null or empty)
     */
    public void prepareSelectFileCmd(SelectFileCmdBuild.SelectControl selectControl,
            String extraInfo) {
        addApduRequest(new SelectFileCmdBuild(poClass, selectControl).getApduRequest());
        if (logger.isTraceEnabled()) {
            logger.trace("Navigate: CONTROL = {}", selectControl);
        }

        /* keep selection parameters in the dedicated Maps */
        commandParametersList.add(new SelectFileControlParameters(selectControl));
    }

    /**
     * This command is deprecated and should not no more be used with the Calypso API 0.9 and above
     * 
     * @param seResponse the received SeResponse containing the commands raw responses
     * @param commandIndex the command index
     * @return a parser of the type matching the command
     */
    @Deprecated
    @Override
    public AbstractApduResponseParser getCommandParser(SeResponse seResponse, int commandIndex) {
        return null;
    }

    /**
     * Create a CalypsoPo object containing the selection data received from the plugin.
     * <p>
     * In addition, the CalypsoPo object is completed by the information collected by the commands
     * that may have followed the selection (Read Records or Select File).
     * 
     * @param seResponse the SE response received
     * @return a {@link CalypsoPo}
     */
    @Override
    protected CalypsoPo parse(SeResponse seResponse) {
        CalypsoPo calypsoPo = new CalypsoPo(seResponse,
                seSelector.getSeProtocol().getTransmissionMode(), seSelector.getExtraInfo());
        List<ApduResponse> apduResponses = seResponse.getApduResponses();
        if (apduResponses != null) {
            /* We update the Calypso Po object the data retrieved from the PO's responses */
            // TODO check if there is something to do when the number of responses is different from
            // the number of initial commands
            Iterator<AbstractCommandData> iterator = commandParametersList.iterator();
            for (ApduResponse apduResponse : apduResponses) {
                AbstractCommandData commandData = iterator.next();
                switch (commandData.commandType) {
                    case READRECORDS:
                        ReadRecordsRespPars readRecordsRespPars =
                                new ReadRecordsRespPars(apduResponse);
                        if (readRecordsRespPars.isSuccessful()) {
                            /* Place the read data into the CalypsoPo */
                            calypsoPo.setRecord(((ReadRecordsParameters) commandData).getSfi(),
                                    ((ReadRecordsParameters) commandData).getRecord(),
                                    readRecordsRespPars.getApduResponse().getDataOut());
                            // TODO process counters differently (add specific structure foru
                            // counters in CalypsoPo)
                        } else {
                            // TODO add a status in CalypsoPo to indicate the access failure (e.g
                            // record not found)
                        }
                        break;
                    case SELECTFILE_PATH:
                        // TODO
                        break;
                    case SELECTFILE_CONTROL:
                        // TODO
                        break;
                }
            }
        }
        return calypsoPo;
    }

    /* Helper inner classes to manage the command parameters list used when parsing the responses */

    /**
     * Types of commands
     */
    private enum CommandType {
        READRECORDS, SELECTFILE_PATH, SELECTFILE_CONTROL
    };

    /**
     * Abstract class defining a generic parameter command set
     */
    private abstract class AbstractCommandData {
        private final CommandType commandType;

        AbstractCommandData(CommandType commandType) {
            this.commandType = commandType;
        }

        public CommandType getCommandType() {
            return commandType;
        }
    }

    /**
     * Class defining the parameters of Read Records
     */
    private class ReadRecordsParameters extends AbstractCommandData {
        private final byte sfi;
        private final byte record;
        private final ReadDataStructure readDataStructure;

        ReadRecordsParameters(byte sfi, byte record, ReadDataStructure readDataStructure) {
            super(CommandType.READRECORDS);
            this.sfi = sfi;
            this.record = record;
            this.readDataStructure = readDataStructure;
        }

        public byte getSfi() {
            return sfi;
        }

        public byte getRecord() {
            return record;
        }

        public ReadDataStructure getReadDataStructure() {
            return readDataStructure;
        }
    }

    /**
     * Class defining the parameters of Select File when the "path" mode is used
     */
    private class SelectFilePathParameters extends AbstractCommandData {
        private final byte[] path;

        SelectFilePathParameters(byte[] path) {
            super(CommandType.SELECTFILE_PATH);
            this.path = path.clone();
        }

        public byte[] getPath() {
            return path;
        }
    }

    /**
     * Class defining the parameters of Select File when the "control" mode is used
     */
    class SelectFileControlParameters extends AbstractCommandData {
        private final SelectFileCmdBuild.SelectControl selectControl;

        SelectFileControlParameters(SelectFileCmdBuild.SelectControl selectControl) {
            super(CommandType.SELECTFILE_CONTROL);
            this.selectControl = selectControl;
        }

        public SelectFileCmdBuild.SelectControl getSelectControl() {
            return selectControl;
        }
    }
}
