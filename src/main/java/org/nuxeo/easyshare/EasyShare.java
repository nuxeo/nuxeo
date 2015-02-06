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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

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
    return new EasyShareUnrestrictedRunner() {
      @Override
      public Object run(CoreSession session, IdRef docRef) throws ClientException {
        if (session.exists(docRef)) {
          DocumentModel docFolder = session.getDocument(docRef);

          if (!docFolder.getType().equals("EasyShareFolder")) {
            return Response.serverError().status(Response.Status.NOT_FOUND).build();
          }

          if (!checkIfShareIsValid(docFolder)) {
            return getView("expired").arg("docFolder", docFolder);
          }

          DocumentModelList docList = session.getFiles(docRef);

          // Audit Log
          OperationContext ctx = new OperationContext(session);
          ctx.setInput(docFolder);

          // Audit.Log operation parameter setting
          try {
            Map<String, Object> params = new HashMap();
            params.put("event", "Access");
            params.put("category", "Document");
            params.put("comment", "IP: " + request.getRemoteAddr());
            AutomationService service = Framework.getLocalService(AutomationService.class);
            service.run(ctx, "Audit.Log", params);
          } catch (Exception ex) {
            log.error(ex.getMessage());
            return getView("denied");
          }

          return getView("folderList").arg("docFolder", docFolder).arg("docList", docList);
        } else {

          return getView("denied");
        }
      }
    }.runUnrestricted(folderId);

  }

  private boolean checkIfShareIsValid(DocumentModel docFolder) {
    Date today = new Date();
    if (today.after(docFolder.getProperty("dc:expired").getValue(Date.class))) {

      //Email notification
      Map mail = new HashMap<>();
      mail.put("template", "easyShareExpired");

      sendNotification("easyShareExpired", docFolder, mail);

      return false;

    }
    return true;
  }

  public String getFileName(DocumentModel doc) throws ClientException {
    BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
    if (blobHolder != null && blobHolder.getBlob() != null) {
      return blobHolder.getBlob().getFilename();
    }
    return doc.getName();
  }

  @GET
  @Path("{folderId}/{fileId}/{fileName}")
  public Response getFileStream(@PathParam("fileId") String fileId) throws ClientException {

    return (Response) new EasyShareUnrestrictedRunner() {
      @Override
      public Object run(CoreSession session, IdRef docRef) throws ClientException {
        if (session.exists(docRef)) {
          try {
            DocumentModel doc = session.getDocument(docRef);
            DocumentModel docFolder = session.getDocument(doc.getParentRef());

            if (!checkIfShareIsValid(docFolder)) {
              return Response.serverError().status(Response.Status.NOT_FOUND).build();
            }

            Blob blob = doc.getAdapter(BlobHolder.class).getBlob();

            // Audit Log
            OperationContext ctx = new OperationContext(session);
            ctx.setInput(doc);

            // Audit.Log operation parameter setting
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("event", "Download");
            params.put("category", "Document");
            params.put("comment", "IP: " + request.getRemoteAddr());
            AutomationService service = Framework.getLocalService(AutomationService.class);
            service.run(ctx, "Audit.Log", params);

            if (doc.isProxy()) {
              DocumentModel liveDoc = session.getSourceDocument(docRef);
              ctx.setInput(liveDoc);
              service.run(ctx, "Audit.Log", params);
            }

            // Email notification
            Map mail = new HashMap<>();
            mail.put("template", "easyShareDownload");
            mail.put("filename", blob.getFilename());
            sendNotification("easyShareDownload", docFolder, mail);

            return Response.ok(blob.getStream(), blob.getMimeType()).build();

          } catch (Exception ex) {
            log.error("error ", ex);
            return Response.serverError().status(Response.Status.NOT_FOUND).build();
          }

        } else {
          return Response.serverError().status(Response.Status.NOT_FOUND).build();
        }
      }
    }.runUnrestricted(fileId);

  }

  public void sendNotification(String notification, DocumentModel docFolder, Map<String, Object> mail) {

    Boolean hasNotification = docFolder.getProperty("eshare:hasNotification").getValue(Boolean.class);

    if (hasNotification) {
      //Email notification
      String email = docFolder.getProperty("eshare:contactEmail").getValue(String.class);
      try {
        log.debug("Easyshare: starting email");
        EmailHelper emailHelper = new EmailHelper();
        Map<String, Object> mailProps = new Hashtable<String, Object>();
        mailProps.put("mail.from", Framework.getProperty("mail.from", "system@nuxeo.com"));
        mailProps.put("mail.to", email);
        mailProps.put("ip", this.request.getRemoteAddr());
        mailProps.put("docFolder", docFolder);

        try {
          Notification notif = NotificationServiceHelper.getNotificationService().getNotificationByName(notification);

          String subject = notif.getSubject() == null ? "Alert" : notif.getSubject();
          if (notif.getSubjectTemplate() != null) {
            subject = notif.getSubjectTemplate();
          }

          subject = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix() + " " + subject;

          mailProps.put("subject", subject);
          mailProps.put("template", notif.getTemplate());

          mailProps.putAll(mail);

          emailHelper.sendmail(mailProps);

        } catch (ClientException e) {
          log.warn(e.getMessage());
        }

        log.debug("Easyshare: completed email");
      } catch (Exception ex) {
        log.error("Cannot send easyShare notification email", ex);
      }
    }
  }

}