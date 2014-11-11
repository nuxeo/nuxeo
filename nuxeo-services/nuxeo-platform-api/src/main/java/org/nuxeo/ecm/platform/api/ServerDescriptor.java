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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.ServiceManagement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated use new service API {@link ServiceManagement}
 */
@SuppressWarnings({"ALL"})
@Deprecated
@XObject("server")
public class ServerDescriptor implements Serializable {

    private static final long serialVersionUID = 4312978713602457662L;

    @XNode("@name")
    public String name;

    @XNode("@host")
    public String host;

    @XNode("@port")
    public String port;

    @XNode("@jndiContextFactory")
    public Class jndiContextFactory;

    @XNode("@serviceConnector")
    public Class serviceConnector;

    @XNode("@repositoryConnector")
    public Class repositoryConnector;

    @XNode("@jndiPrefix")
    public String jndiPrefix = "nuxeo/";

    @XNode("@jndiSuffix")
    public String jndiSuffix = "/remote";

    @XNodeMap(value = "service", key = "@class", type = HashMap.class, componentType = ServiceDescriptor.class)
    public Map<String, ServiceDescriptor> services;

    @XNodeMap(value = "repository", key = "@name", type = HashMap.class, componentType = RepositoryDescriptor.class)
    public Map<String, RepositoryDescriptor> repositories;

}
