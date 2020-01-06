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

/**
 * A set of enumerations used to manage Stored Value transactions
 */
public class SvSettings {
    /**
     * {@link Operation} specifies the type of operation intended to be carried out
     */
    public enum Operation {
        /** Increase the balance of the stored value */
        RELOAD,
        /** Decrease the balance of the stored value */
        DEBIT;
    }

    /**
     * {@link Action} specifies the type of action:
     * <ul>
     * <li>Reload: DO loads a positive amount, UNDO loads a negative amount
     * <li>Debit: DO debits a positive amount, UNDO cancels, totally or partially, a previous debit.
     * </ul>
     */
    public enum Action {
        DO, UNDO
    }

    /**
     * {@link LogRead} specifies whether only the log related to the current operation {@link} is
     * requested or whether both logs are requested.
     */
    public enum LogRead {
        /** Request the RELOAD or DEBIT log according to the currently specified operation */
        SINGLE,
        /** Request both RELOAD and DEBIT logs */
        ALL
    }

    /**
     * {@link NegativeBalance} indicates whether negative balances are allowed when debiting the SV
     */
    public enum NegativeBalance {
        /**
         * An SV exception will be raised if the attempted debit of the SV would result in a
         * negative balance.
         */
        FORBIDDEN,
        /** Negative balance is allowed */
        AUTHORIZED
    }
}
