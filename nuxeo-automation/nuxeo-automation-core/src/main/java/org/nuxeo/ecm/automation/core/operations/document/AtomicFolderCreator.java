/*******************************************************************************
 *  (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *  
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Lesser General Public License
 *  (LGPL) version 2.1 which accompanies this distribution, and is available at
 *  http://www.gnu.org/licenses/lgpl.html
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *******************************************************************************/
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.critical.CriticalSectionRunner;

/**
 * Atomic folder creation, allow to create a folder hierarchy in a multi-threaded 
 * context.
 * 
 * @since 5.7
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 *
 */
public class AtomicFolderCreator extends CriticalSectionRunner {

    protected final Path path;
    
    protected DocumentModel folder;

    public AtomicFolderCreator(CoreSession session, String path) throws ClientException {
        super(session);
        this.path = new Path(path);
    }

    public DocumentModel getFolder() {
        return folder;
    }
    
    @Override
    public void run() throws ClientException {
        PathRef ref = new PathRef(path.toString());
        if (session.exists(ref)) {
            folder = session.getDocument(ref);
            return;
        }
        super.run();
    }
    
    @Override
    protected void enter(DocumentRef ref) throws ClientException {
        DocumentModel doc = session.getDocument(ref);
        Path parent = new Path(doc.getPathAsString());
        Path relative = path.removeFirstSegments(parent.segmentCount());
        mkdirs(parent, relative);
    }
    
    protected void mkdirs(Path parent, Path relative)
            throws ClientException {
        DocumentRef ref;
        for (String segment : relative.segments()) {
            Path child = parent.append(segment);
            ref = new PathRef(child.toString());
            if (!session.exists(ref)) {
                mkdir(parent, segment);
            } else {
                folder = session.getDocument(ref);
            }
            parent = child;
        }
    }
    
    protected void mkdir(Path path, String name)
            throws ClientException {
        folder = session.createDocumentModel("Folder");
        folder.setPathInfo(path.toString(), name);
        folder = session.createDocument(folder);
        folder = session.saveDocument(folder);
    }

    @Override
    protected DocumentRef lockTarget() throws ClientException {
        return tryResolve(path);
    }
    
    protected DocumentRef tryResolve(Path path) throws ClientException {
        if (path.segmentCount() == 0) {
            return session.getRootDocument().getRef();
        }
        PathRef ref = new PathRef(path.toString());
        if (session.exists(ref)) {
            return ref;
        }
        return tryResolve(path.removeLastSegments(1));
    }

}
