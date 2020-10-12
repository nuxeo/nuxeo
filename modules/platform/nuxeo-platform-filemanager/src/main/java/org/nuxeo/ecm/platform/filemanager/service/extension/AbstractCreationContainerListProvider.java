/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: AbstractCreationContainerListProvider.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.util.Arrays;

/**
 * Helper class to contribute CreationContainerListProvider implementation to the FileManagerService.
 *
 * @author Olivier Grisel (ogrisel@nuxeo.com)
 */
public abstract class AbstractCreationContainerListProvider implements CreationContainerListProvider {

    private String name = "";

    private String[] docTypes;

    @Override
    public boolean accept(String docType) {
        if (docTypes == null || docTypes.length == 0) {
            return true;
        } else {
            return Arrays.asList(docTypes).contains(docType);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String[] getDocTypes() {
        return docTypes;
    }

    @Override
    public void setDocTypes(String[] docTypes) {
        this.docTypes = docTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CreationContainerListProvider) {
            // casting on the interface gives no guarantee that the equals
            // relationship will be symmetric but this is documented in the interface
            // javadoc of the getName method
            CreationContainerListProvider provider = (CreationContainerListProvider) o;
            return name != null && name.equals(provider.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

}
