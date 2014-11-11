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
 * $Id$
 */

package org.nuxeo.ecm.core.repository;

/**
 * Managed bean to be used in JBoss to introspect repository state and configuration.
 *
 * @author bstefanescu
 */
public interface JBossRepositoryMBean {

    String listOpenedSessions() throws Exception;

    String listRegisteredTypes() throws Exception;

    String listRegisteredDocumentTypes() throws Exception;

    String listRegisteredSchemas() throws Exception;

    String listDocumentLocks() throws Exception;

    void restart(boolean reloadTypes) throws Exception;

}
