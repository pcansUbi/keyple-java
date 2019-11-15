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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import org.eclipse.keyple.core.seproxy.ReaderPlugin;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PcscPoolPluginImpl implements PcscPoolPlugin {
    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(PcscPoolPluginImpl.class);

    private final static int DEFAULT_MAX_ALLOCATION_TIME_MS = 15000;
    private int maxAllocationTimeMs = DEFAULT_MAX_ALLOCATION_TIME_MS;

    @Override
    public SortedSet<String> getReaderGroupReferences() {
        return null;
    }

    @Override
    public SeReader allocateReader(String groupReference) {
        return null;
    }

    @Override
    public void releaseReader(SeReader seReader) {

    }

    @Override
    public SortedSet<String> getReaderNames() {
        return null;
    }

    @Override
    public SortedSet<SeReader> getReaders() throws KeypleReaderException {
        return null;
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
}
