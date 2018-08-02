/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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

    protected String sourceDirectoryName;

    protected Directory sourceDirectory;

    protected String targetDirectoryName;

    protected Directory targetDirectory;

    protected String fieldName;

    /**
     * @since 9.2
     */
    public AbstractReference(String fieldName, String targetDirectoryName) {
        this.fieldName = fieldName;
        this.targetDirectoryName = targetDirectoryName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Directory getSourceDirectory() {
        if (sourceDirectory == null) {
            sourceDirectory = Framework.getService(DirectoryService.class).getDirectory(sourceDirectoryName);
        }
        return sourceDirectory;
    }

    @Override
    public void setSourceDirectoryName(String sourceDirectoryName) {
        sourceDirectory = null;
        this.sourceDirectoryName = sourceDirectoryName;
    }

    @Override
    public Directory getTargetDirectory() {
        if (targetDirectory == null) {
            targetDirectory = Framework.getService(DirectoryService.class).getDirectory(targetDirectoryName);
        }
        return targetDirectory;
    }

    @Override
    public void setTargetDirectoryName(String targetDirectoryName) {
        targetDirectory = null;
        this.targetDirectoryName = targetDirectoryName;
    }
}
