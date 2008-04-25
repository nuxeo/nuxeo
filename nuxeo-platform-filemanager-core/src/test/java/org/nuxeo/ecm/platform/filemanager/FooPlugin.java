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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.filemanager.service.extension.Plugin;
import org.nuxeo.ecm.platform.types.TypeManager;

public class FooPlugin implements Plugin {

    private static final long serialVersionUID = 1L;

    protected String name = "";

    protected List<String> filters = new ArrayList<String>();

    public boolean isEnabled()
    {
        return true;
    }

    public void setEnabled(boolean enabled)
    {
        //
    }


    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String filename,
            TypeManager typService) {
        return null;
    }

    public boolean matches(String mimeType) {
        return false;
    }

    public void setFileManagerService(FileManagerService fileManagerService) {
        // ignore
    }

}
