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
package org.eclipse.keyple.calypso.command.po.parser.storedvalue;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.keyple.calypso.PoData;
import org.eclipse.keyple.calypso.PoSvStatus;
import org.eclipse.keyple.calypso.command.po.AbstractPoResponseParser;
import org.eclipse.keyple.core.command.AbstractApduResponseParser;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;

/**
 * SV Get (007C) response parser. See specs: Calypso
 */
public final class SvGetRespPars extends AbstractPoResponseParser {

    private static final Map<Integer, StatusProperties> STATUS_TABLE;

    static {
        Map<Integer, StatusProperties> m =
                new HashMap<Integer, StatusProperties>(AbstractApduResponseParser.STATUS_TABLE);
        m.put(0x6982, new StatusProperties(false, "Security conditions not fulfilled."));
        m.put(0x6985, new StatusProperties(false,
                "Condition for use not satisfied (a store value operation was already done in the current session)"));
        m.put(0x6A81, new StatusProperties(false, "Incorrect P1 or P2."));
        m.put(0x6A86, new StatusProperties(false, "Le inconsistent with P2."));
        m.put(0x6D00, new StatusProperties(false, "Command unavailable."));
        m.put(0x9000, new StatusProperties(true, "Successful execution."));
        STATUS_TABLE = m;
    }
    private final byte[] poResponse;
    private final byte currentKVC;
    private final int transactionNumber;

    private final byte[] previousSignatureLo;
    private final byte[] challengeOut;
    private final int balance;
    private final LoadLog loadLog;
    private final DebitLog debitLog;
    private final byte[] svCommandHeader;

    /**
     * inner class to help extract the load Log fields
     */
    public class LoadLog {
        final int offset;

        /**
         * Constructor
         * 
         * @param offset position of the load log in the PO response
         */
        LoadLog(int offset) {
            this.offset = offset;
            /* parses the data and fills in the fields */
        }

        public byte[] getDate() {
            final byte[] date = new byte[2];
            date[0] = poResponse[offset + 0];
            date[1] = poResponse[offset + 1];
            return date;
        }

        public byte[] getTime() {
            final byte[] time = new byte[2];
            time[0] = poResponse[offset + 11];
            time[1] = poResponse[offset + 12];
            return time;
        }

        public byte getKVC() {
            return poResponse[offset + 3];
        }

        public byte[] getFree() {
            final byte[] free = new byte[2];
            free[0] = poResponse[offset + 2];
            free[1] = poResponse[offset + 4];
            return free;
        }

        public int getBalance() {
            return ByteArrayUtil.threeBytesSignedToInt(poResponse, offset + 5);
        }

        public int getAmount() {
            return ByteArrayUtil.threeBytesSignedToInt(poResponse, offset + 8);
        }

        public byte[] getSamID() {
            byte[] samId = new byte[4];
            System.arraycopy(poResponse, offset + 13, samId, 0, 4);
            return samId;
        }

        public int getSamTransactionNumber() {
            return ByteArrayUtil.threeBytesToInt(poResponse, offset + 17);
        }

        public int getSvTransactionNumber() {
            return ByteArrayUtil.twoBytesToInt(poResponse, offset + 20);
        }
    }

    /**
     * inner class to help extract the debit Log fields
     */
    public class DebitLog {
        final int offset;

        /**
         * Constructor
         * 
         * @param offset position of the debit log in the PO response
         */
        DebitLog(int offset) {
            if (offset >= poResponse.length) {
                throw new IllegalArgumentException("bad offset length");
            }
            this.offset = offset;
        }

        public byte[] getDate() {
            final byte[] date = new byte[2];
            date[0] = poResponse[offset + 2];
            date[1] = poResponse[offset + 3];
            return date;
        }

        public byte[] getTime() {
            final byte[] time = new byte[2];
            time[0] = poResponse[offset + 4];
            time[1] = poResponse[offset + 5];
            return time;
        }

        public byte getKVC() {
            return poResponse[offset + 6];
        }

        public int getBalance() {
            return ByteArrayUtil.threeBytesSignedToInt(poResponse, offset + 14);
        }

        public int getAmount() {
            return ByteArrayUtil.twoBytesSignedToInt(poResponse, offset);
        }

        public byte[] getSamID() {
            byte[] samId = new byte[4];
            System.arraycopy(poResponse, offset + 7, samId, 0, 4);
            return samId;
        }

        public int getSamTransactionNumber() {
            return ByteArrayUtil.threeBytesToInt(poResponse, offset + 11);
        }

        public int getSvTransactionNumber() {
            return ByteArrayUtil.twoBytesToInt(poResponse, offset + 17);
        }
    }

    /**
     * Constructor to build a parser of the SvGet command response.
     *
     * @param svCommandHeader the SvGet command header bytes
     * @param response response to parse
     */
    public SvGetRespPars(byte[] svCommandHeader, ApduResponse response) {
        super(response);
        poResponse = response.getDataOut();
        // keep the command header
        this.svCommandHeader = svCommandHeader.clone();
        switch (poResponse.length) {
            case 0x21: /* Compatibility mode, Reload */
            case 0x1E: /* Compatibility mode, Debit or Undebit */
                challengeOut = new byte[2];
                previousSignatureLo = new byte[3];
                currentKVC = poResponse[0];
                transactionNumber = ByteArrayUtil.twoBytesToInt(poResponse, 1);
                System.arraycopy(poResponse, 3, previousSignatureLo, 0, 3);
                challengeOut[0] = poResponse[6];
                challengeOut[1] = poResponse[7];
                balance = ByteArrayUtil.threeBytesSignedToInt(poResponse, 8);
                if (poResponse.length == 0x21) {
                    /* Reload */
                    loadLog = new LoadLog(11);
                    debitLog = null;
                } else {
                    /* Debit */
                    loadLog = null;
                    debitLog = new DebitLog(11);
                }
                break;
            case 0x3D: /* Revision 3.2 mode */
                challengeOut = new byte[8];
                previousSignatureLo = new byte[6];
                System.arraycopy(poResponse, 0, challengeOut, 0, 8);
                currentKVC = poResponse[8];
                transactionNumber = ByteArrayUtil.twoBytesToInt(poResponse, 9);
                System.arraycopy(poResponse, 11, previousSignatureLo, 0, 6);
                balance = ByteArrayUtil.threeBytesSignedToInt(poResponse, 17);
                loadLog = new LoadLog(20);
                debitLog = new DebitLog(42);
                break;
            default:
                throw new IllegalStateException("Incorrect data length in response to SVGet");
        }
    }

    @Override
    protected Map<Integer, StatusProperties> getStatusTable() {
        return STATUS_TABLE;
    }


    @Override
    public String toString() {
        return "SV Get";
    }

    public byte[] getSvGetCommandHeader() {
        return svCommandHeader;
    }

    public byte getCurrentKVC() {
        return currentKVC;
    }

    public int getTransactionNumber() {
        return transactionNumber;
    }

    public byte[] getPreviousSignatureLo() {
        return previousSignatureLo;
    }

    public byte[] getChallengeOut() {
        return challengeOut;
    }

    public int getBalance() {
        return balance;
    }

    public LoadLog getLoadLog() {
        return loadLog;
    }

    public DebitLog getDebitLog() {
        return debitLog;
    }

    @Override
    public PoData getPoData() {
        return new PoSvStatus(balance, loadLog, debitLog, currentKVC, transactionNumber);
    }
}
