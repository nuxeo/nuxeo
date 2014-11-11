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
package org.nuxeo.ecm.platform.preview.api;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for the Preview DocumentModel adapter.
 *
 * @author tiry
 */
public interface HtmlPreviewAdapter {

    boolean  cachable();

    String getFilePreviewURL();

    String getFilePreviewURL(String xpath);

    List<Blob> getFilePreviewBlobs() throws PreviewException;

    List<Blob> getFilePreviewBlobs(String xpath) throws PreviewException;

    List<Blob> getFilePreviewBlobs(boolean postProcess) throws PreviewException;

    List<Blob> getFilePreviewBlobs(String xpath, boolean postProcess) throws PreviewException;

    void setAdaptedDocument(DocumentModel doc);

    void cleanup();

}
