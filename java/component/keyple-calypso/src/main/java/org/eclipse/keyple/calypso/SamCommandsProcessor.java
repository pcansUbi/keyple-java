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
package org.eclipse.keyple.calypso;

import org.eclipse.keyple.calypso.command.po.PoRevision;
import org.eclipse.keyple.calypso.command.sam.SamRevision;
import org.eclipse.keyple.calypso.transaction.PoResource;
import org.eclipse.keyple.calypso.transaction.SamResource;
import org.eclipse.keyple.calypso.transaction.SecuritySettings;
import org.eclipse.keyple.core.util.ByteArrayUtil;

public class SamCommandsProcessor {
    /** The SAM resource */
    private final SamResource samResource;
    /** The PO resource */
    private final PoResource poResource;
    /** The security settings. */
    private SecuritySettings securitySettings;

    public SamCommandsProcessor(SamResource samResource, PoResource poResource, SecuritySettings securitySettings) {
        this.samResource = samResource;
        this.poResource = poResource;
        this.securitySettings = securitySettings;
    }

    public boolean initializeDigester() {
        return true;
    }
    /**
     * Initializes the digest computation process
     *
     * @param sessionEncryption true if the session is encrypted
     * @param verificationMode true if the verification mode is active
     * @param workKeyKif the PO KIF
     * @param workKeyKVC the PO KVC
     * @param digestData a first bunch of data to digest.
     * @return true if the initialization is successful
     */
    public boolean initializeDigester(boolean sessionEncryption,
                           boolean verificationMode, byte workKeyKif, byte workKeyKVC, byte[] digestData) {
        if(digestData == null) {
            return false;
        }

        encryption = sessionEncryption;
        verification = verificationMode;
        revMode = rev3_2Mode;
        keyRecordNumber = workKeyRecordNumber;
        keyKIF = workKeyKif;
        keyKVC = workKeyKVC;
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "PoTransaction.DigestProcessor => initialize: POREVISION = {}, SAMREVISION = {}, SESSIONENCRYPTION = {}",
                    poRev, samRev, sessionEncryption, verificationMode);
            logger.debug(
                    "PoTransaction.DigestProcessor => initialize: VERIFICATIONMODE = {}, REV32MODE = {} KEYRECNUMBER = {}",
                    verificationMode, rev3_2Mode, workKeyRecordNumber);
            logger.debug(
                    "PoTransaction.DigestProcessor => initialize: KIF = {}, KVC {}, DIGESTDATA = {}",
                    String.format("%02X", workKeyKif), String.format("%02X", workKeyKVC),
                    ByteArrayUtil.toHex(digestData));
        }

        /* Clear data cache */
        poDigestDataCache.clear();

        /*
         * Build Digest Init command as first ApduRequest of the digest computation process
         */
        poDigestDataCache.add(digestData);

        return true;
    }
}
