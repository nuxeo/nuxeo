/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: AbstractCreationContainerListProvider.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.util.Arrays;

/**
 * Helper class to contribute CreationContainerListProvider implementation to
 * the FileManagerService.
 *
 * @author Olivier Grisel (ogrisel@nuxeo.com)
 */
public abstract class AbstractCreationContainerListProvider implements
        CreationContainerListProvider {

    private String name = "";

    private String[] docTypes;

    public boolean accept(String docType) {
        if (docTypes == null || docTypes.length == 0) {
            return true;
        } else {
            return Arrays.asList(docTypes).contains(docType);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getDocTypes() {
        return docTypes;
    }

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
