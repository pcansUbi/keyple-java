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

import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvGetRespPars;
import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoSvException;

/**
 * Container for data received in response to the SvGet command.
 * <p>
 * In case the two logs (reload and debit) are requested and the PO does not support Rev3.2 mode,
 * this data comes from two commands executed separately.
 */
public class SvGetPoResponse {
    private final SvGetRespPars parser1;
    private final SvGetRespPars parser2;

    public SvGetPoResponse(SvGetRespPars parser1) {
        if (parser1 == null) {
            throw new IllegalArgumentException("Parser can't be null.");
        }
        this.parser1 = parser1;
        this.parser2 = null;
    }

    public SvGetPoResponse(SvGetRespPars parser1, SvGetRespPars parser2) {
        if (parser1 == null) {
            throw new IllegalArgumentException("Parser can't be null.");
        }
        this.parser1 = parser1;
        this.parser2 = parser2;
    }

    public byte getCurrentKVC() {
        return parser1.getCurrentKVC();
    }

    public int getTransactionNumber() {
        return parser1.getTransactionNumber();
    }

    public int getBalance() {
        return parser1.getBalance();
    }

    boolean isLoadLogAvailable() {
        return parser1.getLoadLog() != null || (parser2 != null && parser2.getLoadLog() != null);
    }

    public SvGetRespPars.LoadLog getLoadLog() throws KeypleCalypsoSvException {
        if (parser1.getLoadLog() != null) {
            return parser1.getLoadLog();
        } else {
            if (parser2 != null && parser2.getLoadLog() != null) {
                return parser2.getLoadLog();
            } else {
                throw new KeypleCalypsoSvException("No load log available.");
            }
        }
    }

    boolean isDebitLogAvailable() {
        return parser1.getDebitLog() != null || (parser2 != null && parser2.getDebitLog() != null);
    }

    public SvGetRespPars.DebitLog getDebitLog() throws KeypleCalypsoSvException {
        if (parser1.getDebitLog() != null) {
            return parser1.getDebitLog();
        } else {
            if (parser2 != null && parser2.getDebitLog() != null) {
                return parser2.getDebitLog();
            } else {
                throw new KeypleCalypsoSvException("No debit log available.");
            }
        }
    }
}
