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

    Session[] getOpenedSessions() throws DocumentException;

    void shutdown();

    // stats for debug

    int getStartedSessionsCount();

    int getClosedSessionsCount();

    int getActiveSessionsCount();

}
