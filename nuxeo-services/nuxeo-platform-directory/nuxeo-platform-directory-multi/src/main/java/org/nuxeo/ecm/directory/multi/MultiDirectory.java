/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *
 * $Id: MultiDirectory.java 25713 2007-10-05 16:06:58Z fguillaume $
 */

package org.nuxeo.ecm.directory.multi;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class MultiDirectory extends AbstractDirectory {

    // used in double-checked locking for lazy init
    protected volatile boolean initialized;

    public MultiDirectory(MultiDirectoryDescriptor descriptor) {
        super(descriptor, MultiReference.class);
    }

    @Override
    public MultiDirectoryDescriptor getDescriptor() {
        return (MultiDirectoryDescriptor) descriptor;
    }

    @Override
    public Session getSession() {
        MultiDirectorySession session = new MultiDirectorySession(this);
        initializeIfNeeded();
        addSession(session);
        return session;
    }

    protected void initializeIfNeeded() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    initSchemaFieldMap();
                    initialized = true;
                }
            }
        }
    }

    @Override
    public List<Reference> getReferences(String referenceFieldName) {
        Reference reference = new MultiReference(this, referenceFieldName);
        return Collections.singletonList(reference);
    }

    @Override
    public void invalidateDirectoryCache() {
        DirectoryService dirService = Framework.getService(DirectoryService.class);
        getCache().invalidateAll();
        // and also invalidates the cache from the source directories
        for (SourceDescriptor src : getDescriptor().sources) {
            for (SubDirectoryDescriptor sub : src.subDirectories) {
                Directory dir = dirService.getDirectory(sub.name);
                if (dir != null) {
                    dir.invalidateDirectoryCache();
                }
            }
        }
    }

}
