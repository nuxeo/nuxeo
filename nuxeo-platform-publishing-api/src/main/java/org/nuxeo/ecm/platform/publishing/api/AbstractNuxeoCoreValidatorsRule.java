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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: AbstractNuxeoCoreValidatorsRule.java 30393 2008-02-21 07:03:54Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing.api;

import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract Nuxeo Core Validators Rules.
 * <p>
 * Offers Nuxeo Core base API for validators that need to connect on Nuxeo Core
 * to compute validators.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractNuxeoCoreValidatorsRule implements ValidatorsRule {

    private static final long serialVersionUID = -1578337333898362863L;

    protected CoreSession session;

    protected LoginContext loginCtx;

    protected void login() throws Exception {
        loginCtx = Framework.login();
    }

    protected void logout() throws Exception {
        if (loginCtx != null) {
            loginCtx.logout();
        }
    }

    protected void initializeCoreSession(String repoName) throws Exception {
        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        session = mgr.getRepository(repoName).open();
    }

    protected void closeCoreSession() throws Exception {
        if (session != null) {
            CoreInstance.getInstance().close(session);
        }
    }

}
