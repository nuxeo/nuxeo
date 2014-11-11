/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.spaces.core.contribs.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.spaces.api.AbstractSpaceProvider;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;

public class SingleDocSpaceProvider extends AbstractSpaceProvider {

    private static final String PARAM_PATH = "path";

    private String path;

    private String title;

    @Override
    public void initialize(Map<String, String> params) throws Exception {
        if (!params.containsKey(PARAM_PATH)) {
            throw new Exception(
                    "No path argument found for SingleDocSpaceProvider");
        }
        path = params.get(PARAM_PATH);
        title = params.get("title");
        if (null == title) {
            title = getDocName(path);
        }
    }

    public long size(CoreSession session) {
        return 1;
    }

    public boolean isReadOnly(CoreSession session) {
        return true;
    }

    public List<Space> getAll(CoreSession session) throws SpaceException {
        List<Space> result = new ArrayList<Space>();
        result.add(getSpace("", session));
        return result;
    }

    public Space doGetSpace(String spaceName, CoreSession session)
            throws SpaceException {
        return getOrCreateSingleSpace(session).getAdapter(Space.class);
    }

    public boolean isEmpty(CoreSession session) {
        return false;
    }

    private DocumentModel getOrCreateSingleSpace(CoreSession session)
            throws SpaceException {
        PathRef docRef = new PathRef(path);
        try {
            if (session.exists(docRef)) {
                return session.getDocument(docRef);
            } else {
                if (!session.exists(new PathRef(getParentPath(path)))) {
                    throw new ClientException(
                            "Parent path does not exist : unable to get or create space");
                }

                UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(
                        session) {
                    @Override
                    public void run() throws ClientException {
                        DocumentModel doc = session.createDocumentModel(
                                getParentPath(path), getDocName(path), "Space");
                        doc.setPropertyValue("dc:title", title);
                        doc = session.createDocument(doc);
                        session.save();

                    }
                };

                runner.runUnrestricted();
                return session.getDocument(docRef);
            }
        } catch (ClientException e) {
            throw new SpaceNotFoundException(e);
        }
    }

    static String getParentPath(String fullPath) {
        int firstCharOfDocName = fullPath.lastIndexOf("/");
        if (firstCharOfDocName == -1) {
            return fullPath;
        } else {
            if (firstCharOfDocName > 0) {
                return fullPath.substring(0, firstCharOfDocName);
            } else {
                return "/";
            }
        }
    }

    static String getDocName(String fullPath) {
        int firstCharOfDocName = fullPath.lastIndexOf("/");
        if (firstCharOfDocName == -1) {
            return "";
        } else {
            return fullPath.substring(firstCharOfDocName + 1);
        }
    }

}
