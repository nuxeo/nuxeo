/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of common Reference logic.
 *
 * @author ogrisel
 */
public abstract class AbstractReference implements Reference {

    protected DirectoryServiceImpl directoryService;

    protected String sourceDirectoryName;

    protected Directory sourceDirectory;

    protected String targetDirectoryName;

    protected Directory targetDirectory;

    protected String fieldName;

    public String getFieldName() {
        return fieldName;
    }

    public Directory getSourceDirectory() throws DirectoryException {
        if (sourceDirectory == null) {
            sourceDirectory = getDirectoryService().getDirectory(
                    sourceDirectoryName);
        }
        return sourceDirectory;
    }

    public void setSourceDirectoryName(String sourceDirectoryName) {
        sourceDirectory = null;
        this.sourceDirectoryName = sourceDirectoryName;
    }

    public Directory getTargetDirectory() throws DirectoryException {
        if (targetDirectory == null) {
            targetDirectory = getDirectoryService().getDirectory(
                    targetDirectoryName);
        }
        return targetDirectory;
    }

    public void setTargetDirectoryName(String targetDirectoryName) {
        targetDirectory = null;
        this.targetDirectoryName = targetDirectoryName;
    }

    protected DirectoryServiceImpl getDirectoryService() {
        if (directoryService == null) {
            directoryService = (DirectoryServiceImpl) Framework.getRuntime()
                    .getComponent(DirectoryService.NAME);
        }
        return directoryService;
    }

}
