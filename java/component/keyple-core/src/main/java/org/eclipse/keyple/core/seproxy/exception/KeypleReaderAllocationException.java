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
package org.eclipse.keyple.core.seproxy.exception;

/**
 * Exceptions thrown in a {@link org.eclipse.keyple.core.seproxy.ReaderPoolPlugin} context when
 * managing allocated readers
 */
public class KeypleReaderAllocationException extends KeypleBaseException {

    /**
     * New reader allocation exception to be thrown
     *
     * @param message : message to identify the exception and the context
     */
    public KeypleReaderAllocationException(String message) {
        super(message);
    }

    /**
     * Encapsulate a lower level reader allocation exception
     *
     * @param message : message to add some context to the exception
     * @param cause : lower level exception
     */
    public KeypleReaderAllocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
