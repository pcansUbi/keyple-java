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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.keyple.calypso.command.po.PoRevision;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvDebitCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvReloadCmdBuild;
import org.eclipse.keyple.calypso.command.po.builder.storedvalue.SvUndebitCmdBuild;
import org.eclipse.keyple.calypso.command.po.parser.storedvalue.SvGetRespPars;
import org.eclipse.keyple.calypso.command.sam.AbstractSamCommandBuilder;
import org.eclipse.keyple.calypso.command.sam.builder.security.*;
import org.eclipse.keyple.calypso.command.sam.parser.security.*;
import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoSecureSessionException;
import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoSvSecurityException;
import org.eclipse.keyple.core.command.AbstractApduCommandBuilder;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.message.*;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SamCommandsProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SamCommandsProcessor.class);

    private static final byte KIF_UNDEFINED = (byte) 0xFF;

    private static final byte CHALLENGE_LENGTH_REV_INF_32 = (byte) 0x04;
    private static final byte CHALLENGE_LENGTH_REV32 = (byte) 0x08;
    private static final byte SIGNATURE_LENGTH_REV_INF_32 = (byte) 0x04;
    private static final byte SIGNATURE_LENGTH_REV32 = (byte) 0x08;

    /** The SAM resource */
    private final SamResource samResource;
    /** The Proxy reader to communicate with the SAM */
    private ProxyReader samReader;
    /** The PO resource */
    private final PoResource poResource;
    /** The security settings. */
    private SecuritySettings securitySettings;
    /*
     * The digest data cache stores all PO data to be send to SAM during a Secure Session. The 1st
     * buffer is the data buffer to be provided with Digest Init. The following buffers are PO
     * command/response pairs
     */
    private static final List<byte[]> poDigestDataCache = new ArrayList<byte[]>();
    private boolean sessionEncryption;
    private boolean verificationMode;
    private byte workKeyRecordNumber;
    private byte workKeyKif;
    private byte workKeyKVC;
    private boolean isDiversificationDone;

    SamCommandsProcessor(SamResource samResource, PoResource poResource,
            SecuritySettings securitySettings) {
        this.samResource = samResource;
        this.poResource = poResource;
        this.securitySettings = securitySettings;
        samReader = (ProxyReader) this.samResource.getSeReader();
    }

    byte[] getSessionTerminalChallenge() throws KeypleReaderException {
        /*
         * counts 'select diversifier' and 'get challenge' commands. At least get challenge is
         * present
         */
        int numberOfSamCmd = 1;

        /* SAM ApduRequest List to hold Select Diversifier and Get Challenge commands */
        List<ApduRequest> samApduRequestList = new ArrayList<ApduRequest>();

        if (logger.isDebugEnabled()) {
            logger.debug("processAtomicOpening => Identification: DFNAME = {}, SERIALNUMBER = {}",
                    ByteArrayUtil.toHex(poResource.getMatchingSe().getDfName()),
                    ByteArrayUtil.toHex(samResource.getMatchingSe().getSerialNumber()));
        }
        /* diversify only if this has not already been done. */
        if (!isDiversificationDone) {
            /* Build the SAM Select Diversifier command to provide the SAM with the PO S/N */
            AbstractApduCommandBuilder selectDiversifier =
                    new SelectDiversifierCmdBuild(samResource.getMatchingSe().getSamRevision(),
                            poResource.getMatchingSe().getApplicationSerialNumber());

            samApduRequestList.add(selectDiversifier.getApduRequest());

            /* increment command number */
            numberOfSamCmd++;

            /* change the diversification status */
            isDiversificationDone = true;
        }
        /* Build the SAM Get Challenge command */
        byte challengeLength =
                poResource.getMatchingSe().isRev3_2ModeAvailable() ? CHALLENGE_LENGTH_REV32
                        : CHALLENGE_LENGTH_REV_INF_32;

        AbstractSamCommandBuilder samGetChallenge = new SamGetChallengeCmdBuild(
                samResource.getMatchingSe().getSamRevision(), challengeLength);

        samApduRequestList.add(samGetChallenge.getApduRequest());

        /* Build a SAM SeRequest */
        SeRequest samSeRequest = new SeRequest(samApduRequestList);

        logger.trace("processAtomicOpening => identification: SAMSEREQUEST = {}", samSeRequest);

        /*
         * Transmit the SeRequest to the SAM and get back the SeResponse (list of ApduResponse)
         */
        SeResponse samSeResponse = samReader.transmit(samSeRequest);

        if (samSeResponse == null) {
            throw new KeypleCalypsoSecureSessionException(
                    "getSessionTerminalChallenge: null response received",
                    KeypleCalypsoSecureSessionException.Type.SAM, samSeRequest.getApduRequests(),
                    null);
        }

        logger.trace("processAtomicOpening => identification: SAMSERESPONSE = {}", samSeResponse);

        List<ApduResponse> samApduResponseList = samSeResponse.getApduResponses();
        byte[] sessionTerminalChallenge;

        if (samApduResponseList.size() == numberOfSamCmd
                && samApduResponseList.get(numberOfSamCmd - 1).isSuccessful() && samApduResponseList
                        .get(numberOfSamCmd - 1).getDataOut().length == challengeLength) {
            SamGetChallengeRespPars samChallengePars =
                    new SamGetChallengeRespPars(samApduResponseList.get(numberOfSamCmd - 1));
            sessionTerminalChallenge = samChallengePars.getChallenge();
            if (logger.isDebugEnabled()) {
                logger.debug("processAtomicOpening => identification: TERMINALCHALLENGE = {}",
                        ByteArrayUtil.toHex(sessionTerminalChallenge));
            }
        } else {
            throw new KeypleCalypsoSecureSessionException("Invalid message received",
                    KeypleCalypsoSecureSessionException.Type.SAM, samApduRequestList,
                    samApduResponseList);
        }
        return sessionTerminalChallenge;
    }

    /**
     * Check if the provided kvc value is authorized or not.
     * <p>
     * If no list of authorized kvc is defined (authorizedKvcList null), all kvc are authorized.
     *
     * @param kvc to be tested
     * @return true if the kvc is authorized
     */
    public boolean isAuthorizedKvc(byte kvc) {
        return securitySettings.isAuthorizedKvc(kvc);
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
    boolean initializeDigester(PoTransaction.SessionAccessLevel accessLevel,
            boolean sessionEncryption, boolean verificationMode,
            SecuritySettings.DefaultKeyInfo workKeyRecordNumber, byte workKeyKif, byte workKeyKVC,
            byte[] digestData) {
        if (digestData == null) {
            return false;
        }

        this.sessionEncryption = sessionEncryption;
        this.verificationMode = verificationMode;
        this.workKeyRecordNumber = securitySettings.getKeyInfo(workKeyRecordNumber);
        if (workKeyKif == KIF_UNDEFINED) {
            switch (accessLevel) {
                case SESSION_LVL_PERSO:
                    this.workKeyKif = securitySettings
                            .getKeyInfo(SecuritySettings.DefaultKeyInfo.SAM_DEFAULT_KIF_PERSO);
                    break;
                case SESSION_LVL_LOAD:
                    this.workKeyKif = securitySettings
                            .getKeyInfo(SecuritySettings.DefaultKeyInfo.SAM_DEFAULT_KIF_LOAD);
                    break;
                case SESSION_LVL_DEBIT:
                default:
                    this.workKeyKif = securitySettings
                            .getKeyInfo(SecuritySettings.DefaultKeyInfo.SAM_DEFAULT_KIF_DEBIT);
                    break;
            }
        } else {
            this.workKeyKif = workKeyKif;
        }
        this.workKeyKVC = workKeyKVC;
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "PoTransaction.DigestProcessor => initialize: POREVISION = {}, SAMREVISION = {}, SESSIONENCRYPTION = {}, VERIFICATIONMODE = {}",
                    poResource.getMatchingSe().getRevision(),
                    samResource.getMatchingSe().getSamRevision(), sessionEncryption,
                    verificationMode);
            logger.debug(
                    "PoTransaction.DigestProcessor => initialize: VERIFICATIONMODE = {}, REV32MODE = {} KEYRECNUMBER = {}",
                    verificationMode, poResource.getMatchingSe().isRev3_2ModeAvailable(),
                    workKeyRecordNumber);
            logger.debug(
                    "PoTransaction.DigestProcessor => initialize: KIF = {}, KVC {}, DIGESTDATA = {}",
                    String.format("%02X", workKeyKif), String.format("%02X", workKeyKVC),
                    ByteArrayUtil.toHex(digestData));
        }

        /* Clear data cache */
        poDigestDataCache.clear();

        /* Build Digest Init command as first ApduRequest of the digest computation process */
        poDigestDataCache.add(digestData);

        return true;
    }

    /**
     * Appends a full PO exchange (request and response) to the digest data cache.
     *
     * @param request PO request
     * @param response PO response
     */
    void pushPoExchangeData(ApduRequest request, ApduResponse response) {

        logger.trace("PoTransaction.DigestProcessor => pushPoExchangeData: REQUEST = {}", request);

        /*
         * Add an ApduRequest to the digest computation: if the request is of case4 type, Le must be
         * excluded from the digest computation. In this cas, we remove here the last byte of the
         * command buffer.
         */
        if (request.isCase4()) {
            poDigestDataCache
                    .add(Arrays.copyOfRange(request.getBytes(), 0, request.getBytes().length - 1));
        } else {
            poDigestDataCache.add(request.getBytes());
        }

        logger.trace("PoTransaction.DigestProcessor => pushPoExchangeData: RESPONSE = {}",
                response);

        /* Add an ApduResponse to the digest computation */
        poDigestDataCache.add(response.getBytes());
    }

    /**
     * Get a unique SAM request for the whole digest computation process.
     *
     * @return SeRequest all the ApduRequest to send to the SAM in order to get the terminal
     *         signature
     */
    private SeRequest getSamDigestRequest() {
        // TODO optimization with the use of Digest Update Multiple whenever possible.
        List<ApduRequest> samApduRequestList = new ArrayList<ApduRequest>();

        if (poDigestDataCache.isEmpty()) {
            logger.debug("PoTransaction.DigestProcessor => getSamDigestRequest: no data in cache.");
            throw new IllegalStateException("Digest data cache is empty.");
        }
        if (poDigestDataCache.size() % 2 == 0) {
            /* the number of buffers should be 2*n + 1 */
            logger.debug(
                    "PoTransaction.DigestProcessor => getSamDigestRequest: wrong number of buffer in cache NBR = {}.",
                    poDigestDataCache.size());
            throw new IllegalStateException("Digest data cache is inconsistent.");
        }

        /*
         * Build and append Digest Init command as first ApduRequest of the digest computation
         * process
         */
        samApduRequestList.add(new DigestInitCmdBuild(samResource.getMatchingSe().getSamRevision(),
                verificationMode, poResource.getMatchingSe().isRev3_2ModeAvailable(),
                workKeyRecordNumber, workKeyKif, workKeyKVC, poDigestDataCache.get(0))
                        .getApduRequest());

        /*
         * Build and append Digest Update commands
         *
         * The first command is at index 1.
         */
        for (int i = 1; i < poDigestDataCache.size(); i++) {
            samApduRequestList
                    .add(new DigestUpdateCmdBuild(samResource.getMatchingSe().getSamRevision(),
                            sessionEncryption, poDigestDataCache.get(i)).getApduRequest());
        }

        /*
         * Build and append Digest Close command
         */
        samApduRequestList
                .add((new DigestCloseCmdBuild(samResource.getMatchingSe().getSamRevision(),
                        poResource.getMatchingSe().getRevision().equals(PoRevision.REV3_2)
                                ? SIGNATURE_LENGTH_REV32
                                : SIGNATURE_LENGTH_REV_INF_32).getApduRequest()));


        return new SeRequest(samApduRequestList);
    }

    /**
     * Gets the terminal signature from the SAM
     * <p>
     * All remaining data in the digest cache is sent to the SAM and the Digest Close command is
     * executed.
     * 
     * @return the terminal signature
     * @throws KeypleReaderException
     */
    byte[] getTerminalSignature() throws KeypleReaderException {
        /* All SAM digest operations will now run at once. */
        /* Get the SAM Digest request from the cache manager */
        SeRequest samSeRequest = getSamDigestRequest();

        logger.trace("processAtomicClosing => SAMREQUEST = {}", samSeRequest);

        /* Transmit SeRequest and get SeResponse */
        SeResponse samSeResponse = samReader.transmit(samSeRequest);

        logger.trace("processAtomicClosing => SAMRESPONSE = {}", samSeResponse);

        if (samSeResponse == null) {
            throw new KeypleCalypsoSecureSessionException("Null response received",
                    KeypleCalypsoSecureSessionException.Type.SAM, samSeRequest.getApduRequests(),
                    null);
        }

        if (!samSeResponse.wasChannelPreviouslyOpen()) {
            throw new KeypleCalypsoSecureSessionException("The logical channel was not open",
                    KeypleCalypsoSecureSessionException.Type.PO, samSeRequest.getApduRequests(),
                    null);
        }

        List<ApduResponse> samApduResponseList = samSeResponse.getApduResponses();

        for (int i = 0; i < samApduResponseList.size(); i++) {
            if (!samApduResponseList.get(i).isSuccessful()) {

                logger.debug("processAtomicClosing => command failure REQUEST = {}, RESPONSE = {}",
                        samSeRequest.getApduRequests().get(i), samApduResponseList.get(i));
                throw new IllegalStateException(
                        "ProcessClosing command failure during digest computation process.");
            }
        }

        /* Get Terminal Signature from the latest response */
        byte[] sessionTerminalSignature = null;
        // TODO Add length check according to Calypso REV (4 / 8)
        if (!samApduResponseList.isEmpty()) {
            DigestCloseRespPars respPars = new DigestCloseRespPars(
                    samApduResponseList.get(samApduResponseList.size() - 1));

            sessionTerminalSignature = respPars.getSignature();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("processAtomicClosing => SIGNATURE = {}",
                    ByteArrayUtil.toHex(sessionTerminalSignature));
        }

        return sessionTerminalSignature;
    }

    /**
     * Authenticates the signature part from the PO
     * 
     * @param poSignatureLo the signature part from the PO
     * @return true if the PO signature is correct
     * @throws KeypleReaderException
     */
    boolean authenticatePoSignature(byte[] poSignatureLo) throws KeypleReaderException {
        /* Check the PO signature part with the SAM */
        /* Build and send SAM Digest Authenticate command */
        AbstractApduCommandBuilder digestAuth = new DigestAuthenticateCmdBuild(
                samResource.getMatchingSe().getSamRevision(), poSignatureLo);

        List<ApduRequest> samApduRequestList = new ArrayList<ApduRequest>();
        samApduRequestList.add(digestAuth.getApduRequest());

        SeRequest samSeRequest = new SeRequest(samApduRequestList);

        logger.trace("PoTransaction.DigestProcessor => checkPoSignature: SAMREQUEST = {}",
                samSeRequest);

        SeResponse samSeResponse = samReader.transmit(samSeRequest);

        logger.trace("PoTransaction.DigestProcessor => checkPoSignature: SAMRESPONSE = {}",
                samSeResponse);

        if (samSeResponse == null) {
            throw new KeypleCalypsoSecureSessionException("Null response received",
                    KeypleCalypsoSecureSessionException.Type.SAM, samSeRequest.getApduRequests(),
                    null);
        }

        if (!samSeResponse.wasChannelPreviouslyOpen()) {
            throw new KeypleCalypsoSecureSessionException("The logical channel was not open",
                    KeypleCalypsoSecureSessionException.Type.SAM, samSeRequest.getApduRequests(),
                    null);
        }

        /* Get transaction result parsing the response */
        List<ApduResponse> samApduResponseList = samSeResponse.getApduResponses();

        boolean authenticationStatus = false;
        if ((samApduResponseList != null) && !samApduResponseList.isEmpty()) {
            DigestAuthenticateRespPars respPars =
                    new DigestAuthenticateRespPars(samApduResponseList.get(0));
            authenticationStatus = respPars.isSuccessful();
            if (authenticationStatus) {
                logger.debug(
                        "PoTransaction.DigestProcessor => checkPoSignature: mutual authentication successful.");
            } else {
                logger.debug(
                        "PoTransaction.DigestProcessor => checkPoSignature: mutual authentication failure.");
            }
        } else {
            logger.debug(
                    "DigestProcessor => checkPoSignature: no response to Digest Authenticate.");
            throw new IllegalStateException("No response to Digest Authenticate.");
        }
        return authenticationStatus;
    }

    /**
     * Computes the cryptographic data required for the SvReload command.
     * <p>
     * Use the data from the SvGet command and the partial data from the SvReload command for this
     * purpose.
     * <p>
     * The returned data will be used to finalize the PO SvReload command.
     * 
     * @param svGetResponseParser the SvGet parser providing the SvGet output data
     * @param svReloadCmdBuild the SvDebit builder providing the SvReload partial data
     * @return the complementary security data to finalize the SvDebit PO command (sam ID + SV
     *         prepare load output)
     * @throws KeypleReaderException
     */
    byte[] getSvReloadComplementaryData(SvGetRespPars svGetResponseParser,
            SvReloadCmdBuild svReloadCmdBuild) throws KeypleReaderException {
        // get the complementary data from the SAM
        SvPrepareLoadCmdBuild svPrepareLoadCmdBuild =
                new SvPrepareLoadCmdBuild(samResource.getMatchingSe().getSamRevision(),
                        svGetResponseParser, svReloadCmdBuild);

        List<ApduRequest> samApduRequestList = new ArrayList<ApduRequest>();
        int svPrepareLoadCmdIndex;
        if (!isDiversificationDone) {
            /* Build the SAM Select Diversifier command to provide the SAM with the PO S/N */
            AbstractApduCommandBuilder selectDiversifier =
                    new SelectDiversifierCmdBuild(samResource.getMatchingSe().getSamRevision(),
                            poResource.getMatchingSe().getApplicationSerialNumber());
            samApduRequestList.add(selectDiversifier.getApduRequest());
            isDiversificationDone = true;
            svPrepareLoadCmdIndex = 1;
        } else {
            svPrepareLoadCmdIndex = 0;
        }
        samApduRequestList.add(svPrepareLoadCmdBuild.getApduRequest());

        // build a SAM SeRequest
        SeRequest samSeRequest = new SeRequest(samApduRequestList);

        // execute the command
        SeResponse samSeResponse = samReader.transmit(samSeRequest);

        ApduResponse prepareReloadResponse =
                samSeResponse.getApduResponses().get(svPrepareLoadCmdIndex);
        if (!prepareReloadResponse.isSuccessful()) {
            throw new KeypleCalypsoSvSecurityException(
                    "SAM command prepareReload failed with status word "
                            + ByteArrayUtil.toHex(prepareReloadResponse.getBytes()));
        }

        // create a parser
        SvPrepareLoadRespPars svPrepareLoadRespPars =
                new SvPrepareLoadRespPars(prepareReloadResponse);

        byte[] samId = samResource.getMatchingSe().getSerialNumber();
        byte[] prepareLoadData = svPrepareLoadRespPars.getApduResponse().getDataOut();

        byte[] reloadComplementaryData = new byte[samId.length + prepareLoadData.length];

        System.arraycopy(samId, 0, reloadComplementaryData, 0, samId.length);
        System.arraycopy(prepareLoadData, 0, reloadComplementaryData, samId.length,
                prepareLoadData.length);

        return reloadComplementaryData;
    }

    /**
     * Computes the cryptographic data required for the SvDebit command.
     * <p>
     * Use the data from the SvGet command and the partial data from the SvDebit command for this
     * purpose.
     * <p>
     * The returned data will be used to finalize the PO SvDebit command.
     * 
     * @param svGetResponseParser the SvGet parser providing the SvGet output data
     * @param svDebitCmdBuild the SvDebit builder providing the SvUndebit partial data
     * @return the security data to finalize the SvDebit PO command (sam ID + SV prepare debit
     *         output)
     * @throws KeypleReaderException
     */
    byte[] getSvDebitComplementaryData(SvGetRespPars svGetResponseParser,
            SvDebitCmdBuild svDebitCmdBuild) throws KeypleReaderException {
        // get the complementary data from the SAM
        SvPrepareDebitCmdBuild svPrepareDebitCmdBuild = new SvPrepareDebitCmdBuild(
                samResource.getMatchingSe().getSamRevision(), svGetResponseParser, svDebitCmdBuild);

        List<ApduRequest> samApduRequestList = new ArrayList<ApduRequest>();
        int svPrepareDebitCmdIndex;
        if (!isDiversificationDone) {
            AbstractApduCommandBuilder selectDiversifier =
                    new SelectDiversifierCmdBuild(samResource.getMatchingSe().getSamRevision(),
                            poResource.getMatchingSe().getApplicationSerialNumber());
            samApduRequestList.add(selectDiversifier.getApduRequest());
            isDiversificationDone = true;
            svPrepareDebitCmdIndex = 1;
        } else {
            svPrepareDebitCmdIndex = 0;
        }
        samApduRequestList.add(svPrepareDebitCmdBuild.getApduRequest());

        // build a SAM SeRequest
        SeRequest samSeRequest = new SeRequest(samApduRequestList);

        // execute the command
        SeResponse samSeResponse = samReader.transmit(samSeRequest);

        ApduResponse prepareDebitResponse =
                samSeResponse.getApduResponses().get(svPrepareDebitCmdIndex);
        if (!prepareDebitResponse.isSuccessful()) {
            throw new KeypleCalypsoSvSecurityException(
                    "SAM command prepareDebit failed with status word "
                            + ByteArrayUtil.toHex(prepareDebitResponse.getBytes()));
        }

        // create a parser
        SvPrepareDebitRespPars svPrepareDebitRespPars =
                new SvPrepareDebitRespPars(prepareDebitResponse);

        byte[] samId = samResource.getMatchingSe().getSerialNumber();
        byte[] prepareDebitData = svPrepareDebitRespPars.getApduResponse().getDataOut();

        byte[] debitComplementaryData = new byte[samId.length + prepareDebitData.length];

        System.arraycopy(samId, 0, debitComplementaryData, 0, samId.length);
        System.arraycopy(prepareDebitData, 0, debitComplementaryData, samId.length,
                prepareDebitData.length);

        return debitComplementaryData;
    }

    /**
     * Computes the cryptographic data required for the SvUndebit command.
     * <p>
     * Use the data from the SvGet command and the partial data from the SvUndebit command for this
     * purpose.
     * <p>
     * The returned data will be used to finalize the PO SvUndebit command.
     * 
     * @param svGetResponseParser the SvGet parser providing the SvGet output data
     * @param svUndebitCmdBuild the SvUndebit builder providing the SvUndebit partial data
     * @return the security data to finalize the SvUndebit PO command (sam ID + SV prepare debit
     *         output)
     * @throws KeypleReaderException
     */
    public byte[] getSvUndebitComplementaryData(SvGetRespPars svGetResponseParser,
            SvUndebitCmdBuild svUndebitCmdBuild) throws KeypleReaderException {
        // get the complementary data from the SAM
        SvPrepareUndebitCmdBuild svPrepareUndebitCmdBuild =
                new SvPrepareUndebitCmdBuild(samResource.getMatchingSe().getSamRevision(),
                        svGetResponseParser, svUndebitCmdBuild);

        List<ApduRequest> samApduRequestList = new ArrayList<ApduRequest>();
        int svPrepareUndebitCmdIndex;
        if (!isDiversificationDone) {
            AbstractApduCommandBuilder selectDiversifier =
                    new SelectDiversifierCmdBuild(samResource.getMatchingSe().getSamRevision(),
                            poResource.getMatchingSe().getApplicationSerialNumber());
            samApduRequestList.add(selectDiversifier.getApduRequest());
            isDiversificationDone = true;
            svPrepareUndebitCmdIndex = 1;
        } else {
            svPrepareUndebitCmdIndex = 0;
        }
        samApduRequestList.add(svPrepareUndebitCmdBuild.getApduRequest());

        // build a SAM SeRequest
        SeRequest samSeRequest = new SeRequest(samApduRequestList);

        // execute the command
        SeResponse samSeResponse = samReader.transmit(samSeRequest);

        ApduResponse prepareUndebitResponse =
                samSeResponse.getApduResponses().get(svPrepareUndebitCmdIndex);
        if (!prepareUndebitResponse.isSuccessful()) {
            throw new KeypleCalypsoSvSecurityException(
                    "SAM command prepareUndebit failed with status word "
                            + ByteArrayUtil.toHex(prepareUndebitResponse.getBytes()));
        }

        // create a parser
        SvPrepareUndebitRespPars svPrepareUndebitRespPars =
                new SvPrepareUndebitRespPars(prepareUndebitResponse);

        byte[] samId = samResource.getMatchingSe().getSerialNumber();
        byte[] prepareUndebitData = svPrepareUndebitRespPars.getApduResponse().getDataOut();

        byte[] undebitComplementaryData = new byte[samId.length + prepareUndebitData.length];

        System.arraycopy(samId, 0, undebitComplementaryData, 0, samId.length);
        System.arraycopy(prepareUndebitData, 0, undebitComplementaryData, samId.length,
                prepareUndebitData.length);

        return undebitComplementaryData;
    }

    /**
     * Checks the status of the last SV operation
     * <p>
     * The PO signature is compared by the SAM with the one it has computed on its side.
     * 
     * @param svOperationResponseData
     * @return true if the SV check is successful
     * @throws KeypleReaderException
     */
    boolean isSvCheckSuccessful(byte[] svOperationResponseData) throws KeypleReaderException {
        List<ApduRequest> samApduRequestList = new ArrayList<ApduRequest>();
        AbstractApduCommandBuilder selectDiversifier = new SvCheckCmdBuild(
                samResource.getMatchingSe().getSamRevision(), svOperationResponseData);
        samApduRequestList.add(selectDiversifier.getApduRequest());

        // build a SAM SeRequest
        SeRequest samSeRequest = new SeRequest(samApduRequestList);

        // execute the command
        SeResponse samSeResponse = samReader.transmit(samSeRequest);

        ApduResponse svCheckResponse = samSeResponse.getApduResponses().get(0);

        if (!svCheckResponse.isSuccessful() && logger.isErrorEnabled()) {
            logger.error("SAM command prepareReload failed with status word {}",
                    ByteArrayUtil.toHex(svCheckResponse.getBytes()));
        }

        return svCheckResponse.isSuccessful();
    }
}
