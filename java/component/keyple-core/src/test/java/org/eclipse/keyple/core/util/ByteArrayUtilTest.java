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
package org.eclipse.keyple.core.util;

import static org.junit.Assert.*;
import org.junit.Test;

public class ByteArrayUtilTest {
    private final static String HEXSTRING_ODD = "0102030";
    private final static String HEXSTRING_BAD = "010203ABGH80";
    private final static String HEXSTRING_GOOD = "1234567890ABCDEFFEDCBA0987654321";
    private final static byte[] BYTEARRAY_GOOD =
            new byte[] {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x90,
                    (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0xFE, (byte) 0xDC, (byte) 0xBA,
                    (byte) 0x09, (byte) 0x87, (byte) 0x65, (byte) 0x43, (byte) 0x21};
    private final static byte[] BYTEARRAY_LEN_2 = new byte[] {(byte) 0x12, (byte) 0x34};
    private final static byte[] BYTEARRAY_LEN_3 =
            new byte[] {(byte) 0x12, (byte) 0x34, (byte) 0x56};

    @Test(expected = NullPointerException.class)
    public void fromHex_null() {
        byte[] bytes = ByteArrayUtil.fromHex(null);
    }

    @Test
    public void fromHex_empty() {
        byte[] bytes = ByteArrayUtil.fromHex("");
        assertEquals(bytes.length, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromHex_odd_length() {
        byte[] bytes = ByteArrayUtil.fromHex(HEXSTRING_ODD);
    }

    @Test
    public void fromHex_bad_hex() {
        // no verification is being carried out at the moment.
        byte[] bytes = ByteArrayUtil.fromHex(HEXSTRING_BAD);
    }

    @Test
    public void fromHex_good_hex() {
        // no verification is being carried out at the moment.
        byte[] bytes = ByteArrayUtil.fromHex(HEXSTRING_GOOD);
        assertArrayEquals(bytes, BYTEARRAY_GOOD);
    }

    @Test
    public void toHex_null() {
        String hex = ByteArrayUtil.toHex(null);
        assertEquals(hex.length(), 0);
    }

    @Test
    public void toHex_empty() {
        byte[] bytes = new byte[0];
        String hex = ByteArrayUtil.toHex(bytes);
        assertEquals(hex.length(), 0);
    }

    @Test
    public void toHex_bytearray_good() {
        String hex = ByteArrayUtil.toHex(BYTEARRAY_GOOD);
        assertEquals(hex, HEXSTRING_GOOD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void threeBytesToInt_null() {
        int value = ByteArrayUtil.threeBytesToInt(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void threeBytesToInt_negative_offset() {
        int value = ByteArrayUtil.threeBytesToInt(BYTEARRAY_GOOD, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void threeBytesToInt_too_short_buffer_1() {
        int value = ByteArrayUtil.threeBytesToInt(BYTEARRAY_LEN_2, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void threeBytesToInt_too_short_buffer_2() {
        int value = ByteArrayUtil.threeBytesToInt(BYTEARRAY_LEN_3, 1);
    }

    @Test
    public void threeBytesToInt_buffer_ok_1() {
        int value = ByteArrayUtil.threeBytesToInt(BYTEARRAY_LEN_3, 0);
        assertEquals(0x123456, value);
    }

    @Test
    public void threeBytesToInt_buffer_ok_2() {
        int value = ByteArrayUtil.threeBytesToInt(BYTEARRAY_GOOD, 0);
        assertEquals(0x123456, value);
    }

    @Test
    public void threeBytesToInt_buffer_ok_3() {
        int value = ByteArrayUtil.threeBytesToInt(BYTEARRAY_GOOD, 1);
        assertEquals(0x345678, value);
    }

    @Test
    public void threeBytesToInt_buffer_ok_4() {
        int value = ByteArrayUtil.threeBytesToInt(BYTEARRAY_GOOD, 4);
        assertEquals(0x90ABCD, value);
    }

    @Test
    public void threeBytesToInt_buffer_ok_5() {
        int value = ByteArrayUtil.threeBytesToInt(BYTEARRAY_GOOD, 13);
        assertEquals(0x654321, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_1() {
        int value = ByteArrayUtil.threeBytesSignedToInt(BYTEARRAY_LEN_3, 0);
        assertEquals(0x123456, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_2() {
        int value = ByteArrayUtil.threeBytesSignedToInt(BYTEARRAY_GOOD, 0);
        assertEquals(0x123456, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_3() {
        int value = ByteArrayUtil.threeBytesSignedToInt(BYTEARRAY_GOOD, 1);
        assertEquals(0x345678, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_4() {
        int value = ByteArrayUtil.threeBytesSignedToInt(BYTEARRAY_GOOD, 4);
        assertEquals(-7296051, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_5() {
        int value = ByteArrayUtil.threeBytesSignedToInt(BYTEARRAY_GOOD, 13);
        assertEquals(0x654321, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_6() {
        int value = ByteArrayUtil.threeBytesSignedToInt(ByteArrayUtil.fromHex("000000"), 0);
        assertEquals(0, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_7() {
        int value = ByteArrayUtil.threeBytesSignedToInt(ByteArrayUtil.fromHex("000100"), 0);
        assertEquals(256, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_8() {
        int value = ByteArrayUtil.threeBytesSignedToInt(ByteArrayUtil.fromHex("010000"), 0);
        assertEquals(65536, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_9() {
        int value = ByteArrayUtil.threeBytesSignedToInt(ByteArrayUtil.fromHex("FFFFFF"), 0);
        assertEquals(-1, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_10() {
        int value = ByteArrayUtil.threeBytesSignedToInt(ByteArrayUtil.fromHex("800000"), 0);
        assertEquals(-8388608, value);
    }

    @Test
    public void threeBytesSignedToInt_buffer_ok_11() {
        int value = ByteArrayUtil.threeBytesSignedToInt(ByteArrayUtil.fromHex("7FFFFF"), 0);
        assertEquals(8388607, value);
    }
}
