/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: AbstractIndexableResource.java 30393 2008-02-21 07:03:54Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract indexable resource.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractIndexableResource implements IndexableResource {

    private static final Log log = LogFactory.getLog(AbstractIndexableResource.class);

    private static final long serialVersionUID = 7457965764411626518L;

    protected String name;

    protected IndexableResourceConf configuration;

    private LoginContext loginCtx;

    protected AbstractIndexableResource() {
    }

    protected AbstractIndexableResource(String name,
            IndexableResourceConf configuration) {
        this.name = name;
        this.configuration = configuration;
    }

    public String getName() {
        return name;
    }

    public IndexableResourceConf getConfiguration() {
        return configuration;
    }

    protected void login() {
        // We need to login here on the nuxeo ecm security domain for being able
        // to access the remote logs bean.
        try {
            loginCtx = Framework.login();
            log.debug("Authenticating against the nuxeo security domain.");
        } catch (Exception e) {
            log.warn("Cannot authenticated against the nuxeo security domain...."
                    + e.getMessage());
        }
    }

    protected void logout() {
        // We are not executing within an indexing thread here.
        if (loginCtx != null) {
            try {
                loginCtx.logout();
                log.debug("Logout from the nuxeo security domain");
            } catch (LoginException le) {
                log.warn("Failed to logout from the nuxeo security domain");
            }
        }
    }

    /**
     * @return null, which encourages the caller to continue investigating.
     */
    public ACP computeAcp() {
        return null;
    }

}
