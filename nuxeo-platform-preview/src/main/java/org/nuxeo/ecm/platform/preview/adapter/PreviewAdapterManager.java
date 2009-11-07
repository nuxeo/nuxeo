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
 */
package org.nuxeo.ecm.platform.preview.adapter;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;

/**
 * Interface of the service that is responsible for managing PreviewAdapters.
 *
 * @author tiry
 */
public interface PreviewAdapterManager {

    HtmlPreviewAdapter getAdapter(DocumentModel doc);

    boolean hasAdapter(DocumentModel doc);

    MimeTypePreviewer getPreviewer(String mimeType);

    List<BlobPostProcessor> getBlobPostProcessors();

}
