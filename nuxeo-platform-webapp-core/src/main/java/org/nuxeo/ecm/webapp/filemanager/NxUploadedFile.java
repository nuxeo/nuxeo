/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */

/**
 * @since 6.0
 */
package org.nuxeo.ecm.webapp.filemanager;

import java.io.File;
import java.io.Serializable;

public class NxUploadedFile implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String contentType;

    protected String name;

    protected File file;

    public NxUploadedFile(String name, String contentType, File file) {
        super();
        this.contentType = contentType;
        this.name = name;
        this.file = file;
    }

    public String getContentType() {
        return contentType;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setName(String name) {
        this.name = name;
    }

}
