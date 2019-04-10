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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.fileimporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

public class UploadItemMock extends UploadItem {

    private static final long serialVersionUID = 1L;

    protected final Blob blob;

    public UploadItemMock(String fileName, String contentType, Object object) {
        super(fileName, 1, contentType, object);

        File file = (File) object;

        blob = new FileBlob(file);
        blob.setFilename(file.getName());
    }

    public static UploadEvent getUploadEvent(File file) {
        UploadItem item = new UploadItem(file.getName(), 1, null, file);
        UIComponent component = new UIData();
        List<UploadItem> items = new ArrayList<UploadItem>();
        items.add(item);
        return new UploadEvent(component, items);
    }

}
