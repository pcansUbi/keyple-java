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
package org.eclipse.keyple.calypso;

import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvGetRespPars;

public class PoSvStatus implements PoData {
    private final int balance;
    private final SvGetRespPars.LoadLog loadLog;
    private final SvGetRespPars.DebitLog debitLog;
    private final byte currentKVC;
    private final int transactionNumber;

    public PoSvStatus(int balance, SvGetRespPars.LoadLog loadLog, SvGetRespPars.DebitLog debitLog,
            byte currentKVC, int transactionNumber) {
        this.balance = balance;
        this.loadLog = loadLog;
        this.debitLog = debitLog;
        this.currentKVC = currentKVC;
        this.transactionNumber = transactionNumber;
    }

    @Override
    public Type getType() {
        return Type.SV_STATUS;
    }

    public int getBalance() {
        return balance;
    }

    public SvGetRespPars.LoadLog getLoadLog() {
        return loadLog;
    }

    public SvGetRespPars.DebitLog getDebitLog() {
        return debitLog;
    }

    public byte getCurrentKVC() {
        return currentKVC;
    }

    public int getTransactionNumber() {
        return transactionNumber;
    }
}
