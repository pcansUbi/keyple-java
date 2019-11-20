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

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.eclipse.keyple.core.seproxy.ReaderPlugin;
import org.eclipse.keyple.core.seproxy.event.ObservablePlugin;
import org.eclipse.keyple.core.seproxy.event.PluginEvent;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PluginHub class provides {@link ReaderPlugin}s as subsets of the base plugin provided in the
 * constructor.
 * <p>
 * It allows slave plugins to be registered in order to receive event notifications from the base
 * plugin (connecting and disconnecting readers) according to a filtering based on the name of the
 * readers.
 */
public class PluginHub implements ObservablePlugin.PluginObserver {
    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(PluginHub.class);

    /**
     * The base plugin from which events will be received and the readers retrieved..
     */
    private final ReaderPlugin readerPlugin;

    /**
     * The list of registered slave plugins
     */
    private final SortedSet<RegisteredSlavePlugin> registeredSlavePlugins =
            new TreeSet<RegisteredSlavePlugin>();

    /**
     * Instantiates a PluginHub from the provided base plugin.
     *
     * @param readerPlugin the base plugin
     */
    public PluginHub(ReaderPlugin readerPlugin) {
        this.readerPlugin = readerPlugin;
    }

    /**
     * Register a slave plugin
     * 
     * @param readerPluginSlave the slave plugin object
     * @param readerNameFilter a filter applied to the name of the reader
     * @return true if the slave plugin has been registered successfully
     */
    boolean registerSlavePlugin(ReaderPluginSlave readerPluginSlave, String readerNameFilter) {
        RegisteredSlavePlugin registeredSlavePlugin =
                new RegisteredSlavePlugin(readerPluginSlave, readerNameFilter);
        if (!registeredSlavePlugins.contains(registeredSlavePlugin)) {
            return registeredSlavePlugins.add(registeredSlavePlugin);
        }
        return false;
    }

    /**
     * Starts the base plugin observation
     */
    void startObservation() {
        ((ObservablePlugin) readerPlugin).addObserver(this);
    }

    /**
     * Stops the base plugin observation
     */
    void stopObservation() {
        ((ObservablePlugin) readerPlugin).removeObserver(this);
    }

    /**
     * Receives and forwards events from the base plugin to the registered slave plugins.
     * 
     * @param event the base plugin event
     */
    @Override
    public void update(PluginEvent event) {
        for (String readerName : event.getReaderNames()) {
            for (RegisteredSlavePlugin registeredSlavePlugin : registeredSlavePlugins) {
                if (registeredSlavePlugin.isNameMatching(readerName)) {
                    try {
                        if (event.getEventType() == PluginEvent.EventType.READER_CONNECTED) {
                            registeredSlavePlugin.getReaderPluginSlave()
                                    .addReader(readerPlugin.getReader(readerName));
                        } else {
                            registeredSlavePlugin.getReaderPluginSlave()
                                    .removeReader(readerPlugin.getReader(readerName));
                        }
                    } catch (KeypleReaderNotFoundException e) {
                        logger.error("KeypleReaderNotFoundException: {}", e);
                    }
                }
            }
        }
    }

    /**
     * Class associating a {@link ReaderPluginSlave} and a filter on the name of the reader.
     * <p>
     * Allows the creation of a list, implements the Comparable interface to enable the use of the
     * "contains" method with the built lists.
     */
    private class RegisteredSlavePlugin implements Comparable {
        private final ReaderPluginSlave readerPluginSlave;
        private final String readerNameFilter;

        /**
         * Instantiation with ReaderPluginSlave and regex String
         * 
         * @param readerPluginSlave the {@link ReaderPluginSlave}
         * @param readerNameFilter a regular expression
         */
        RegisteredSlavePlugin(ReaderPluginSlave readerPluginSlave, String readerNameFilter) {
            if (readerPluginSlave == null || readerNameFilter == null) {
                throw new IllegalArgumentException("Null argument not allowed.");
            }
            this.readerPluginSlave = readerPluginSlave;
            this.readerNameFilter = readerNameFilter;
        }

        public ReaderPluginSlave getReaderPluginSlave() {
            return readerPluginSlave;
        }

        public String getReaderNameFilter() {
            return readerNameFilter;
        }

        public boolean isNameMatching(String readerName) {
            return Pattern.matches(readerNameFilter, readerName);
        }

        @Override
        public int compareTo(Object obj) {
            if (!(obj instanceof RegisteredSlavePlugin)) {
                return -1;
            }
            return ((RegisteredSlavePlugin) obj).getReaderPluginSlave().getName()
                    .compareTo(this.readerPluginSlave.getName());
        }
    }
}
