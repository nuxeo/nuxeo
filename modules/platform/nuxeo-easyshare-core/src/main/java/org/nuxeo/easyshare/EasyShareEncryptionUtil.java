/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Sebastien Guillaume
 */

package org.nuxeo.easyshare;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @since 11.5
 */

public class EasyShareEncryptionUtil {

    // some random salt
    private static final byte[] SALT = { (byte) 0x21, (byte) 0x21, (byte) 0xF0, (byte) 0x55, (byte) 0xC3, (byte) 0x9F,
            (byte) 0x5A, (byte) 0x75 };

    private final static int ITERATION_COUNT = 31;

    private EasyShareEncryptionUtil() {
    }

    /**
     * Return a predefined SALT
     * 
     * @return Predefined SAL (@see SALT)
     */
    public static String getDefaultSalt() {

        return Base64.encodeBase64String(SALT);
    }

    /**
     * Generate a custom salt as base64 encoded string
     * 
     * @return random base64 string
     */
    public static String getNextSalt() {

        return Base64.encodeBase64String(getNextSaltAsByte());
    }

    /**
     * Encode an input string with passed salt string (default value will be used for salt if param is empty/null)
     * (wrapper for byte[] salt)
     * 
     * @param input String to encode
     * @param salt SALT to use
     * @return An encoded string
     */
    public static String encode(String input, String salt) {
        return encodeWithSaltByte(input, Base64.decodeBase64(salt));
    }

    /**
     * Decode an encoded token with passed salt string (default value will be used for salt if param is empty/null)
     * 
     * @param token Encoded String to decode using passed salt
     * @param salt SALT to use when decoding
     * @return Decoded string from token
     */
    public static String decode(String token, String salt) {
        return decodeWithSaltByte(token, Base64.decodeBase64(salt));
    }

    /**
     * Generate a random salt
     * 
     * @return random generated salt over 8 bytes
     */
    private static byte[] getNextSaltAsByte() {
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    /**
     * Encode an input string with passed salt byte[] (default value will be used for salt if param is empty/null)
     * 
     * @param input String to encode
     * @param salt SALT to use
     * @return An encoded string
     */
    private static String encodeWithSaltByte(String input, byte[] salt) {
        if (input == null) {
            // throw new IllegalArgumentException();
            throw new NuxeoException("Illegal argument, input must not be null", SC_BAD_REQUEST);
        }
        try {

            byte[] randomSalt = salt != null ? salt : SALT;

            KeySpec keySpec = new PBEKeySpec(null, randomSalt, ITERATION_COUNT);
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(randomSalt, ITERATION_COUNT);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);

            Cipher ecipher = Cipher.getInstance(key.getAlgorithm());
            ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

            byte[] enc = ecipher.doFinal(input.getBytes());

            String res = new String(Base64.encodeBase64(enc));
            // escapes for url
            res = URLEncoder.encode(res, StandardCharsets.UTF_8.toString());

            return res;

        } catch (GeneralSecurityException e) {
            throw new NuxeoException("Error encoding/crypting input data", e, SC_FORBIDDEN);
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException("Error escaping encoded base64 data for URL", e, SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Decode an encoded token with passed salt byte[] (default value will be used for salt if param is empty/null)
     * 
     * @param token Encoded String to decode using passed salt
     * @param salt SALT to use when decoding
     * @return Decoded string from token
     */
    private static String decodeWithSaltByte(String token, byte[] salt) {
        if (token == null) {
            // return null;
            throw new NuxeoException("Illegal argument, input must not be null", SC_BAD_REQUEST);
        }
        try {

            byte[] randomSalt = salt != null ? salt : SALT;

            String input = URLDecoder.decode(token, StandardCharsets.UTF_8.toString());

            byte[] dec = Base64.decodeBase64(input.getBytes());

            KeySpec keySpec = new PBEKeySpec(null, randomSalt, ITERATION_COUNT);
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(randomSalt, ITERATION_COUNT);

            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);

            Cipher dcipher = Cipher.getInstance(key.getAlgorithm());
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

            byte[] decoded = dcipher.doFinal(dec);

            String result = new String(decoded);
            return result;

        } catch (GeneralSecurityException e) {
            throw new NuxeoException("Error encoding/crypting input data", e, SC_FORBIDDEN);
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException("Error escaping encoded base64 data for URL", e, SC_INTERNAL_SERVER_ERROR);
        }
    }
}
