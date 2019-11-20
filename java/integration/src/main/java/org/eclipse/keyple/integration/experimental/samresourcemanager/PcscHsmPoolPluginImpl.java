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


import static org.eclipse.keyple.calypso.command.sam.SamRevision.AUTO;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.eclipse.keyple.calypso.transaction.*;
import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoNoSamResourceException;
import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoSamResourceFailureException;
import org.eclipse.keyple.core.selection.SeSelection;
import org.eclipse.keyple.core.selection.SelectionsResult;
import org.eclipse.keyple.core.seproxy.ReaderPlugin;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.event.ObservablePlugin;
import org.eclipse.keyple.core.seproxy.event.PluginEvent;
import org.eclipse.keyple.core.seproxy.event.PluginEvent.*;
import org.eclipse.keyple.core.seproxy.exception.*;
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols;
import org.eclipse.keyple.plugin.pcsc.PcscProtocolSetting;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PcscHsmPoolPluginImpl implements PcscHsmPoolPlugin, ObservablePlugin.PluginObserver {
    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(PcscHsmPoolPluginImpl.class);

    private final SortedSet<ConnectedReader> connectedReaders = new TreeSet<ConnectedReader>();

    private final ReaderPlugin baseReaderPlugin;

    private static final int DEFAULT_MAX_ALLOCATION_TIME_MS = 15000;
    private int maxAllocationTimeMs = DEFAULT_MAX_ALLOCATION_TIME_MS;

    public PcscHsmPoolPluginImpl(ReaderPlugin baseReaderPlugin) {
        this.baseReaderPlugin = baseReaderPlugin;
        if (!(baseReaderPlugin instanceof ObservablePlugin)) {
            throw new IllegalStateException("A local pool plugin needs an observable plugin. "
                    + baseReaderPlugin.getName() + " is not observable.");
        }
        ((ObservablePlugin) baseReaderPlugin).addObserver(this);
    }

    @Override
    public SortedSet<String> getReaderGroupReferences() {
        return null;
    }

    @Override
    public SeReader allocateReader(String groupReference) {
        for (ConnectedReader connectedReader : connectedReaders) {
            if (connectedReader.allocate()) {
                return connectedReader.getReader();
            }
        }
        return null;
    }

    @Override
    public void releaseReader(SeReader seReader) {
        for (ConnectedReader connectedReader : connectedReaders) {
            if (connectedReader.getName().equals(seReader.getName())) {
                connectedReader.release();
            }
        }
    }

    @Override
    public SortedSet<String> getReaderNames() {
        SortedSet<String> readerNames = new TreeSet<String>();
        for (ConnectedReader connectedReader : connectedReaders) {
            readerNames.add(connectedReader.getName());
        }
        return readerNames;
    }

    @Override
    public SortedSet<SeReader> getReaders() {
        SortedSet<SeReader> readers = new TreeSet<SeReader>();
        for (ConnectedReader connectedReader : connectedReaders) {
            readers.add(connectedReader.getReader());
        }
        return readers;
    }

    @Override
    public SeReader getReader(String name) throws KeypleReaderNotFoundException {
        return null;
    }

    /**
     * Get the current plugin parameters set with setParameter and/or setParameters
     *
     * @return a Map containing the parameters
     */
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parametersMap = new HashMap<String, String>();
        parametersMap.put(MAX_CHANNEL_ALLOCATION_TIME_MS, Integer.toString(maxAllocationTimeMs));
        return parametersMap;
    }

    /**
     * Set a parameter defined by a key and a value
     *
     * @param key the parameter key
     * @param value the parameter value
     * @throws IllegalArgumentException if the key or the value is not correct
     */
    @Override
    public void setParameter(String key, String value) throws IllegalArgumentException {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Null key or value");
        }
        if (key.equals(MAX_CHANNEL_ALLOCATION_TIME_MS)) {
            try {
                maxAllocationTimeMs = Integer.parseInt(value);
                if (maxAllocationTimeMs < 0) {
                    throw new IllegalArgumentException(
                            "Bad parameter MAX_CHANNEL_ALLOCATION_TIME_MS value (must be positive).");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad number format: " + value);
            }
        } else {
            throw new IllegalArgumentException("Unknown parameter: " + key);
        }
    }

    /**
     * Define a set of parameters placed in a Map at once (see getParameters)
     *
     * @param parameters Parameters to setup
     * @throws IllegalArgumentException if one of the parameter is not correct
     */
    @Override
    public void setParameters(Map<String, String> parameters) throws IllegalArgumentException {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            setParameter(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Get the plugin name
     *
     * @return String
     */
    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    /**
     * Compare the name of the current ReaderPlugin to the name of the ReaderPlugin provided in
     * argument
     *
     * @param readerPlugin a {@link ReaderPlugin} object
     * @return true if the names match (The method is needed for the SortedSet lists)
     */
    @Override
    public int compareTo(ReaderPlugin readerPlugin) {
        return this.getName().compareTo(readerPlugin.getName());
    }

    @Override
    public void update(PluginEvent event) {
        for (String readerName : event.getReaderNames()) {
            ConnectedReader connectedReader;
            try {
                connectedReader = new ConnectedReader(baseReaderPlugin.getReader(readerName));
                if (event.getEventType() == EventType.READER_CONNECTED) {
                    if (!connectedReaders.contains(connectedReader)) {
                        connectedReaders.add(connectedReader);
                    } else {
                        logger.error(
                                "Reader {} is already present in the list of connected readers.",
                                readerName);
                    }
                } else {
                    if (connectedReaders.contains(connectedReader)) {
                        connectedReaders.remove(connectedReader);
                    } else {
                        logger.error("Reader {} is not present in the list of connected readers.",
                                readerName);
                    }
                }
            } catch (KeypleReaderNotFoundException e) {
                logger.error("Error while getting reader.");
            }
        }
    }

    @Override
    public SamResource allocateSamResource(String s)
            throws KeypleCalypsoNoSamResourceException, KeypleCalypsoSamResourceFailureException {
        for (ConnectedReader connectedReader : connectedReaders) {
            if (connectedReader.allocate()) {
                return new SamResource(connectedReader.getReader(), null);
            }
        }
        throw new KeypleCalypsoNoSamResourceException("No SamResource available.");
    }

    @Override
    public void releaseSamResource(SamResource samResource)
            throws KeypleCalypsoSamResourceFailureException {

    }

    private class ConnectedReader implements Comparable {
        private final SeReader seReader;
        private String groupReference;
        private boolean allocated;
        private SamResource samResource;

        public ConnectedReader(SeReader seReader) {
            this.seReader = seReader;
            seReader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_ISO7816_3,
                    PcscProtocolSetting.PCSC_PROTOCOL_SETTING
                            .get(SeCommonProtocols.PROTOCOL_ISO7816_3));
            try {
                seReader.setParameter(PcscReader.SETTING_KEY_LOGGING, "true");
                seReader.setParameter(PcscReader.SETTING_KEY_PROTOCOL,
                        PcscReader.SETTING_PROTOCOL_T0);
                seReader.setParameter(PcscReader.SETTING_KEY_MODE, PcscReader.SETTING_MODE_SHARED);
            } catch (KeypleBaseException e) {
                logger.error("Failed to set parameters.");
            }
            allocated = false;
        }

        String getName() {
            return seReader.getName();
        }

        @Override
        public int compareTo(Object obj) {
            if (!(obj instanceof ConnectedReader)) {
                return -1;
            }
            return ((ConnectedReader) obj).getName().compareTo(this.getName());
        }

        public SeReader getReader() {
            return seReader;
        }

        public String getGroupReference() {
            return groupReference;
        }

        public void initSamResource() throws KeypleCalypsoSamResourceFailureException {
            SeSelection samSelection = new SeSelection();

            SamSelector samSelector = new SamSelector(new SamIdentifier(AUTO, null, null), "SAM");

            /* Prepare selector, ignore MatchingSe here */
            samSelection.prepareSelection(new SamSelectionRequest(samSelector));

            SelectionsResult selectionsResult = null;
            try {
                selectionsResult = samSelection.processExplicitSelection(seReader);
                if (selectionsResult.hasActiveSelection()) {
                    CalypsoSam calypsoSam =
                            (CalypsoSam) selectionsResult.getActiveSelection().getMatchingSe();
                    samResource = new SamResource(seReader, calypsoSam);
                } else {
                    logger.error("Unable to open a logical channel for SAM!");
                }
            } catch (KeypleReaderException e) {
                logger.error("IO Reader exception {}", e.getMessage());
            }
            throw new KeypleCalypsoSamResourceFailureException(
                    "SamResource not available for reader " + seReader.getName());
        }

        public SamResource getSamResource() throws KeypleCalypsoNoSamResourceException {
            if (samResource != null) {
                try {
                    if (samResource.getSeReader() != null
                            && samResource.getSeReader().isSePresent()) {
                        return samResource;
                    }
                } catch (KeypleIOReaderException e) {
                    // just log
                    logger.error("IO Reader exception {}", e.getMessage());
                }
            }
            throw new KeypleCalypsoNoSamResourceException(
                    "SamResource not available for reader " + seReader.getName());
        }

        /**
         *
         * @return
         */
        public boolean allocate() {
            if (allocated) {
                return false;
            }
            try {
                if (seReader.isSePresent()) {
                    allocated = true;
                }
                return true;
            } catch (KeypleIOReaderException e) {
                logger.error("isSePresent failed while locking the connected reader {}",
                        seReader.getName());
                return false;
            }
        }

        public void release() {
            allocated = false;
        }
    }
}
