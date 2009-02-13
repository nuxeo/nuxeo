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

package org.nuxeo.ecm.core.model;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Repository {

    String getName();

    Session getSession(Map<String, Serializable> context) throws DocumentException;

    SchemaManager getTypeManager();

    SecurityManager getNuxeoSecurityManager();

    void initialize() throws DocumentException;

    Session getSession(long sessionId) throws DocumentException;

    Session[] getOpenedSessions() throws DocumentException;

    /**
     * TODO: hide shutdown from public API.
     *
     */
    void shutdown();

    // stats for debug

    int getStartedSessionsCount();

    int getClosedSessionsCount();

    int getActiveSessionsCount();

}
