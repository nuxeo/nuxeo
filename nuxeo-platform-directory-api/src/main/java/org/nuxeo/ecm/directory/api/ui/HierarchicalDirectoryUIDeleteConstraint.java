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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * Delete constraint preventing from removing an entry from a parent directory
 * if it is referenced in a child directory.
 *
 * <p>
 * Needs to know the child directory name and the field where parent entry id is
 * declared on it.
 *
 * @author Anahide Tchertchian
 */
public class HierarchicalDirectoryUIDeleteConstraint extends
        AbstractDirectoryUIDeleteConstraint {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(HierarchicalDirectoryUIDeleteConstraint.class);

    protected String targetDirectory;

    protected String targetDirectoryField;

    @Override
    public void setProperties(Map<String, String> properties)
            throws DirectoryException {
        String targetDirKey = "targetDirectory";
        String targetDirFieldKey = "targetDirectoryField";
        if (properties == null) {
            throw new DirectoryException(String.format(
                    "This delete constraint requires properties '%s' and '%s'",
                    targetDirKey, targetDirFieldKey));
        }
        if (!properties.containsKey(targetDirKey)) {
            throw new DirectoryException(String.format(
                    "This delete constraint requires property '%s'",
                    targetDirKey));
        }
        if (!properties.containsKey(targetDirFieldKey)) {
            throw new DirectoryException(String.format(
                    "This delete constraint requires property '%s'",
                    targetDirFieldKey));
        }
        targetDirectory = properties.get(targetDirKey);
        targetDirectoryField = properties.get(targetDirFieldKey);
    }

    public boolean canDelete(DirectoryService dirService, String entryId)
            throws DirectoryException, ClientException {
        Session dirSession = null;
        try {
            dirSession = dirService.open(targetDirectory);
            // search for given entry id usage in this directory
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put(targetDirectoryField, entryId);
            DocumentModelList res = dirSession.query(filter);
            if (res.isEmpty()) {
                return true;
            }
            if (log.isDebugEnabled()) {
                log.debug("Can not delete " + targetDirectory + " " + entryId
                        + ", constraint on " + targetDirectoryField + ":"
                        + res.get(0).getId());
            }
            return false;
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
    }

}
