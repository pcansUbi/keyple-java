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
package org.eclipse.keyple.integration.experimental.samresourcemanager;

import org.eclipse.keyple.core.seproxy.ReaderPoolPlugin;

public interface PcscPoolPlugin extends ReaderPoolPlugin {
    String PLUGIN_NAME = "PcscPoolPlugin";

    /**
     * Plugin parameters keys
     */
    String MAX_CHANNEL_ALLOCATION_TIME_MS = "MAX_CHANNEL_ALLOCATION_TIME_MS";
}
