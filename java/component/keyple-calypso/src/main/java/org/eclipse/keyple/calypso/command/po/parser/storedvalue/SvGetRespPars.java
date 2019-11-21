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

    private final byte currentKVC;
    private final int transactionNumber;

    private final byte[] previousSignatureLo;
    private final byte[] challengeOut;
    private final int balance;
    private final LoadLog loadLog;
    private final DebitLog debitLog;

    /**
     * inner class containing the load log
     */
    class LoadLog {
        private final byte[] date = new byte[2];
        private final byte[] time = new byte[2];
        private final byte KVC;
        private final byte[] free = new byte[2];
        private final int balance, amount;
        private final byte[] samID = new byte[4];
        private final int samTransactionNumber;
        private final int svTransactionNumber;

        /**
         * Constructor
         * 
         * @param poResponse response data from the PO
         * @param offset position of the load log in the response
         */
        LoadLog(byte[] poResponse, int offset) {
            /* parses the data and fills in the fields */
            date[0] = poResponse[offset + 0];
            date[1] = poResponse[offset + 1];
            free[0] = poResponse[offset + 2];
            free[1] = poResponse[offset + 4];
            KVC = poResponse[offset + 3];
            balance = ByteArrayUtil.threeBytesSignedToInt(poResponse, offset + 5);
            amount = ByteArrayUtil.threeBytesSignedToInt(poResponse, offset + 8);
            time[0] = poResponse[offset + 11];
            time[1] = poResponse[offset + 12];
            System.arraycopy(poResponse, offset + 13, samID, 0, 4);
            samTransactionNumber = ByteArrayUtil.threeBytesToInt(poResponse, offset + 17);
            svTransactionNumber = ByteArrayUtil.twoBytesToInt(poResponse, offset + 20);
        }

        public byte[] getDate() {
            return date;
        }

        public byte[] getTime() {
            return time;
        }

        public byte getKVC() {
            return KVC;
        }

        public byte[] getFree() {
            return free;
        }

        public int getBalance() {
            return balance;
        }

        public int getAmount() {
            return amount;
        }

        public byte[] getSamID() {
            return samID;
        }

        public int getSamTransactionNumber() {
            return samTransactionNumber;
        }

        public int getSvTransactionNumber() {
            return svTransactionNumber;
        }
    }

    /**
     * inner class containing the load log
     */
    class DebitLog {
        private final byte[] date = new byte[2];
        private final byte[] time = new byte[2];
        private final byte KVC;
        private final int balance, amount;
        private final byte[] samID = new byte[4];
        private final int samTransactionNumber;
        private final int svTransactionNumber;

        /**
         * Constructor
         * 
         * @param poResponse response data from the PO
         * @param offset position of the debit log in the response
         */
        DebitLog(byte[] poResponse, int offset) {
            /* parses the data and fills in the fields */
            amount = ByteArrayUtil.twoBytesSignedToInt(poResponse, offset);
            date[0] = poResponse[offset + 2];
            date[1] = poResponse[offset + 3];
            time[0] = poResponse[offset + 4];
            time[1] = poResponse[offset + 5];
            KVC = poResponse[offset + 6];
            System.arraycopy(poResponse, offset + 7, samID, 0, 4);
            samTransactionNumber = ByteArrayUtil.threeBytesToInt(poResponse, offset + 11);
            balance = ByteArrayUtil.threeBytesSignedToInt(poResponse, offset + 14);
            svTransactionNumber = ByteArrayUtil.twoBytesToInt(poResponse, offset + 17);
        }

        public byte[] getDate() {
            return date;
        }

        public byte[] getTime() {
            return time;
        }

        public byte getKVC() {
            return KVC;
        }

        public int getBalance() {
            return balance;
        }

        public int getAmount() {
            return amount;
        }

        public byte[] getSamID() {
            return samID;
        }

        public int getSamTransactionNumber() {
            return samTransactionNumber;
        }

        public int getSvTransactionNumber() {
            return svTransactionNumber;
        }
    }

    /**
     * Constructor to build a parser of the SvGet command response.
     *
     * @param response response to parse
     */
    public SvGetRespPars(ApduResponse response) {
        super(response);
        byte[] poResponse = response.getDataOut();
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
                    loadLog = new LoadLog(poResponse, 11);;
                    debitLog = null;
                } else {
                    /* Debit */
                    loadLog = null;
                    debitLog = new DebitLog(poResponse, 11);;
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
                loadLog = new LoadLog(poResponse, 20);
                debitLog = new DebitLog(poResponse, 42);
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
        return String.format("SV Get");
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
}
