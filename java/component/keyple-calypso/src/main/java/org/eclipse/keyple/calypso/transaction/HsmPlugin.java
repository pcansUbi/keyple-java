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

import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoNoSamResourceException;
import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoSamResourceFailureException;
import org.eclipse.keyple.core.seproxy.ReaderPoolPlugin;

public interface HsmPlugin extends ReaderPoolPlugin {
    SamResource allocateSamResource(String groupReference)
            throws KeypleCalypsoNoSamResourceException, KeypleCalypsoSamResourceFailureException;

    void releaseSamResource(SamResource samResource)
            throws KeypleCalypsoSamResourceFailureException;
}
