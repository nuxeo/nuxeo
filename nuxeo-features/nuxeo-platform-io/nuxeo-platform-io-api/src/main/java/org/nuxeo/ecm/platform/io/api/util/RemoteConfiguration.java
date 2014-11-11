/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.io.api.util;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.io.api.IOManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RemoteConfiguration extends AbstractIOConfiguration {

    private static final Log log = LogFactory.getLog(RemoteConfiguration.class);

    private static final long serialVersionUID = 1L;

    protected transient IOManager manager;
    protected final Properties jndiEnv;
    protected String jndiName;

    public RemoteConfiguration(String jndiName, Properties jndiEnv) {
        this.jndiEnv = jndiEnv;
    }

    public IOManager getManager() {
        if (manager == null) {
            try {
                manager = (IOManager) new InitialContext(jndiEnv).lookup(jndiName);
            } catch (NamingException e) {
                log.error(e, e); // TODO throw exception
                return null;
            }
        }
        return manager;
    }

    public boolean isLocal() {
        return false;
    }

}
