/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
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
    public Object getFolderListing(@PathParam("folderId")
    String folderId) {
        try {
            return new EasyShareUnrestrictedRunner() {
                @Override
                public Object run(CoreSession session, IdRef docRef)
                        throws ClientException {
                    if (session.exists(docRef)) {
                        DocumentModel docFolder = session.getDocument(docRef);

                        Date today = new Date();
                        if (today.after(docFolder.getProperty("dc:expired").getValue(
                                Date.class))) {
                            return getView("denied");
                        }

                        if (!docFolder.getType().equals("EasyShareFolder")) {
                            return Response.serverError().status(
                                    Response.Status.NOT_FOUND).build();
                        }

                        DocumentModelList docList = session.getChildren(docRef);

                        // Audit Log
                        OperationContext ctx = new OperationContext(session);
                        ctx.setInput(docFolder);

                        // Audit.Log operation parameter setting
                        try {
                            Map<String, Object> params = new HashMap<String, Object>();
                            params.put("event", "Access");
                            params.put("category", "Document");
                            params.put("comment",
                                    "IP: " + request.getRemoteAddr());
                            AutomationService service = Framework.getLocalService(AutomationService.class);
                            service.run(ctx, "Audit.Log", params);
                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                            return getView("denied");
                        }

                        return getView("folderList").arg("docFolder", docFolder).arg(
                                "docList", docList);
                    } else {

                        return getView("denied");
                    }
                }
            }.runUnrestricted(folderId);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);

        }

    }

    @GET
    @Path("{folderId}/{fileId}/{fileName}")
    public Response getFileStream(@PathParam("fileId")
    String fileId) throws ClientException {

        try {
            return (Response) new EasyShareUnrestrictedRunner() {
                @Override
                public Object run(CoreSession session, IdRef docRef)
                        throws ClientException {
                    if (session.exists(docRef)) {
                        try {
                            DocumentModel doc = session.getDocument(docRef);

                            Blob blob = doc.getAdapter(BlobHolder.class).getBlob();
                            DocumentModel docFolder = session.getDocument(doc.getParentRef());

                            // Audit Log
                            OperationContext ctx = new OperationContext(session);
                            ctx.setInput(doc);

                            Date today = new Date();
                            if (today.after(docFolder.getProperty("dc:expired").getValue(
                                    Date.class))) {
                                return Response.serverError().status(
                                        Response.Status.NOT_FOUND).build();

                            }

                            // Audit.Log operation parameter setting
                            Map<String, Object> params = new HashMap<String, Object>();
                            params.put("event", "Download");
                            params.put("category", "Document");
                            params.put("comment",
                                    "IP: " + request.getRemoteAddr());
                            AutomationService service = Framework.getLocalService(AutomationService.class);
                            service.run(ctx, "Audit.Log", params);

                            if (doc.isProxy()) {
                                DocumentModel liveDoc = session.getSourceDocument(docRef);
                                ctx.setInput(liveDoc);
                                service.run(ctx, "Audit.Log", params);
                            }

                            // //Email notification
                            // EmailHelper emailer = new EmailHelper();
                            // String shareOwnerEmail = "mobrebski@nuxeo.com";
                            // Hashtable mail = new Hashtable();
                            // mail.put("from", "dion@almaer.com");
                            // mail.put("to", shareOwnerEmail);
                            // mail.put("subject", "EasyShare Download");
                            // mail.put("body",
                            // "File from Share downloaded by IP");
                            // emailer.sendmail(mail);

                            return Response.ok(blob.getStream(),
                                    blob.getMimeType()).build();

                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                            return Response.serverError().status(
                                    Response.Status.NOT_FOUND).build();
                        }

                    } else {
                        return Response.serverError().status(
                                Response.Status.NOT_FOUND).build();
                    }
                }
            }.runUnrestricted(fileId);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);

        }

    }

}
