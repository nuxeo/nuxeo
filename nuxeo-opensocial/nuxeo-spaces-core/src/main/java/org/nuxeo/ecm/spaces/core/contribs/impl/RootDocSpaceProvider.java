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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.spaces.api.AbstractSpaceProvider;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.DocSpaceImpl;

/**
 *
 * @author 10044893
 *
 */
public class RootDocSpaceProvider extends AbstractSpaceProvider {

    private static final Log log = LogFactory.getLog(RootDocSpaceProvider.class);

    private final DocumentModel rootDoc;

    public RootDocSpaceProvider(DocumentModel rootDoc) {
        this.rootDoc = rootDoc;
    }

    public void add(Space o, CoreSession session) throws SpaceException {
        try {
            DocSpaceImpl space = DocSpaceImpl.createFromSpace(o,
                    rootDoc.getPathAsString(), session);
            session.saveDocument(space.getDocument());
            session.save();
        } catch (ClientException e) {
            throw new SpaceException(e);
        }
    }

    public void addAll(Collection<? extends Space> c, CoreSession session)
            throws SpaceException {
        try {
            for (Space o : c) {
                add(o, session);
            }
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public void clear(CoreSession session) throws SpaceException {
        try {
            session.removeChildren(rootDoc.getRef());
        } catch (ClientException e) {
            throw new SpaceException("Unable to complete clear", e);
        }

    }

    public Space doGetSpace(String spaceName, CoreSession session)
            throws SpaceException {
        DocumentModel doc;
        try {
            doc = session.getChild(rootDoc.getRef(), spaceName);
        } catch (ClientException e) {
            throw new SpaceNotFoundException(e);
        }
        return doc.getAdapter(Space.class);

    }

    public boolean isEmpty(CoreSession session) throws SpaceException {
        return this.size(session) == 0;
    }

    public boolean remove(Space space, CoreSession session)
            throws SpaceException {
        DocumentRef spaceRef = new PathRef(rootDoc.getPathAsString() + "/"
                + space.getName());
        try {
            if (session.exists(spaceRef)) {
                session.removeDocument(spaceRef);
                return true;
            } else {
                return false;
            }
        } catch (ClientException e) {
            throw new SpaceNotFoundException(e);
        }
    }

    public long size(CoreSession session) throws SpaceException {
        try {
            return session.getChildrenIterator(rootDoc.getRef()).size();
        } catch (ClientException e) {
            throw new SpaceException(e);
        }
    }

    public List<Space> getAll(CoreSession session) throws SpaceException {
        List<Space> spaces = new ArrayList<Space>();
        try {
            for (DocumentModel doc : session.getChildren(rootDoc.getRef())) {
                Space space = doc.getAdapter(Space.class);
                if (space != null) {
                    spaces.add(space);
                }
            }
        } catch (ClientException e) {
            throw new SpaceException("Unable to query childrens", e);
        }
        return spaces;
    }

    public boolean isReadOnly(CoreSession session) {
        return false;
    }

}
