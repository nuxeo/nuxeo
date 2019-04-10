/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.segment.io.web;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.segment.io.SegmentIO;

public class MarketoHelper {

    public static final String SECRET_KEY_NAME = "MARKETO_SECRET";

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    protected static final Log log = LogFactory.getLog(MarketoHelper.class);

    protected static String toHexString(byte[] data) {
        StringBuilder buf = new StringBuilder(2 * data.length);
        for (byte b : data) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
    }

    protected static String getSecret() {
        SegmentIO service = Framework.getService(SegmentIO.class);
        return service.getGlobalParameters().get(SECRET_KEY_NAME);
    }

    public static String getLeadHash(String leadEmail)  {

        try {
           String digestInput = getSecret() + leadEmail;
           MessageDigest md = MessageDigest.getInstance("SHA-1");
           byte[] digest = md.digest(digestInput.getBytes());
           return toHexString(digest);
        }
        catch (NoSuchAlgorithmException t) {
            log.error("Error while computing Marketo digest", t);
            return null;
        }
    }
}
