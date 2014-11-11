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
 *     Nuxeo - initial API and implementation
 *
 * $Id: CreationContainerListProviderDescriptor.java 30594 2008-02-26 17:21:10Z ogrisel $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("creationContainerListProvider")
public class CreationContainerListProviderDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected String className;

    @XNodeList(value = "docType", type = ArrayList.class, componentType = String.class)
    protected List<String> docTypes = new ArrayList<String>();

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String[] getDocTypes() {
        return docTypes.toArray(new String[docTypes.size()]);
    }

}
