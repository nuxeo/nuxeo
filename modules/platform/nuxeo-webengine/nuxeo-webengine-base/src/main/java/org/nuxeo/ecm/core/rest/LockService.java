/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest;

import static org.nuxeo.common.utils.DateUtils.formatISODateTime;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * Lock Service - manages locks on documents.
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li>GET - get the Lock Owner if any
 * <li>POST - Lock the document using current login information as the lock owner
 * <li>DELETE - Delete the lock
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name = "lock", type = "LockService", targetType = "Document")
public class LockService extends DefaultAdapter {

    @GET
    public Object doGet() {
        try {
            DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
            Lock lock = ctx.getCoreSession().getLockInfo(doc.getRef());
            return lock.getOwner() + '/' + formatISODateTime(lock.getCreated().getTime());
        } catch (NuxeoException e) {
            e.addInfo("Failed to get lock on document");
            throw e;
        }
    }

    @DELETE
    public Object removeLock() {
        try {
            DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
            ctx.getCoreSession().removeLock(doc.getRef());
            doc.refresh();
            return null; // TODO
        } catch (NuxeoException e) {
            e.addInfo("Failed to unlock document");
            throw e;
        }
    }

    @POST
    public Object doPost() {
        try {
            DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
            ctx.getCoreSession().setLock(doc.getRef());
            doc.refresh();
            return null; // TODO
        } catch (NuxeoException e) {
            e.addInfo("Failed to lock document");
            throw e;
        }
    }

}
