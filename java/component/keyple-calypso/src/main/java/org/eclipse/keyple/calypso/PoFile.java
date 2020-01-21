/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
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

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Defines a file structure
 */
public class PoFile implements PoData {
    private final byte sfi;
    private final SortedMap<Integer, byte[]> records = new TreeMap<Integer, byte[]>();
    private final String extraInfo;

    /**
     * Constructor
     * 
     * @param sfi the short file identifier
     * @param record the record number
     * @param data the associated data
     * @param extraInfo information string
     */
    public PoFile(byte sfi, byte record, byte[] data, String extraInfo) {
        this.sfi = sfi;
        this.extraInfo = extraInfo;
        setRecord(record, data);
    }

    /**
     * Constructor
     * 
     * @param sfi the short file identifier
     * @param records a Map of records
     * @param extraInfo information string
     */
    public PoFile(byte sfi, SortedMap<Integer, byte[]> records, String extraInfo) {
        this.sfi = sfi;
        this.extraInfo = extraInfo;
        setRecords(records);
    }

    @Override
    public Type getType() {
        return Type.FILE;
    }

    /**
     * @return the SFI of the file
     */
    public byte getSfi() {
        return sfi;
    }

    /**
     * Sets a record
     * 
     * @param record the record number
     * @param data the associated data
     */
    public void setRecord(byte record, byte[] data) {
        this.records.put((int) record, data);
    }

    /**
     * Sets several records at once
     * 
     * @param records a Map of records
     */
    public void setRecords(SortedMap<Integer, byte[]> records) {
        this.records.putAll(records);
    }

    /**
     * Retrieves the data of a record
     * 
     * @param record the record number
     * @return a byte array containing the data or null if the data doesn't exist
     */
    public byte[] getRecord(byte record) {
        return records.get((int) record);
    }

    /**
     * @return the Map containing all records
     */
    public SortedMap<Integer, byte[]> getRecords() {
        return records;
    }

    /**
     * @return the extraInfo String
     */
    public String getExtraInfo() {
        return extraInfo;
    }
}
