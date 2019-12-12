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

public enum SvOperation {
    /** No operation */
    NONE,
    /** Increase the balance of the stored value */
    RELOAD,
    /** Decrease the balance of the stored value */
    DEBIT,
    /** Cancel the last increase the balance of the stored value */
    UNDEBIT;
}