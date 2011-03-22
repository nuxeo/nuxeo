/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.osgi;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class OSGiHostAdapter {

    private static OSGiHostAdapter instance;

    public static void setInstance(OSGiHostAdapter instance) {
        OSGiHostAdapter.instance = instance;
    }

    public static OSGiHostAdapter getInstance() {
        return instance;
    }

    public abstract Object invoke(Object ... args) throws Exception;

    public abstract Object getProperty(String key);

    public abstract Object getProperty(String key, Object defValue);

    public abstract void setProperty(String key, Object value);

}
