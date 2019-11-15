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

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.event.ObservablePlugin;
import org.eclipse.keyple.core.seproxy.event.PluginEvent;
import org.eclipse.keyple.core.seproxy.plugin.AbstractPlugin;

public class ReaderPluginSlave extends AbstractPlugin implements ObservablePlugin {
    /**
     * Instantiates a new ReaderPluginSlave.
     * 
     * @param name name of the plugin
     */
    protected ReaderPluginSlave(String name) {
        super(name);
    }

    /**
     * Add a SeReader tp the current reader Set
     * 
     * @param seReader
     * @return true if the SeReader has been added
     */
    boolean addReader(SeReader seReader) {
        if (seReader != null && readers.add(seReader)) {
            notifyObservers(new PluginEvent(this.getName(), seReader.getName(),
                    PluginEvent.EventType.READER_CONNECTED));
            return true;
        }
        return false;
    }

    /**
     * Remove a SeReader from the current reader Set
     * 
     * @param seReader the SeReader to remove
     * @return true if the SeReader has been removed
     */
    boolean removeReader(SeReader seReader) {
        if (seReader != null) {
            notifyObservers(new PluginEvent(this.getName(), seReader.getName(),
                    PluginEvent.EventType.READER_DISCONNECTED));
            return readers.remove(seReader);
        }
        return false;
    }

    @Override
    protected SortedSet<SeReader> initNativeReaders() {
        readers = new TreeSet<SeReader>();
        return readers;
    }

    @Override
    public Map<String, String> getParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setParameter(String key, String value) {
        throw new UnsupportedOperationException();
    }
}
