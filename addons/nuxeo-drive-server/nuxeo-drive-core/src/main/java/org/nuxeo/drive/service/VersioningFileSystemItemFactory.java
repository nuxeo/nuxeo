/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * A {@link FileSystemItemFactory} able to handle versioning.
 * 
 * @author Antoine Taillefer
 * @see DefaultFileSystemItemFactory
 */
public interface VersioningFileSystemItemFactory extends FileSystemItemFactory {

    /**
     * Returns true if the given {@link DocumentModel} needs to be versioned. An example of policy could be to version
     * the document if the last modification was done more than {@link #getVersioningDelay()} seconds ago.
     * 
     * @see DocumentBackedFileItem#versionIfNeeded(DocumentModel doc, CoreSession session)
     */
    boolean needsVersioning(DocumentModel doc) throws ClientException;

    /**
     * Gets the delay passed which a document needs to be versioned since its last modification.
     * 
     * @see DefaultFileSystemItemFactory#needsVersioning(DocumentModel)
     */
    double getVersioningDelay();

    /**
     * Sets the delay passed which a document needs to be versioned since its last modification.
     */
    void setVersioningDelay(double versioningDelay);

    /**
     * Gets the increment option used when versioning a document.
     * 
     * @see DocumentBackedFileItem#versionIfNeeded(DocumentModel doc, CoreSession session)
     */
    VersioningOption getVersioningOption();

    /**
     * Sets the increment option used when versioning a document.
     */
    void setVersioningOption(VersioningOption versioningOption);

}
