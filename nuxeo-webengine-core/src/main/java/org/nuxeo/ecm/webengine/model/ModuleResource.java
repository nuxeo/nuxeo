/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model;

import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ModuleResource extends Resource {

    String getName();

    Object getErrorView(WebApplicationException e);

    ScriptFile getFile(String path) throws WebException;

    Class<?> loadClass(String className) throws ClassNotFoundException;

    ResourceType getType(String typeName) throws TypeNotFoundException;

    ResourceType[] getTypes();

    ServiceType[] getServices();

    ServiceType getService(Resource ctx, String name) throws ServiceNotFoundException;

    List<ServiceType> getServices(Resource ctx);

    List<String> getServiceNames(Resource ctx);

    List<ServiceType> getEnabledServices(Resource ctx);

    List<String> getEnabledServiceNames(Resource ctx);

}
