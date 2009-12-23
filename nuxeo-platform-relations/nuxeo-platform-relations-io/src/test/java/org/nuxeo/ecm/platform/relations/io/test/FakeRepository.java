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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FakeRepository.java 25081 2007-09-18 14:57:22Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.io.test;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class FakeRepository implements Repository {

    public String getName() {
        return "demo";
    }

    public SecurityManager getNuxeoSecurityManager() {
        return new FakeSecurityManager();
    }

    public Session getSession(long sessionId) throws DocumentException {
        return FakeSession.getSession();
    }

    public Session getSession(Map<String, Serializable> context)
            throws DocumentException {
        return FakeSession.getSession();
    }

    public int getActiveSessionsCount() {
        return 0;
    }

    public int getClosedSessionsCount() {
        return 0;
    }

    public Session[] getOpenedSessions() throws DocumentException {
        return null;
    }

    public int getStartedSessionsCount() {
        return 0;
    }

    public SchemaManager getTypeManager() {
        return null;
    }

    public void initialize() throws DocumentException {
    }

    public void shutdown() {
    }

    public boolean supportsTags() {
        return false;
    }

}
