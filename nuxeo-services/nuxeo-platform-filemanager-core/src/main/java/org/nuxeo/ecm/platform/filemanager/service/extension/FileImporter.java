/*
 * (C) Copyright 2006 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: Plugin.java 4449 2006-10-19 11:51:56Z janguenot $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * FileManagerServiceCommon plugin default interface.
 * <p>
 * Responsible for converting given sources to a given type of Document using default.
 *
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas Kalogeropoulos</a>
 * @author Anahide Tchertchian
 */
public interface FileImporter extends Serializable, Comparable<FileImporter> {

    // XXX: OG: why make plugin serializable?

    /**
     * Gets the plugin name.
     *
     * @return a string holding the plugin name
     */
    String getName();

    /**
     * Returns the document type configured for this {@code FileImporter}, {@code null} if no document type is
     * configured.
     *
     * @since 5.5
     */
    String getDocType();

    /**
     * Sets the document type configured for this importer.
     * <p>
     *
     * @since 5.5
     */
    void setDocType(String docType);

    /**
     * Gets filters.
     * <p>
     * The filters are all the mime/type this plugin can deal with.
     *
     * @return list of string holding each filters.
     */
    List<String> getFilters();

    /**
     * Sets plugin name.
     *
     * @param name a string holding the name
     */
    void setName(String name);

    /**
     * Sets filters.
     * <p>
     * The filters are all the mime/types this plugin can deal with.
     *
     * @param filters a list of strings representing each filter
     */
    void setFilters(List<String> filters);

    /**
     * Embeds a reference to the holding FileManagerService instance to be able to reuse generic file creation utility
     * methods in specific plugin implementations.
     *
     * @param fileManagerService instance where the Plugin is registered as a contribution
     * @deprecated since 10.3, use {@link Framework#getService(Class)} instead if needed
     */
    @Deprecated
    void setFileManagerService(FileManagerService fileManagerService);

    /**
     * Tests whether plugin is suitable for the given mimetype.
     *
     * @param mimeType the mimetype to test
     */
    boolean matches(String mimeType);

    /**
     * Creates the document.
     *
     * @param documentManager the manager used to create the Document
     * @param content the content of the File
     * @param path the path of current document
     * @param overwrite a boolean deciding whether to create or update if we find a document with the same fileName
     * @param filename the filename of the File
     */
    DocumentModel create(CoreSession documentManager, Blob content, String path, boolean overwrite, String filename,
            TypeManager typeService) throws IOException;

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Returns the plugin order for sorting.
     */
    Integer getOrder();

    /**
     * Sets the plugin order for sorting.
     */
    void setOrder(Integer order);

}
