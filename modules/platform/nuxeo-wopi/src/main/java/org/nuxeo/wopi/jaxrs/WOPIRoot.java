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

import static org.nuxeo.wopi.Constants.WOPI_BASE_URL_PROPERTY;
import static org.nuxeo.wopi.Headers.PROOF;
import static org.nuxeo.wopi.Headers.PROOF_OLD;
import static org.nuxeo.wopi.Headers.TIMESTAMP;

import java.util.function.Supplier;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.web.common.RequestContext;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.wopi.Constants;
import org.nuxeo.wopi.FileInfo;
import org.nuxeo.wopi.Helpers;
import org.nuxeo.wopi.WOPIService;
import org.nuxeo.wopi.exception.InternalServerErrorException;
import org.nuxeo.wopi.exception.NotFoundException;
import org.nuxeo.wopi.lock.LockHelper;

/**
 * @since 10.3
 */
@Path("/")
@WebObject(type = "wopi")
public class WOPIRoot extends ModuleRoot {

    protected static final String THREAD_NAME_PREFIX = "WOPI_";

    @Context
    protected HttpHeaders httpHeaders;

    @Path("/files/{fileId}")
    public Object filesResource(@PathParam("fileId") FileInfo fileInfo) {
        // prefix thread name for logging purpose
        return prefixThreadName(() -> {
            // verify proof key
            verifyProofKey();

            // flag the request as originating from a WOPI client for locking policy purpose
            LockHelper.flagWOPIRequest();
            RequestContext.getActiveContext(request).addRequestCleanupHandler(req -> LockHelper.unflagWOPIRequest());

            WebContext context = getContext();
            context.setRepositoryName(fileInfo.repositoryName);
            CoreSession session = context.getCoreSession();
            DocumentModel doc = getDocument(session, fileInfo.docId);
            Blob blob = getBlob(doc, fileInfo.xpath);
            return newObject("wopiFiles", session, doc, blob, fileInfo.xpath);
        });
    }

    protected <T> T prefixThreadName(Supplier<T> supplier) {
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        if (!threadName.startsWith(THREAD_NAME_PREFIX)) {
            currentThread.setName(THREAD_NAME_PREFIX + threadName);
        }
        try {
            return supplier.get();
        } finally {
            currentThread.setName(threadName);
        }
    }

    protected void verifyProofKey() {
        String proofKeyHeader = Helpers.getHeader(httpHeaders, PROOF);
        String oldProofKeyHeader = Helpers.getHeader(httpHeaders, PROOF_OLD);
        String timestampHeader = Helpers.getHeader(httpHeaders, TIMESTAMP);
        String accessToken = request.getParameter(Constants.ACCESS_TOKEN_PARAMETER);
        String wopiBaseURL = Framework.getProperty(WOPI_BASE_URL_PROPERTY, VirtualHostHelper.getBaseURL(request));
        // remove /nuxeo/ from the request URI
        String requestURI = request.getRequestURI().substring(request.getContextPath().length() + 1);
        String url = String.format("%s%s?%s", wopiBaseURL, requestURI, request.getQueryString());
        if (!Framework.getService(WOPIService.class)
                      .verifyProofKey(proofKeyHeader, oldProofKeyHeader, url, accessToken, timestampHeader)) {
            throw new InternalServerErrorException();
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
