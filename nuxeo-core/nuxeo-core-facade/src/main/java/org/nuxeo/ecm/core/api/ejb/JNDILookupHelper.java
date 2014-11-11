/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.ejb;

import java.io.Serializable;
import java.net.URL;
import java.util.Properties;

import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Provides helper methods to load a JNDI configuration and lookup some ejb
 * references in JNDI.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 * @deprecated use ECM.getPlatform().getService(...) instead
 */
@Deprecated
public class JNDILookupHelper implements Serializable {

    private static final long serialVersionUID = -3907087396628330293L;

    private static final Log log = LogFactory.getLog(JNDILookupHelper.class);

    protected final Properties serverContainerLocation;

    protected final Properties ejbLocations;

    protected InitialContext initialContext;

    public JNDILookupHelper(String path) throws ClientException {
        serverContainerLocation = new Properties();
        ejbLocations = new Properties();

        try {
            if (null == path) {
                path = "META-INF/";
            }

            String jndiResPath = path + "jndi.properties";

            URL jndiResourceURL = Thread.currentThread()
                    .getContextClassLoader().getResource(jndiResPath);
            if (jndiResourceURL == null) {
                String errMsg = "Cannot load jndi.properties from classpath location: "
                        + jndiResPath;
                throw new ClientException(errMsg);
            }

            log.info("Loading jndi.properties file from: "
                    + jndiResourceURL.getPath());
            serverContainerLocation.load(jndiResourceURL.openStream());

            URL jndiLocationsURL = Thread.currentThread()
                    .getContextClassLoader().getResource(
                            path + "JNDILocations.properties");
            if (jndiLocationsURL == null) {
                String errMsg = "Cannot load JNDILocations.properties from classpath location: "
                        + path;
                throw new ClientException(errMsg);
            }

            log.info("Loading JNDILocations.properties file from: "
                    + jndiLocationsURL.getPath());
            ejbLocations.load(jndiLocationsURL.openStream());

            initialContext = loadInitialContext();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    protected InitialContext loadInitialContext() throws ClientException {
        InitialContext ctx;
        try {
            ctx = new InitialContext(serverContainerLocation);
        } catch (Throwable t) {
            log.error("Cannot create InitialContext for: "
                    + serverContainerLocation);
            throw ClientException.wrap(t);
        }

        return ctx;
    }

    /**
     * Returns the ejb remote handler for the specified identifier.
     *
     * @param identifier
     *            the identifier that identifies the JNDI location entry in the
     *            JNDILocations.properties file.
     * @return
     * @throws ClientException
     */
    public Object lookupEjbReference(String identifier) throws ClientException {
        try {
            final String location = ejbLocations.getProperty(identifier);
            if (location == null) {
                throw new ClientException(
                        "cannot retrieve jndi location for identifier: "
                                + identifier);
            }

            return initialContext.lookup(location);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }
}
