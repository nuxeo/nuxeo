/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.text.DateFormat;
import java.util.Date;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.w3c.dom.Element;

/**
 * Restlet returning the locking information on a document.
 */
public class LockingRestlet extends BaseStatelessNuxeoRestlet {

    public static final String LOCK = "lock";

    public static final String UNLOCK = "unlock";

    public static final String STATUS = "status";

    public static final String STATE = "state";

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
        String action = STATUS;
        if (req.getResourceRef().getSegments().size() > 5) {
            action = req.getResourceRef().getSegments().get(5).toLowerCase();
        }
        if (req.getMethod().equals(Method.LOCK)) {
            action = LOCK;
        }
        if (req.getMethod().equals(Method.UNLOCK)) {
            action = UNLOCK;
        }

        String response;
        String code;
        if (action.equals(LOCK)) {
            try {
                Lock lock = session.getLockInfo(targetDocRef);
                if (lock == null) {
                    session.setLock(targetDocRef);
                    session.save();
                    response = "lock acquired on document " + docid;
                    code = SC_LOCKED_OK;
                } else if (lock.getOwner().equals(cUserName)) {
                    response = "document " + docid + " is already locked by you";
                    code = SC_ALREADY_LOCKED_OK;
                } else {
                    response = "document " + docid + " is already locked by " + lock.getOwner();
                    code = SC_ALREADY_LOCKED_KO;
                }
            } catch (NuxeoException e) {
                handleError(result, res, e);
                return;
            }
        } else if (action.equals(UNLOCK)) {

            try {
                Lock lock = session.getLockInfo(targetDocRef);
                if (lock == null) {
                    response = "document " + docid + " is not locked";
                    code = SC_ALREADY_UNLOCKED_OK;
                } else if (lock.getOwner().equals(cUserName)) {
                    session.removeLock(targetDocRef);
                    session.save();
                    response = "document " + docid + " unlocked";
                    code = SC_UNLOCKED_OK;
                } else {
                    response = "document " + docid + " is locked by " + lock.getOwner();
                    code = SC_ALREADY_LOCKED_KO;
                }
            } catch (NuxeoException e) {
                handleError(result, res, e);
                return;
            }

        } else if (action.equals(STATUS)) {
            try {
                Lock lock = session.getLockInfo(targetDocRef);
                response = oldLockKey(session.getLockInfo(targetDocRef));
                if (lock == null) {
                    code = SC_LOCKINFO_NOT_LOCKED;
                } else {
                    code = SC_LOCKINFO_LOCKED;
                }
            } catch (NuxeoException e) {
                handleError(result, res, e);
                return;
            }

        } else if (action.equals(STATE)) {
            try {
                Lock lock = session.getLockInfo(targetDocRef);
                if (lock == null) {
                    code = SC_LOCKINFO_NOT_LOCKED;
                    response = "";
                } else {
                    code = SC_LOCKINFO_LOCKED;
                    response = lock.getOwner() + '/'
                            + ISODateTimeFormat.dateTime().print(new DateTime(lock.getCreated()));
                }
            } catch (NuxeoException e) {
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

    protected String oldLockKey(Lock lock) {
        if (lock == null) {
            return null;
        }
        // return deprecated format, like "someuser:Nov 29, 2010"
        String lockCreationDate = (lock.getCreated() == null) ? null : DateFormat.getDateInstance(DateFormat.MEDIUM)
                                                                                 .format(new Date(
                                                                                         lock.getCreated()
                                                                                             .getTimeInMillis()));
        return lock.getOwner() + ':' + lockCreationDate;
    }

}
