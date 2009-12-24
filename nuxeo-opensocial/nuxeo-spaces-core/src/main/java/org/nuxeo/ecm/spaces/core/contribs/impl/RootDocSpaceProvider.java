/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.core.contribs.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.spaces.api.AbstractSpaceProvider;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.DocSpaceImpl;

/**
 *
 * @author 10044893
 *
 */
public class RootDocSpaceProvider extends AbstractSpaceProvider {

    private final DocumentModel rootDoc;



    public RootDocSpaceProvider(DocumentModel rootDoc) {
        this.rootDoc = rootDoc;
    }


    public void add(Space o, CoreSession session) throws ClientException {
        DocSpaceImpl space = DocSpaceImpl.createFromSpace(o, rootDoc.getPathAsString(), session);
        session.saveDocument(space.getDocument());
        session.save();
    }


    public void addAll(Collection<? extends Space> c, CoreSession session) throws ClientException {
        try {
            for( Space o : c) {
                add(o,session);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void clear(CoreSession session) throws ClientException {
        session.removeChildren(rootDoc.getRef());

    }


    public Space getSpace(String spaceName, CoreSession session) throws ClientException {
        DocumentModel doc = session.getChild(rootDoc.getRef(), spaceName);
        return doc.getAdapter(Space.class);

    }


    public boolean isEmpty(CoreSession session) throws ClientException {
        return this.size(session) == 0;
    }



    public boolean remove(Space space, CoreSession session) throws ClientException {
        DocumentRef spaceRef = new PathRef(rootDoc.getPathAsString() + "/" + space.getName());
        if(session.exists(spaceRef)) {
            session.removeDocument(spaceRef);
            return true;
        } else {
            return false;
        }
    }


    public long size(CoreSession session) throws ClientException {
        return session.getChildrenIterator(rootDoc.getRef()).hashCode();
    }


    public List<Space> getAll(CoreSession session) throws ClientException {
        List<Space> spaces = new ArrayList<Space>();
        for(DocumentModel doc : session.getChildren(rootDoc.getRef())) {
            Space space = doc.getAdapter(Space.class);
            if(space != null) {
                spaces.add(space);
            }
        }
        return spaces;
    }


    public boolean isReadOnly() {
        return false;
    }
}
