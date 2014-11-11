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
 *     matic
 */
package org.nuxeo.ecm.client;

import java.net.URL;

import org.nuxeo.ecm.client.impl.CannotInstantiateConnectorException;

/**
 * mapped to APP document service used for introspection
 * 
 * @author matic
 * 
 * @apiviz.owns org.nuxeo.ecm.client.Connector
 * @apiviz.owns org.nuxeo.ecm.client.ContentHandlerRegistry
 * 
 */
public interface ContentManager extends RepositoryService, Console {

    void init(URL baseURL, Class<? extends Connector> connectorClass) throws CannotInstantiateConnectorException;

    URL getBaseURL();

    Connector getConnector();

    ContentHandlerRegistry getContentHandlerRegistry();

}
