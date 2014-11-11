/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.api.ui;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * Interface for delete constraint on a directory, dependent to another
 * directory
 *
 * @author Anahide Tchertchian
 *
 */
public interface DirectoryUIDeleteConstraint extends Serializable {

    /**
     * Sets properties that may depend on the directory configuration
     *
     * @param properties
     * @throws DirectoryException
     */
    void setProperties(Map<String, String> properties)
            throws DirectoryException;

    /**
     * Returns true if given entry can be deleted from the directory where
     * constraint is declared.
     *
     * @param dirService the directory service
     * @param entryId the entry to delete id
     * @throws DirectoryException
     * @throws ClientException
     */
    boolean canDelete(DirectoryService dirService, String entryId)
            throws DirectoryException, ClientException;

}
