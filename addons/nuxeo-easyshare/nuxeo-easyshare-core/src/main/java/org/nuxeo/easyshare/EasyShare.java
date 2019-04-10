/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Michal Obrebski - Nuxeo
 */

package org.nuxeo.easyshare;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.notification.api.Notification;
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

  private static final String DEFAULT_PAGE_INDEX = "0";
  private static final Long PAGE_SIZE = 20L;
  private static final String SHARE_DOC_TYPE = "EasyShareFolder";
  private static AutomationService automationService;
  protected final Log log = LogFactory.getLog(EasyShare.class);

  @GET
  public Object doGet() {
    return getView("index");
  }

  public EasyShareUnrestrictedRunner buildUnrestrictedRunner(final String docId, final Long pageIndex) {

    return new EasyShareUnrestrictedRunner() {
      @Override
      public Object run(CoreSession session, IdRef docRef) throws NuxeoException {
        if (session.exists(docRef)) {
          DocumentModel docShare = session.getDocument(docRef);

          if (!SHARE_DOC_TYPE.equals(docShare.getType())) {
            return Response.serverError().status(Response.Status.NOT_FOUND).build();
          }

          if (!checkIfShareIsValid(docShare)) {
            return getView("expired").arg("docShare", docShare);
          }

          DocumentModel document = session.getDocument(new IdRef(docId));

          String query = buildQuery(document);

          if (query == null) {
            return getView("denied");
          }

          try (OperationContext opCtx = new OperationContext(session)) {
            OperationChain chain = new OperationChain("getEasyShareContent");
            chain.add("Document.Query")
                .set("query", query)
                .set("currentPageIndex", pageIndex)
                .set("pageSize", PAGE_SIZE);

            PaginableDocumentModelListImpl paginable = (PaginableDocumentModelListImpl) getAutomationService().run(opCtx, chain);

            try (OperationContext ctx = new OperationContext(session)) {
                ctx.setInput(docShare);

                // Audit Log
                Map<String, Object> params = new HashMap<>();
                params.put("event", "Access");
                params.put("category", "Document");
                params.put("comment", "IP: " + getIpAddr());
                getAutomationService().run(ctx, "Audit.Log", params);
            }

            return getView("folderList")
                .arg("isFolder", document.isFolder() && !SHARE_DOC_TYPE.equals(document.getType()))  //Backward compatibility to non-collection
                .arg("currentPageIndex", paginable.getCurrentPageIndex())
                .arg("numberOfPages", paginable.getNumberOfPages())
                .arg("docShare", docShare)
                .arg("docList", paginable)
                .arg("previousPageAvailable", paginable.isPreviousPageAvailable())
                .arg("nextPageAvailable", paginable.isNextPageAvailable())
                .arg("currentPageStatus", paginable.getProvider().getCurrentPageStatus());

          } catch (Exception ex) {
            log.error(ex.getMessage());
            return getView("denied");
          }

        } else {
          return getView("denied");
        }
      }
    };
  }


  protected static String buildQuery(DocumentModel documentModel) {

	  //Backward compatibility to non-collection
    if (documentModel.isFolder() && !SHARE_DOC_TYPE.equals(documentModel.getType())) {
      return " SELECT * FROM Document WHERE ecm:parentId = '" + documentModel.getId() + "' AND " +
          "ecm:mixinType != 'HiddenInNavigation' AND " +
          "ecm:mixinType != 'NotCollectionMember' AND " +
          "ecm:isVersion = 0 AND " +
          "ecm:isTrashed = 0"
          + "ORDER BY dc:title";

    } else if (SHARE_DOC_TYPE.equals(documentModel.getType())) {
      return "SELECT * FROM Document where ecm:mixinType != 'HiddenInNavigation' AND " +
          "ecm:isVersion = 0 AND ecm:isTrashed = 0 " +
          "AND collectionMember:collectionIds/* = '" + documentModel.getId() + "'" +
          "OR ecm:parentId = '" + documentModel.getId() + "'"
                  + "ORDER BY dc:title";
    }
    return null;
  }

  private boolean checkIfShareIsValid(DocumentModel docShare) {
    Date today = new Date();
    Date expired = docShare.getProperty("dc:expired").getValue(Date.class);
    if (expired == null) {
      log.error("Invalid null dc:expired for share: " + docShare.getTitle() + " (" + docShare.getId() + ")");
      // consider the share as expired
      return false;
    }
    if (today.after(expired)) {

      //Email notification
      Map<String, Object> mail = new HashMap<>();
      sendNotification("easyShareExpired", docShare, mail);

      return false;
    }
    return true;
  }


  private static AutomationService getAutomationService() {
    if (automationService == null) {
      automationService = Framework.getService(AutomationService.class);
    }
    return automationService;
  }


  @Path("{shareId}/{folderId}")
  @GET
  public Object getFolderListing(@PathParam("shareId") String shareId, @PathParam("folderId") final String folderId,
                                 @DefaultValue(DEFAULT_PAGE_INDEX) @QueryParam("p") final Long pageIndex) {
    return buildUnrestrictedRunner(folderId, pageIndex).runUnrestricted(shareId);
  }

  @Path("{shareId}")
  @GET
  public Object getShareListing(@PathParam("shareId") String shareId,
                                @DefaultValue(DEFAULT_PAGE_INDEX) @QueryParam("p") Long pageIndex) {
    return buildUnrestrictedRunner(shareId, pageIndex).runUnrestricted(shareId);
  }

  public String getFileName(DocumentModel doc) throws NuxeoException {
    BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
    if (blobHolder != null && blobHolder.getBlob() != null) {
      return blobHolder.getBlob().getFilename();
    }
    return doc.getName();
  }

  @GET
  @Path("{shareId}/{fileId}/{fileName}")
  public Response getFileStream(@PathParam("shareId") final String shareId, @PathParam("fileId") String fileId) throws NuxeoException {

    return (Response) new EasyShareUnrestrictedRunner() {
      @Override
      public Object run(CoreSession session, IdRef docRef) throws NuxeoException {
                if (session.exists(docRef)) {
                    DocumentModel doc = session.getDocument(docRef);
                    try (OperationContext ctx = new OperationContext(session)) {
                        DocumentModel docShare = session.getDocument(new IdRef(shareId));

                        if (!checkIfShareIsValid(docShare)) {
                            return Response.serverError().status(Response.Status.NOT_FOUND).build();
                        }

                        Blob blob = doc.getAdapter(BlobHolder.class).getBlob();

                        // Audit Log
                        ctx.setInput(doc);

                        // Audit.Log automation parameter setting
                        Map<String, Object> params = new HashMap<>();
                        params.put("event", "Download");
                        params.put("category", "Document");
                        params.put("comment", "IP: " + getIpAddr());
                        AutomationService service = Framework.getService(AutomationService.class);
                        service.run(ctx, "Audit.Log", params);

                        if (doc.isProxy()) {
                            DocumentModel liveDoc = session.getSourceDocument(docRef);
                            ctx.setInput(liveDoc);
                            service.run(ctx, "Audit.Log", params);

                        }

                        // Email notification
                        Map<String, Object> mail = new HashMap<>();
                        mail.put("filename", blob.getFilename());
                        sendNotification("easyShareDownload", docShare, mail);

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

  public void sendNotification(String notification, DocumentModel docShare, Map<String, Object> mail) {

    Boolean hasNotification = docShare.getProperty("eshare:hasNotification").getValue(Boolean.class);

    if (hasNotification) {
      //Email notification
      String email = docShare.getProperty("eshare:contactEmail").getValue(String.class);
      if (StringUtils.isEmpty(email)) {
          return;
      }
      try {
        log.debug("Easyshare: starting email");
        EmailHelper emailHelper = new EmailHelper();
        Map<String, Object> mailProps = new HashMap<>();
        mailProps.put("mail.from", Framework.getProperty("mail.from", "system@nuxeo.com"));
        mailProps.put("mail.to", email);
        mailProps.put("ip", getIpAddr());
        mailProps.put("docShare", docShare);

        try {
          Notification notif = NotificationServiceHelper.getNotificationService().getNotificationByName(notification);

          if (notif.getSubjectTemplate() != null) {
            mailProps.put(NotificationConstants.SUBJECT_TEMPLATE_KEY, notif.getSubjectTemplate());
          }

          mailProps.put(NotificationConstants.SUBJECT_KEY, NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix() + " " + notif.getSubject());
          mailProps.put(NotificationConstants.TEMPLATE_KEY, notif.getTemplate());

          mailProps.putAll(mail);

          emailHelper.sendmail(mailProps);

        } catch (NuxeoException e) {
          log.warn(e.getMessage());
        }

        log.debug("Easyshare: completed email");
      } catch (Exception ex) {
        log.error("Cannot send easyShare notification email", ex);
      }
    }
  }


  protected String getIpAddr() {
      String ip = request.getHeader("X-FORWARDED-FOR");
      if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
          ip = request.getHeader("Proxy-Client-IP");
      }
      if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
          ip = request.getRemoteAddr();
      }
      return ip;
   }
}
