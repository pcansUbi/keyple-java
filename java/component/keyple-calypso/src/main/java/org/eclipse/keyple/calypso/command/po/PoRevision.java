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
package org.eclipse.keyple.calypso.command.po;


import org.eclipse.keyple.calypso.command.PoClass;

/**
 * Calypso revisions
 */
public enum PoRevision {
    /**
     * Calypso Revision 1 (CLA 0x94)
     */
    REV1_0(PoClass.LEGACY),

    /**
     * Calypso Revision 2.4 (CLA 0x94)
     */
    REV2_4(PoClass.LEGACY),

    /**
     * Calypso Revision 3.1 (CLA 0x00)
     */
    REV3_1(PoClass.ISO),

    /**
     * Calypso Revision 3.1 CLAP (CLA 0x00, application type 0x90)
     */
    REV3_1_CLAP(PoClass.ISO),

    /**
     * Calypso Revision 3.2 (CLA 0x00)
     */
    REV3_2(PoClass.ISO);

    private final PoClass poClass;

    public PoClass getPoClass() {
        return poClass;
    }

    PoRevision(PoClass poClass) {
        this.poClass = poClass;
    }
}
