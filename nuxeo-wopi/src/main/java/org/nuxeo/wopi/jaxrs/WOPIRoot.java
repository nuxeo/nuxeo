/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.wopi.jaxrs;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.wopi.FileInfo;
import org.nuxeo.wopi.Helpers;
import org.nuxeo.wopi.exception.NotFoundException;
import org.nuxeo.wopi.lock.LockHelper;

/**
 * @since 10.3
 */
@Path("/wopi")
@WebObject(type = "wopi")
public class WOPIRoot extends ModuleRoot {

    protected static final String THREAD_NAME_PREFIX = "WOPI_";

    @Path("/files/{fileId}")
    public Object filesResource(@PathParam("fileId") FileInfo fileInfo) {
        // prefix thread name for logging purpose
        prefixThreadName();

        WebContext context = getContext();
        context.setRepositoryName(fileInfo.repositoryName);
        CoreSession session = context.getCoreSession();
        // flag the session's principal as a WOPI user for locking policy purpose
        // TODO find a better way, see NXP-25855
        LockHelper.markAsWOPIUser(session.getPrincipal());
        DocumentModel doc = getDocument(session, fileInfo.docId);
        Blob blob = getBlob(doc, fileInfo.xpath);
        return newObject("wopiFiles", session, doc, blob, fileInfo.xpath);
    }

    protected void prefixThreadName() {
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        if (!threadName.startsWith(THREAD_NAME_PREFIX)) {
            currentThread.setName(THREAD_NAME_PREFIX + threadName);
        }
    }

    protected DocumentModel getDocument(CoreSession session, String fileId) {
        DocumentRef ref = new IdRef(fileId);
        if (!session.exists(ref)) {
            throw new NotFoundException();
        }
        return session.getDocument(ref);
    }

    protected Blob getBlob(DocumentModel doc, String xpath) {
        Blob blob = Helpers.getEditableBlob(doc, xpath);
        if (blob == null) {
            throw new NotFoundException();
        }
        return blob;
    }

}
