/*
 * Copyright (c) 2018, Loong Wan (https://github.com/loong10k).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.pac4j.yiban.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES decryption utility used by the YiBan light-application SDK.
 *
 * <p>This class provides AES/CBC/NoPadding decryption of hex-encoded
 * ciphertext.  It is used internally to decode the {@code verify_request}
 * parameter that YiBan passes to the application callback.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
public class AESDecoder {

    /** AES algorithm identifier. */
    final static String ALGORITHM = "AES";

    /** AES/CBC/NoPadding transformation string. */
    final static String TRANSFORM = "AES/CBC/NoPadding";

    /**
     * Decrypt a hex-encoded AES/CBC/NoPadding ciphertext.
     *
     * @param text the hex-encoded ciphertext to decrypt
     * @param key  the AES secret key (16 bytes); if longer than 16 characters
     *             only the first 16 are used
     * @param iv   the initialisation vector; only the first 16 characters are used
     * @return the decrypted plaintext string
     * @throws Exception if decryption fails for any cryptographic reason
     */
    public static String dec(String text, String key, String iv)
            throws Exception {
        SecretKeySpec keyval;
        if (iv.length() == 16) {
            keyval = new SecretKeySpec(key.getBytes(), ALGORITHM);
        } else {
            keyval = new SecretKeySpec(key.substring(0, 16).getBytes(), ALGORITHM);
        }
        IvParameterSpec ivspec = new IvParameterSpec(iv.substring(0, 16).getBytes());
        Cipher cipher = Cipher.getInstance(TRANSFORM);
        cipher.init(Cipher.DECRYPT_MODE, keyval, ivspec);
        byte[] buffer = hexToBin(text);
        byte[] decode = cipher.doFinal(buffer);
        return new String(decode);
    }

    /**
     * Convert a hexadecimal string to a byte array.
     *
     * @param text the hexadecimal string (must have an even number of characters)
     * @return the decoded byte array, or {@code null} if the input is empty
     */
    public static byte[] hexToBin(String text) {
        if (text.length() < 1) {
            return null;
        }
        int len = text.length() / 2;
        byte[] result = new byte[text.length() / 2];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) (0xff & Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16));
        }
        return result;
    }
}
