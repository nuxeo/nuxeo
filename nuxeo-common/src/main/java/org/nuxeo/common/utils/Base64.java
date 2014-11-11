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


/**
 * Simple wrapper around codec library to preserve backward compatibility
 * @author bstefanescu
 *
 */
public class Base64 {

    public final static String encodeBytes(byte[] source) {
        return org.apache.commons.codec.binary.Base64.encodeBase64String(source).trim();
    }

    public final static byte[] decode(String s) {
        return org.apache.commons.codec.binary.Base64.decodeBase64(s);
    }

}
