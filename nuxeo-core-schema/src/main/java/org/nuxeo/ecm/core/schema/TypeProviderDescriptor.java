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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.ServiceLocatorFactory;

/**
 * A object describing a type provider.
 * <p>
 * A type provider is useful to import types from remote servers
 * and it is described by an uri or a service group
 * <p>
 * I an uri is given it will be used to lookup the type provider service
 * using a service locatror as returned by {@link ServiceLocatorFactory}
 * <p>
 * If a service group is given the service locator bound to the group will be used to locate the service
 * <p>
 * If both the uri and group are defined, the uri will take precedence.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("provider")
public class TypeProviderDescriptor {

    @XNode("@uri")
    public String uri;

    @XNode("@group")
    public String group;

}
