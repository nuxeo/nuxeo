/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Michal Obrebski - Nuxeo
 */

package org.nuxeo.easyshare;

import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * The root entry for the WebEngine module.
 *
 * @author mikeobrebski
 */
@Path("/easyshare")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "EasyShare")
public class EasyShare extends ModuleRoot {

    protected final Log log = LogFactory.getLog(EasyShare.class);

    @GET
    public Object doGet() {
        return getView("index");
    }

    @Path("{folderId}")
    @GET
    public Object getFolderListing(@PathParam("folderId") String folderId) {
        EasyShareUnrestrictedRunner runner = new EasyShareUnrestrictedRunner(
                getContext().getCoreSession(), folderId) {
            @Override
            public void run() throws ClientException {
                if (session.exists(docRef)) {
                    DocumentModel docFolder = session.getDocument(docRef);
                    Date today = new Date();
                    if (today.after(docFolder.getProperty("dc:expired").getValue(
                            Date.class))) {
                        result = getView("denied");
                    }
                    if (!docFolder.getType().equals("EasyShareFolder")) {
                        result = Response.status(Status.NOT_FOUND).build();
                    }
                    DocumentModelList docList = session.getChildren(docRef);
                    // Audit Log
                    OperationContext opCtx = new OperationContext(session);
                    opCtx.setInput(docFolder);
                    // Audit.Log operation parameter setting
                    try {
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("event", "Access");
                        params.put("category", "Document");
                        params.put("comment", "IP: " + request.getRemoteAddr());
                        AutomationService service = Framework.getLocalService(AutomationService.class);
                        service.run(opCtx, "Audit.Log", params);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("interrupted", e);
                    } catch (Exception e) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new RuntimeException(e);
                        }
                        log.error(e.getMessage(), e);
                        result = getView("denied");
                    }
                    result = getView("folderList").arg("docFolder", docFolder).arg(
                            "docList", docList);
                } else {
                    result = getView("denied");
                }
            }
        };
        try {
            runner.runUnrestricted();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
        return runner.getResult();
    }

    @GET
    @Path("{folderId}/{fileId}/{fileName}")
    public Object getFileStream(@PathParam("fileId") String fileId) {
        EasyShareUnrestrictedRunner runner = new EasyShareUnrestrictedRunner(
                getContext().getCoreSession(), fileId) {
            @Override
            public void run() throws ClientException {
                if (session.exists(docRef)) {
                    try {
                        DocumentModel doc = session.getDocument(docRef);
                        Blob blob = doc.getAdapter(BlobHolder.class).getBlob();
                        DocumentModel docFolder = session.getDocument(doc.getParentRef());
                        // Audit Log
                        OperationContext opCtx = new OperationContext(session);
                        opCtx.setInput(doc);
                        Date today = new Date();
                        if (today.after(docFolder.getProperty("dc:expired").getValue(
                                Date.class))) {
                            result = Response.status(Status.NOT_FOUND).build();
                        }
                        // Audit.Log operation parameter setting
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("event", "Download");
                        params.put("category", "Document");
                        params.put("comment", "IP: " + request.getRemoteAddr());
                        AutomationService service = Framework.getLocalService(AutomationService.class);
                        service.run(opCtx, "Audit.Log", params);
                        if (doc.isProxy()) {
                            DocumentModel liveDoc = session.getSourceDocument(docRef);
                            opCtx.setInput(liveDoc);
                            service.run(opCtx, "Audit.Log", params);
                        }
                        // Email notification
                        try {
                            log.debug("Easyshare: starting email");
                            EmailHelper emailer = new EmailHelper();
                            Map<String, Object> mailProps = new Hashtable<String, Object>();
                            mailProps.put("mail.from", "mobrebski@nuxeo.com");
                            mailProps.put("mail.to", "mobrebski@nuxeo.com");
                            mailProps.put("subject", "EasyShare Download");
                            mailProps.put("body",
                                    "File from Share downloaded by IP");
                            mailProps.put("template", "easyShareEmail");
                            mailProps.put("subjectTemplate", "easyShareEmail");
                            emailer.sendmail(mailProps);
                            log.debug("Easyshare: completed email");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("interrupted", e);
                        } catch (Exception e) {
                            if (Thread.currentThread().isInterrupted()) {
                                throw e;
                            }
                            log.error(
                                    "Cannot send easyShare notification email",
                                    e);
                        }
                        result = Response.ok(blob.getStream(),
                                blob.getMimeType()).build();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("interrupted", e);
                    } catch (Exception e) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new RuntimeException(e);
                        }
                        log.error(e.getMessage(), e);
                        result = Response.status(Status.NOT_FOUND).build();
                    }
                } else {
                    result = Response.status(Status.NOT_FOUND).build();
                }
            }
        };
        try {
            runner.runUnrestricted();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
        return runner.getResult();
    }

}
