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

package org.nuxeo.runtime.api;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JBossServiceLocator extends JndiServiceLocator {

    private static final long serialVersionUID = -5691359964790311122L;

    private String prefix = "";

    private String suffix = "";

    @Override
    public void initialize(String host, int port, Properties properties)
            throws Exception {
        if (port == 0) {
            port = 1099;
        }
        if (properties != null) {
            prefix = properties.getProperty("prefix", "nuxeo/");
            suffix = properties.getProperty("suffix", getDefaultSuffix());
            // these properties are set only by the client autonficonguration system if needed
            String value = properties.getProperty(Context.PROVIDER_URL);
            if (value != null) {
                value = String.format(value, host, port);
                properties.put(Context.PROVIDER_URL, value);
            }
        }
        context = new InitialContext(properties);
    }

    @Override
    public Object lookup(ServiceDescriptor sd) throws Exception {
        String locator = sd.getLocator();
        if (locator == null) {
            locator = prefix + sd.getServiceClassSimpleName() + suffix;
            sd.setLocator(locator);
        } else if (locator.startsWith("%")) {
            locator = prefix + locator.substring(1) + suffix;
            sd.setLocator(locator);
        }
        return lookup(locator);
    }

    public static String getDefaultSuffix() {
        if (Framework.getProperty("nuxeo.client.on.jboss") != null) {
            return "/remote";
        }
        return System.getProperty("jboss.home.dir") == null ? "/remote" : "/local"; // if not in jboss return "/remote"
    }

}
