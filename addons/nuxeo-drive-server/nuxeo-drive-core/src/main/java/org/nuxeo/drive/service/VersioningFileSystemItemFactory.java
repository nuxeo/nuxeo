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
package org.nuxeo.drive.service;

import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * A {@link FileSystemItemFactory} able to handle versioning.
 *
 * @author Antoine Taillefer
 * @see DefaultFileSystemItemFactory
 * @deprecated since 9.1 as the automatic versioning is handled by versioning system, we don't need anymore this
 *             mechanism in drive
 */
@Deprecated
public interface VersioningFileSystemItemFactory extends FileSystemItemFactory {

    /**
     * Returns true if the given {@link DocumentModel} needs to be versioned. An example of policy could be to version
     * the document if the last modification was done more than {@link #getVersioningDelay()} seconds ago.
     *
     * @see DocumentBackedFileItem#versionIfNeeded(DocumentModel doc, CoreSession session)
     * @deprecated since 9.1 versioning policy is now handled at versioning service level, as versioning is removed at
     * drive level, this method is not used anymore
     */
    @Deprecated
    boolean needsVersioning(DocumentModel doc);

    /**
     * Gets the delay passed which a document needs to be versioned since its last modification.
     *
     * @see DefaultFileSystemItemFactory#needsVersioning(DocumentModel)
     * @deprecated since 9.1 versioning policy is now handled at versioning service level, as versioning is removed at
     * drive level, this method is not used anymore
     */
    @Deprecated
    double getVersioningDelay();

    /**
     * Sets the delay passed which a document needs to be versioned since its last modification.
     *
     * @deprecated since 9.1 versioning policy is now handled at versioning service level, as versioning is removed at
     * drive level, this method is not used anymore
     */
    @Deprecated
    void setVersioningDelay(double versioningDelay);

    /**
     * Gets the increment option used when versioning a document.
     *
     * @see DocumentBackedFileItem#versionIfNeeded(DocumentModel doc, CoreSession session)
     * @deprecated since 9.1 versioning policy is now handled at versioning service level, as versioning is removed at
     * drive level, this method is not used anymore
     */
    @Deprecated
    VersioningOption getVersioningOption();

    /**
     * Sets the increment option used when versioning a document.
     *
     * @deprecated since 9.1 versioning policy is now handled at versioning service level, as versioning is removed at
     * drive level, this method is not used anymore
     */
    @Deprecated
    void setVersioningOption(VersioningOption versioningOption);

}
