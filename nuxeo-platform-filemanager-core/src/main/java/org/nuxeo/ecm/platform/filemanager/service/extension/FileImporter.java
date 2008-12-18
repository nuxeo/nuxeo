/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.types.TypeManager;

/**
 * FileManagerServiceCommon plugin default interface.
 * <p>
 * Responsible for converting given sources to a given type of Document using
 * default.
 *
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas
 *         Kalogeropoulos</a>
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
     * Embeds a reference to the holding FileManagerService instance to be able
     * to reuse generic file creation utility methods in specific plugin
     * implementations.
     *
     * @param fileManagerService instance where the Plugin is registered as a
     *            contribution
     */
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
     * @param overwrite a boolean deciding whether to create or update if we
     *            find a document with the same fileName
     * @param filename the filename of the File
     */
    DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String filename,
            TypeManager typeService) throws ClientException, IOException;

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
