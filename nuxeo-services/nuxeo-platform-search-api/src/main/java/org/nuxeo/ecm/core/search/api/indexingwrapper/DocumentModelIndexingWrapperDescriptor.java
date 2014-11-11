/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.api.indexingwrapper;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "indexingWrapper")
public class DocumentModelIndexingWrapperDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    private boolean enabled = true;

    @XNode("typeName")
    private String typeName;

    @XNode("class")
    private Class adapterClass;

    public Class getAdapterClass() {
        return adapterClass;
    }

    public void setAdapterClass(Class adapterClass) {
        this.adapterClass = adapterClass;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public DocumentIndexingWrapperFactory getNewInstance() throws InstantiationException,
            IllegalAccessException {
        return (DocumentIndexingWrapperFactory) adapterClass.newInstance();
    }

}
