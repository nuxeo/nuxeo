/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.api;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public interface DirectoryService {

    String SYSTEM_DIRECTORY_TYPE = "system";

    /**
     * Gets the directory ids.
     */
    List<String> getDirectoryNames();

    /**
     * Gets all the directories.
     */
    List<Directory> getDirectories();

    /**
     * Returns the directory for the specified id and document context.
     * <p>
     * The context is given by the document. The document service will try to find the directory local configuration of
     * the document that will specify the suffix. The directory service will fetch the id + suffix found. If no local
     * configuration is found the service will return the directory with the given id.
     * <p>
     * If the id is {@code null}, returns {@code null}.
     *
     * @param id the directory id
     * @param documentContext the document context
     * @return the directory, or {@code null} if not found
     */
    Directory getDirectory(String id, DocumentModel documentContext);

    /**
     * Return the directory with the given id.
     * <p>
     * If the id is {@code null}, returns {@code null}.
     *
     * @param id the directory id
     * @return the directory, or {@code null} if not found
     */
    Directory getDirectory(String id);

    /**
     * Gets the effective directory descriptor for the given directory.
     *
     * @param id the directory id
     * @return the effective directory descriptor, or {@code null} if not registered
     * @since 8.2
     */
    BaseDirectoryDescriptor getDirectoryDescriptor(String id);

    /**
     * Opens a session on specified directory.
     * <p>
     * This method prefers to throw rather than returning null.
     *
     * @return the session
     */
    Session open(String directoryName);

    /**
     * Opens a session on the directory for the specified context. The context is given by the document. The document
     * service will try to find the directory local configuration of the document that will specify the suffix. the
     * directory will fetch the directoryName + suffix found. If no local configuration is found the service will return
     * the directoryName directory.
     * <p>
     * This method prefers to throw rather than returning null.
     *
     * @return the session
     */
    Session open(String directoryName, DocumentModel documentContext);

    /**
     * Gets the schema for a directory.
     *
     * @param id the directory id
     * @return the schema for the directory
     */
    String getDirectorySchema(String id);

    /**
     * Gets the id field for a directory.
     *
     * @param id the directory id
     * @return the id field for the directory
     */
    String getDirectoryIdField(String id);

    /**
     * Gets the password field for a directory.
     *
     * @param directoryName the directory name
     * @return the password field for the directory
     */
    String getDirectoryPasswordField(String directoryName);

    /**
     * Gets the parent directory id a directory.
     *
     * @param id the directory id
     * @return the parent directory id, which may be {@code null}
     */
    String getParentDirectoryName(String id);

    /**
     * INTERNAL registers a directory descriptor.
     */
    void registerDirectoryDescriptor(BaseDirectoryDescriptor descriptor);

    /**
     * INTERNAL unregisters a directory descriptor.
     */
    void unregisterDirectoryDescriptor(BaseDirectoryDescriptor descriptor);

}
