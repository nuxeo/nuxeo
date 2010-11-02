/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.text.DateFormat;
import java.util.Date;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.w3c.dom.Element;

public class LockingRestlet extends BaseStatelessNuxeoRestlet {

    public static final String LOCK = "lock";
    public static final String UNLOCK = "unlock";
    public static final String STATUS = "status";

    public static final String SC_LOCKINFO_LOCKED = "LOCKED";
    public static final String SC_LOCKINFO_NOT_LOCKED = "NOTLOCKED";
    public static final String SC_LOCKED_OK = "OK";
    public static final String SC_ALREADY_LOCKED_KO = "ALREADYLOCKED";
    public static final String SC_ALREADY_LOCKED_OK = "ALREADYLOCKEDBYYOU";
    public static final String SC_UNLOCKED_OK = "OK";
    public static final String SC_ALREADY_UNLOCKED_OK = "NOT LOCKED";

    @Override
    protected void doHandleStatelessRequest(Request req, Response res) {

        String repoId = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");

        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domFactory.createDocument();

        // init repo and document
        boolean initOk = initRepositoryAndTargetDocument(res, repoId, docid);
        if (!initOk) {
            return;
        }

        String cUserName = getUserPrincipal(req).getName();

        // get Action
        String action=STATUS;
        if (req.getResourceRef().getSegments().size()>5) {
            action = req.getResourceRef().getSegments().get(5).toLowerCase();
        }
        if (req.getMethod().equals(Method.LOCK)) {
            action = LOCK;
        }
        if (req.getMethod().equals(Method.UNLOCK)) {
            action = UNLOCK;
        }

        String response = "";
        String code = "";
        if (action.equals(LOCK)) {
            try {
                String existingLock = session.getLock(targetDocRef);
                String lockingUser = getUserFromLockToken(existingLock);
                String userLockKey = getLockToken(cUserName);

                if (existingLock == null || "".equals(existingLock.trim())) {
                    session.setLock(targetDocRef, userLockKey);
                    session.save();
                    response = "lock aquired on document " + docid;
                    code = SC_LOCKED_OK;
                } else if (lockingUser.equals(cUserName)) {
                    response = "document " + docid + " is already locked by you";
                    code = SC_ALREADY_LOCKED_OK;
                } else {
                    response = "document " + docid + " is already locked by " + lockingUser;
                    code = SC_ALREADY_LOCKED_KO;
                }
            }
            catch (ClientException e) {
                handleError(result, res, e);
                return;
            }
        } else if (action.equals(UNLOCK)) {

            try {
                String existingLock = session.getLock(targetDocRef);
                String lockingUser = getUserFromLockToken(existingLock);

                if (existingLock == null) {
                    response = "document " + docid + " is not locked";
                    code = SC_ALREADY_UNLOCKED_OK;
                } else if (lockingUser.equals(cUserName)) {
                    session.unlock(targetDocRef);
                    session.save();
                    response = "document " + docid + " unlocked";
                    code = SC_UNLOCKED_OK;
                } else {
                    response = "document " + docid + " is locked by " + existingLock;
                    code = SC_ALREADY_LOCKED_KO;
                }
            }
            catch (ClientException e) {
                handleError(result, res, e);
                return;
            }

        } else if (action.equals(STATUS)) {
            try {
                response = session.getLock(targetDocRef);
                if (response == null || "".equals(response)) {
                    code = SC_LOCKINFO_NOT_LOCKED;
                } else {
                    code = SC_LOCKINFO_LOCKED;
                }
            }
            catch (ClientException e) {
                handleError(result, res, e);
                return;
            }

        } else {
            handleError(result, res, "Unsupported operation");
            return;
        }

        Element current = result.createElement("document");
        current.setAttribute("code", code);
        current.setAttribute("message", response);
        result.setRootElement((org.dom4j.Element) current);
        res.setEntity(result.asXML(), MediaType.TEXT_XML);
        res.getEntity().setCharacterSet(CharacterSet.UTF_8);
    }

    private static String getLockToken(String user) {
        return user + ":" + DateFormat.getDateInstance(
                DateFormat.MEDIUM).format(new Date());
    }

    private static String getUserFromLockToken(String token) {
        if (token == null) {
            return null;
        }
        if (token.contains(":")) {
            return token.split(":")[0];
        } else {
            return null;
        }
    }

}
