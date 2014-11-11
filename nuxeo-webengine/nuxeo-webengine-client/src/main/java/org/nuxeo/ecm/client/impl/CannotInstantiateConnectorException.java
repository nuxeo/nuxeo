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
package org.nuxeo.ecm.client.impl;

import org.nuxeo.ecm.client.Connector;
import org.nuxeo.ecm.client.ContentManagerException;

/**
 * @author matic
 *
 */
public class CannotInstantiateConnectorException extends
        ContentManagerException {

    protected Class<? extends Connector> connectorClass;

    public CannotInstantiateConnectorException(Class<? extends Connector> connectorClass, Exception e) {
        super("Cannot instantiate connector for " + connectorClass);
        this.connectorClass = connectorClass;
    }

    private static final long serialVersionUID = 5060709344706674038L;

}
