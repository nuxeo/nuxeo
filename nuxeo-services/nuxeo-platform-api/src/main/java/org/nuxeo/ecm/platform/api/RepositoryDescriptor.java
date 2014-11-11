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
 * $Id$
 */

package org.nuxeo.ecm.platform.api;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.ServiceManagement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated use new service API {@link ServiceManagement}
 */
@SuppressWarnings({"ALL"})
@Deprecated
@XObject("repository")
public class RepositoryDescriptor implements Serializable {

    private static final long serialVersionUID = -7968134063116221438L;

    @XNode("@name")
    public String name;

    @XNode("@description")
    public String description;

    @XNode("@class")
    public Class connectorClass;

    public Server server;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

}
