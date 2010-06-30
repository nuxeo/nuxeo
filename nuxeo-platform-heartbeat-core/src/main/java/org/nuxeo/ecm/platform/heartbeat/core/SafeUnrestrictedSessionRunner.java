/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.heartbeat.core;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.api.Framework;

/**
 * When running from another thread, need run unrestricted getting the class
 * loader of the running J2EE.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public abstract class SafeUnrestrictedSessionRunner extends
        UnrestrictedSessionRunner {

    public SafeUnrestrictedSessionRunner(String repositoryName) {
        super(repositoryName);
    }

    @Override
    public void runUnrestricted() throws ClientException {
        Thread currentThread = Thread.currentThread();
        ClassLoader back = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(Framework.class.getClassLoader());
        try {
            super.runUnrestricted();
        } finally {
            currentThread.setContextClassLoader(back);
        }
    }

}
