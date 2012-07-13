/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.common.utils;

import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple wrapper around codec library to preserve backward compatibility
 *
 * @author bstefanescu
 * @deprecated Since 5.6. Use {@link org.apache.commons.codec.binary.Base64}
 *             instead.
 *
 */
@Deprecated
public class Base64 {

    private static final Log log = LogFactory.getLog(Base64.class);

    public final static String encodeBytes(byte[] source) {
        try {
            return new String(
                    org.apache.commons.codec.binary.Base64.encodeBase64(source),
                    "US-ASCII").trim();
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            return "";
        }
    }

    public final static byte[] decode(String s) {
        return org.apache.commons.codec.binary.Base64.decodeBase64(s);
    }

}
