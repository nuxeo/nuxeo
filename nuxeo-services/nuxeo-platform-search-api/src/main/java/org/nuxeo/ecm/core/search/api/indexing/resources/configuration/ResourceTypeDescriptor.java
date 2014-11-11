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
 *     anguenot
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.indexing.resources.configuration;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@XObject("resourceType")
public class ResourceTypeDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ResourceTypeDescriptor.class);

    @XNode("@name")
    protected String name;

    @XNode("@factoryClass")
    protected Class factoryClass;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(Class factoryClass) {
        this.factoryClass = factoryClass;
    }

    public IndexableResourceFactory getFactory() {
        IndexableResourceFactory resource = null;
        try {
            resource = (IndexableResourceFactory) factoryClass.newInstance();
        } catch (InstantiationException e) {
            log.error("Cannot instanciate indexable resource=" + name);
        } catch (IllegalAccessException e) {
            log.error("Cannot instanciate indexable resource=" + name);
        }
        return resource;
    }

}
