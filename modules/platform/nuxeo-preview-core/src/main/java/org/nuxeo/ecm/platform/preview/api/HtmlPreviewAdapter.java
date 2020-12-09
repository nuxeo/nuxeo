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

    boolean cachable();

    String getFilePreviewURL();

    String getFilePreviewURL(String xpath);

    List<Blob> getFilePreviewBlobs() throws PreviewException;

    List<Blob> getFilePreviewBlobs(String xpath) throws PreviewException;

    List<Blob> getFilePreviewBlobs(boolean postProcess) throws PreviewException;

    List<Blob> getFilePreviewBlobs(String xpath, boolean postProcess) throws PreviewException;

    void setAdaptedDocument(DocumentModel doc);

    void cleanup();

    /**
     * Check if the document holds some blobs that are suitable for preview
     *
     * @since 5.7.3
     */
    boolean hasBlobToPreview() throws PreviewException;

    /**
     * @since 8.2
     */
    boolean hasPreview(String xpath);

}
