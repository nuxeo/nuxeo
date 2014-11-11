/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.Serializable;

import org.nuxeo.runtime.api.Framework;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bogdan Stefanescu
 *
 */
public abstract class PropertyDecoder {

    private static final Log log = LogFactory.getLog(PropertyDecoder.class);

    private static final Map<String, PropertyDecoder> decoders = new HashMap<String, PropertyDecoder>();


    public static Serializable decode(String type, String value) {
        // expand value if needed
        if (value != null) {
            value = Framework.getRuntime().expandVars(value);
        }
        PropertyDecoder decoder = decoders.get(type);
        try {
            return decoder == null ? value : decoder.decode(value);
        } catch (Throwable t) {
            log.error(t);
            return null;
        }
    }

    public static PropertyDecoder getDecoder(String type) {
        return decoders.get(type);
    }

    public static void registerDecoder(String type, PropertyDecoder decoder) {
        decoders.put(type, decoder);
    }

    public abstract Serializable decode(String value);


    public static final PropertyDecoder STRING = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return value;
        }
    };

    public static final PropertyDecoder LIST = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            ArrayList<String> values = new ArrayList<String>();
            StringTokenizer tokenizer = new StringTokenizer(value, ",");
            while (tokenizer.hasMoreTokens()) {
                String tok = tokenizer.nextToken();
                tok = tok.trim();
                values.add(tok);
            }
            return values;
        }
    };

    public static final PropertyDecoder LONG = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return Long.valueOf(value);
        }
    };

    public static final PropertyDecoder INTEGER = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return Integer.valueOf(value);
        }
    };

    public static final PropertyDecoder DOUBLE = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return Double.valueOf(value);
        }
    };

    public static final PropertyDecoder FLOAT = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return Float.valueOf(value);
        }
    };

    public static final PropertyDecoder BOOLEAN = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return Boolean.valueOf(value);
        }
    };

    public static final PropertyDecoder BYTE = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return Byte.valueOf(value);
        }
    };

    public static final PropertyDecoder CHAR = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            if (value.length() == 0) {
                return 0;
            }
            return value.charAt(0);
        }
    };

    public static final PropertyDecoder SHORT = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return Short.valueOf(value);
        }
    };

    public static final PropertyDecoder OBJECT = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return null; // TODO not yet impl
        }
    };


    public static final PropertyDecoder CLASS = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return null; // TODO not yet impl
        }
    };

    public static final PropertyDecoder INSTANCE = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return null; // TODO not yet impl
        }
    };

    public static final PropertyDecoder COMPONENT = new PropertyDecoder() {
        @Override
        public Serializable decode(String value) {
            return null; // TODO not yet impl
        }
    };

    static {
        registerDecoder("String", STRING);
        registerDecoder("List", LIST);
        registerDecoder("Long", LONG);
        registerDecoder("Integer", INTEGER);
        registerDecoder("Double", DOUBLE);
        registerDecoder("Float", FLOAT);
        registerDecoder("Boolean", BOOLEAN);
        registerDecoder("Class", CLASS);
        registerDecoder("Instance", INSTANCE);
        registerDecoder("Object", OBJECT);
        registerDecoder("Component", COMPONENT);
        registerDecoder("Byte", BYTE);
        registerDecoder("Char", CHAR);
        registerDecoder("Short", SHORT);
    }

}
