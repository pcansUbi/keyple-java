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
package org.eclipse.keyple.plugin.pcsc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.smartcardio.*;
import org.eclipse.keyple.core.seproxy.exception.*;
import org.eclipse.keyple.core.seproxy.plugin.local.*;
import org.eclipse.keyple.core.seproxy.plugin.local.monitoring.CardPresentMonitoringJob;
import org.eclipse.keyple.core.seproxy.plugin.local.monitoring.SmartInsertionMonitoringJob;
import org.eclipse.keyple.core.seproxy.plugin.local.monitoring.SmartRemovalMonitoringJob;
import org.eclipse.keyple.core.seproxy.plugin.local.state.WaitForSeInsertion;
import org.eclipse.keyple.core.seproxy.plugin.local.state.WaitForSeProcessing;
import org.eclipse.keyple.core.seproxy.plugin.local.state.WaitForSeRemoval;
import org.eclipse.keyple.core.seproxy.plugin.local.state.WaitForStartDetect;
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol;
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PcscReaderImpl extends AbstractObservableLocalReader
        implements PcscReader, SmartInsertionReader, SmartRemovalReader {

    private static final Logger logger = LoggerFactory.getLogger(PcscReaderImpl.class);

    private static final String PROTOCOL_T0 = "T=0";
    private static final String PROTOCOL_T1 = "T=1";
    private static final String PROTOCOL_T_CL = "T=CL";
    private static final String PROTOCOL_ANY = "T=0";

    private final CardTerminal terminal;

    private String parameterCardProtocol;
    private boolean cardExclusiveMode;
    private boolean cardReset;
    private TransmissionMode transmissionMode;

    private Card card;
    private CardChannel channel;

    // the latency delay value (in ms) determines the maximum time during which the
    // waitForCardPresent and waitForCardPresent blocking functions will execute.
    // This will correspond to the capacity to react to the interrupt signal of
    // the thread (see cancel method of the Future object)
    private final long insertLatency = 500;
    private final long removalLatency = 500;

    private final long insertWaitTimeout = 200;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    final private AtomicBoolean loopWaitSe = new AtomicBoolean();
    final private AtomicBoolean loopWaitSeRemoval = new AtomicBoolean();

    private final boolean usePingPresence;

    /**
     * This constructor should only be called by PcscPlugin PCSC reader parameters are initialized
     * with their default values as defined in setParameter. See
     * {@link #setParameter(String, String)} for more details
     *
     * @param pluginName the name of the plugin
     * @param terminal the PC/SC terminal
     */
    protected PcscReaderImpl(String pluginName, CardTerminal terminal) {
        super(pluginName, terminal.getName());
        this.terminal = terminal;
        this.card = null;
        this.channel = null;


        String OS = System.getProperty("os.name").toLowerCase();
        usePingPresence = OS.indexOf("mac") >= 0;
        logger.info("System detected : {}, is macOs checkPresence ping activated {}", OS,
                usePingPresence);

        this.stateService = initStateService();

        logger.info("[{}] constructor => using terminal ", terminal);


        // Using null values to use the standard method for defining default values
        try {
            setParameter(SETTING_KEY_TRANSMISSION_MODE, null);
            setParameter(SETTING_KEY_PROTOCOL, null);
            setParameter(SETTING_KEY_MODE, null);
            setParameter(SETTING_KEY_DISCONNECT, null);
        } catch (KeypleBaseException ex) {
            // can not fail with null value
        }
    }

    @Override
    public ObservableReaderStateService initStateService() {

        Map<AbstractObservableState.MonitoringState, AbstractObservableState> states =
                new HashMap<AbstractObservableState.MonitoringState, AbstractObservableState>();
        states.put(AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION,
                new WaitForStartDetect(this));

        // should the SmartInsertionMonitoringJob be used?
        if (!usePingPresence) {
            // use the SmartInsertionMonitoringJob
            states.put(AbstractObservableState.MonitoringState.WAIT_FOR_SE_INSERTION,
                    new WaitForSeInsertion(this, new SmartInsertionMonitoringJob(this),
                            executorService));
        } else {
            // use the CardPresentMonitoring job (only on Mac due to jvm crash)
            // https://github.com/eclipse/keyple-java/issues/153
            states.put(AbstractObservableState.MonitoringState.WAIT_FOR_SE_INSERTION,
                    new WaitForSeInsertion(this,
                            new CardPresentMonitoringJob(this, insertWaitTimeout, true),
                            executorService));
        }

        states.put(AbstractObservableState.MonitoringState.WAIT_FOR_SE_PROCESSING,
                new WaitForSeProcessing(this, new SmartRemovalMonitoringJob(this),
                        executorService));

        states.put(AbstractObservableState.MonitoringState.WAIT_FOR_SE_REMOVAL,
                new WaitForSeRemoval(this, new SmartRemovalMonitoringJob(this), executorService));


        return new ObservableReaderStateService(this, states,
                AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION);
    }

    @Override
    protected void closePhysicalChannel() throws KeypleChannelControlException {
        try {
            if (card != null) {
                logger.trace("[{}] closePhysicalChannel => closing the channel.", this.getName());

                channel = null;
                card.disconnect(cardReset);
                card = null;
            } else {
                logger.trace("[{}] closePhysicalChannel => card object is null.", this.getName());

            }
        } catch (CardException e) {
            throw new KeypleChannelControlException("Error while closing physical channel", e);
        }
    }

    @Override
    protected boolean checkSePresence() throws KeypleIOReaderException {
        try {
            return terminal.isCardPresent();
        } catch (CardException e) {
            logger.trace("[{}] Exception occurred in isSePresent. Message: {}", this.getName(),
                    e.getMessage());
            throw new KeypleIOReaderException("Exception occurred in isSePresent", e);
        }
    }

    /*
     * Implements from SmartInsertionReader
     */
    @Override
    public boolean waitForCardPresent() throws KeypleIOReaderException {
        logger.trace("[{}] waitForCardPresent => loop with latency of {} ms.", this.getName(),
                insertLatency);

        // activate loop
        loopWaitSe.set(true);

        try {
            while (loopWaitSe.get()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("[{}] waitForCardPresent => looping", this.getName());
                }
                if (terminal.waitForCardPresent(insertLatency)) {
                    // card inserted
                    return true;
                } else {
                    if (Thread.interrupted()) {
                        logger.trace("[{}] waitForCardPresent => task has been cancelled",
                                this.getName());
                        // task has been cancelled
                        return false;
                    }
                }
            }
            // if loop was stopped
            return false;
        } catch (CardException e) {
            throw new KeypleIOReaderException(
                    "[" + this.getName() + "] Exception occurred in waitForCardPresent. "
                            + "Message: " + e.getMessage());
        } catch (Throwable t) {
            // can or can not happen depending on terminal.waitForCardPresent
            logger.trace("[{}] waitForCardPresent => Throwable catched {}", this.getName(),
                    t.getCause());
            return false;
        }

    }

    /*
     * Implements from SmartInsertionReader
     */
    @Override
    public void stopWaitForCard() {
        loopWaitSe.set(false);
    }


    /**
     * Wait for the card absent event from smartcard.io
     * 
     * @return true if the card is removed within the delay
     */
    @Override
    public boolean waitForCardAbsentNative() throws KeypleIOReaderException {
        logger.trace("[{}] waitForCardAbsentNative => loop with latency of {} ms.", this.getName(),
                removalLatency);

        loopWaitSeRemoval.set(true);

        try {
            while (loopWaitSeRemoval.get()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("[{}] waitForCardAbsentNative => looping", this.getName());
                }
                if (terminal.waitForCardAbsent(removalLatency)) {
                    // card removed
                    return true;
                } else {
                    if (Thread.interrupted()) {
                        logger.trace("[{}] waitForCardAbsentNative => task has been cancelled",
                                this.getName());
                        // task has been cancelled
                        return false;
                    }
                }
            }
            return false;
        } catch (CardException e) {
            throw new KeypleIOReaderException(
                    "[" + this.getName() + "] Exception occurred in waitForCardAbsentNative. "
                            + "Message: " + e.getMessage());
        } catch (Throwable t) {
            // can or can not happen depending on terminal.waitForCardAbsent
            logger.trace("[{}] waitForCardAbsentNative => Throwable catched {}", this.getName(),
                    t.getCause());
            return false;
        }
    }

    /*
     * Implements from SmartRemovalReader
     */
    @Override
    public void stopWaitForCardRemoval() {
        loopWaitSeRemoval.set(false);
    }

    /**
     * Transmission of single APDU
     *
     * @param apduIn APDU in buffer
     * @return apduOut buffer
     * @throws KeypleIOReaderException if the transmission failed
     */
    @Override
    protected byte[] transmitApdu(byte[] apduIn) throws KeypleIOReaderException {
        ResponseAPDU apduResponseData;

        if (channel != null) {
            try {
                apduResponseData = channel.transmit(new CommandAPDU(apduIn));
            } catch (CardException e) {
                throw new KeypleIOReaderException(this.getName() + ":" + e.getMessage());
            } catch (IllegalArgumentException e) {
                // card could have been removed prematurely
                throw new KeypleIOReaderException(this.getName() + ":" + e.getMessage());
            }
        } else {
            // could occur if the SE was removed
            throw new KeypleIOReaderException(this.getName() + ": null channel.");
        }
        return apduResponseData.getBytes();
    }

    /**
     * Tells if the current SE protocol matches the provided protocol flag. If the protocol flag is
     * not defined (null), we consider here that it matches. An exception is returned when the
     * provided protocolFlag is not found in the current protocolMap.
     *
     * @param protocolFlag the protocol flag
     * @return true if the current SE matches the protocol flag
     * @throws KeypleReaderException if the protocol mask is not found
     */
    @Override
    protected boolean protocolFlagMatches(SeProtocol protocolFlag) throws KeypleReaderException {
        boolean result;
        // Test protocolFlag to check if ATR based protocol filtering is required
        if (protocolFlag != null) {
            if (!isPhysicalChannelOpen()) {
                openPhysicalChannel();
            }
            // the requestSet will be executed only if the protocol match the requestElement
            String selectionMask = protocolsMap.get(protocolFlag);
            if (selectionMask == null) {
                throw new KeypleReaderException("Target selector mask not found!", null);
            }
            Pattern p = Pattern.compile(selectionMask);
            String atr = ByteArrayUtil.toHex(card.getATR().getBytes());
            if (!p.matcher(atr).matches()) {
                logger.debug(
                        "[{}] protocolFlagMatches => unmatching SE. PROTOCOLFLAG = {}, ATR = {}, MASK = {}",
                        this.getName(), protocolFlag, atr, selectionMask);

                result = false;
            } else {
                logger.debug("[{}] protocolFlagMatches => matching SE. PROTOCOLFLAG = {}",
                        this.getName(), protocolFlag);

                result = true;
            }
        } else {
            // no protocol defined returns true
            result = true;
        }
        return result;
    }

    /**
     * Set a parameter.
     * <p>
     * These are the parameters you can use with their associated values:
     * <ul>
     * <li><strong>protocol</strong>:
     * <ul>
     * <li>Tx: Automatic negotiation (default)</li>
     * <li>T0: T0 protocol</li>
     * <li>T1: T1 protocol</li>
     * </ul>
     * </li>
     * <li><strong>mode</strong>:
     * <ul>
     * <li>shared: Shared between apps and threads (default)</li>
     * <li>exclusive: Exclusive to this app and the current thread</li>
     * </ul>
     * </li>
     * <li><strong>disconnect</strong>:
     * <ul>
     * <li>reset: Reset the card</li>
     * <li>unpower: Simply unpower it</li>
     * <li>leave: Unsupported</li>
     * <li>eject: Eject</li>
     * </ul>
     * </li>
     * <li><strong>thread_wait_timeout</strong>: Number of milliseconds to wait</li>
     * </ul>
     *
     * @param name Parameter name
     * @param value Parameter value
     * @throws KeypleBaseException This method can fail when disabling the exclusive mode as it's
     *         executed instantly
     * @throws IllegalArgumentException when parameter is wrong
     *
     *
     */
    @Override
    public void setParameter(String name, String value)
            throws IllegalArgumentException, KeypleBaseException {

        logger.trace("[{}] setParameter => PCSC: Set a parameter. NAME = {}, VALUE = {}",
                this.getName(), name, value);

        if (name == null) {
            throw new IllegalArgumentException("Parameter shouldn't be null");
        }
        if (name.equals(SETTING_KEY_TRANSMISSION_MODE)) {
            if (value == null) {
                transmissionMode = null;
            } else if (value.equals(SETTING_TRANSMISSION_MODE_CONTACTS)) {
                transmissionMode = TransmissionMode.CONTACTS;
            } else if (value.equals(SETTING_TRANSMISSION_MODE_CONTACTLESS)) {
                transmissionMode = TransmissionMode.CONTACTLESS;
            } else {
                throw new IllegalArgumentException("Bad tranmission mode " + name + " : " + value);
            }
        } else if (name.equals(SETTING_KEY_PROTOCOL)) {
            if (value == null || value.equals(SETTING_PROTOCOL_TX)) {
                parameterCardProtocol = "*";
            } else if (value.equals(SETTING_PROTOCOL_T0)) {
                parameterCardProtocol = "T=0";
            } else if (value.equals(SETTING_PROTOCOL_T1)) {
                parameterCardProtocol = "T=1";
            } else if (value.equals(SETTING_PROTOCOL_T_CL)) {
                parameterCardProtocol = "T=CL";
            } else {
                throw new IllegalArgumentException("Bad protocol " + name + " : " + value);
            }
        } else if (name.equals(SETTING_KEY_MODE)) {
            if (value == null || value.equals(SETTING_MODE_SHARED)) {
                if (cardExclusiveMode && card != null) {
                    try {
                        card.endExclusive();
                    } catch (CardException e) {
                        throw new KeypleReaderException("Couldn't disable exclusive mode", e);
                    }
                }
                cardExclusiveMode = false;
            } else if (value.equals(SETTING_MODE_EXCLUSIVE)) {
                cardExclusiveMode = true;
            } else {
                throw new IllegalArgumentException(
                        "Parameter value not supported " + name + " : " + value);
            }
        } else if (name.equals(SETTING_KEY_DISCONNECT)) {
            if (value == null || value.equals(SETTING_DISCONNECT_RESET)) {
                cardReset = true;
            } else if (value.equals(SETTING_DISCONNECT_UNPOWER)) {
                cardReset = false;
            } else if (value.equals(SETTING_DISCONNECT_EJECT)
                    || value.equals(SETTING_DISCONNECT_LEAVE)) {
                throw new IllegalArgumentException(
                        "This disconnection parameter is not supported by this plugin" + name
                                + " : " + value);
            } else {
                throw new IllegalArgumentException(
                        "Parameters not supported : " + name + " : " + value);
            }
        } else {
            throw new IllegalArgumentException(
                    "This parameter is unknown !" + name + " : " + value);
        }
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<String, String>();

        // Returning the protocol
        String protocol = parameterCardProtocol;
        if (protocol.equals("*")) {
            protocol = SETTING_PROTOCOL_TX;
        } else if (protocol.equals("T=0")) {
            protocol = SETTING_PROTOCOL_T0;
        } else if (protocol.equals("T=1")) {
            protocol = SETTING_PROTOCOL_T1;
        } else {
            throw new IllegalStateException("Illegal protocol: " + protocol);
        }
        parameters.put(SETTING_KEY_PROTOCOL, protocol);

        // The mode ?
        if (!cardExclusiveMode) {
            parameters.put(SETTING_KEY_MODE, SETTING_MODE_SHARED);
        }

        return parameters;
    }

    @Override
    protected byte[] getATR() {
        return card.getATR().getBytes();
    }

    /**
     * Tells if a physical channel is open
     * <p>
     * This status may be wrong if the card has been removed.
     * <p>
     * The caller should test the card presence with isSePresent before calling this method.
     *
     * @return true if the physical channel is open
     */
    @Override
    protected boolean isPhysicalChannelOpen() {
        return card != null;
    }

    /**
     * Opens a physical channel
     *
     * The card access may be set to 'Exclusive' through the reader's settings.
     *
     * In this case be aware that on some platforms (ex. Windows 8+), the exclusivity is granted for
     * a limited time (ex. 5 seconds). After this delay, the card is automatically resetted.
     *
     * @throws KeypleChannelControlException if a reader error occurs
     */
    @Override
    protected void openPhysicalChannel() throws KeypleChannelControlException {
        // init of the physical SE channel: if not yet established, opening of a new physical
        // channel
        try {
            if (card == null) {
                this.card = this.terminal.connect(parameterCardProtocol);
                if (cardExclusiveMode) {
                    card.beginExclusive();
                    logger.debug("[{}] Opening of a physical SE channel in exclusive mode.",
                            this.getName());

                } else {
                    logger.debug("[{}] Opening of a physical SE channel in shared mode.",
                            this.getName());

                }
            }
            this.channel = card.getBasicChannel();
        } catch (CardException e) {
            throw new KeypleChannelControlException("Error while opening Physical Channel", e);
        }
    }

    /**
     * The transmission mode can set with setParameter(SETTING_KEY_TRANSMISSION_MODE, )
     * <p>
     * When the transmission mode has not explicitly set, it is deduced from the protocol:
     * <ul>
     * <li>T=0: contacts mode</li>
     * <li>T=1: contactless mode</li>
     * </ul>
     * 
     * @return the current transmission mode
     */
    @Override
    public TransmissionMode getTransmissionMode() {
        if (transmissionMode != null) {
            return transmissionMode;
        } else {
            if (parameterCardProtocol.contentEquals(PROTOCOL_T1)
                    || parameterCardProtocol.contentEquals(PROTOCOL_T_CL)) {
                return TransmissionMode.CONTACTLESS;
            } else {
                return TransmissionMode.CONTACTS;
            }
        }
    }


}
