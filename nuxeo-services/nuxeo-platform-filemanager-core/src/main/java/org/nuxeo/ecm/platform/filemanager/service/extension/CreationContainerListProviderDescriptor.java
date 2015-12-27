/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
