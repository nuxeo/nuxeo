/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.adapter;

import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Representation of a file, ie. a downloadable {@link FileSystemItem}.
 * <p>
 * In the case of a {@link DocumentModel} backed implementation, the backing document holds a binary content. Typically
 * a File, Note or Picture.
 *
 * @author Antoine Taillefer
 * @see DocumentBackedFileItem
 */
public interface FileItem extends FileSystemItem {

    @JsonIgnore
    Blob getBlob();

    String getDownloadURL();

    String getDigestAlgorithm();

    String getDigest();

    boolean getCanUpdate();

    void setBlob(Blob blob);

}
