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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.BooleanUtils;
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

    protected final Log log = LogFactory.getLog(EasyShare.class);

    @GET
    public Object doGet() {
        return getView("index");
    }

    public EasyShareUnrestrictedRunner buildUnrestrictedRunner(final String jSessionId, final String accessCode,
            final String docId, final Long pageIndex) {

        return new EasyShareUnrestrictedRunner() {
            @Override
            public Object run(CoreSession session, IdRef docRef) throws NuxeoException {

                // Check if docRef exists preventing user as document not found
                if (!session.exists(docRef)) {
                    return getView("notfound");
                }

                DocumentModel docShare = session.getDocument(docRef);

                if (!SHARE_DOC_TYPE.equals(docShare.getType())) {
                    return Response.serverError().status(Response.Status.NOT_FOUND).build();
                }

                if (!checkIfShareIsValid(docShare)) {
                    return getView("expired").arg("docShare", docShare);
                }

                // Check if ref exists preventing user as document not found
                // instead of denied
                if (!session.exists(new IdRef(docId))) {
                    return getView("notfound");
                }

                DocumentModel document = session.getDocument(new IdRef(docId));

                // Check validity of accesscode (password)
                if (!checkIfAccessCodeIsValid(docShare, accessCode)) {
                    return getView("access").arg("docShare", docShare).arg("doc", document).arg("wrongAccessCode",
                            StringUtils.isNotEmpty(accessCode));
                }

                // Check validity of "authenticated client" comparing current JSESSIONID
                // with JSESSIONID captured when password was specified by user.
                // Goal is to avoid sending a link to another person without
                // entering the accessCode (password)
                if (StringUtils.isNotBlank(jSessionId) && StringUtils.isNotBlank(getRequestJSESSIONID())
                        && !jSessionId.equals(getRequestJSESSIONID())) {
                    return getView("denied");
                }

                String query = buildQuery(document);

                if (query == null) {
                    return getView("notfound");
                }

                try (OperationContext opCtx = new OperationContext(session)) {
                    OperationChain chain = new OperationChain("getEasyShareContent");
                    chain.add("Document.Query").set("query", query).set("currentPageIndex", pageIndex).set("pageSize",
                            PAGE_SIZE);

                    AutomationService automationService = Framework.getService(AutomationService.class);
                    PaginableDocumentModelListImpl paginable = (PaginableDocumentModelListImpl) automationService.run(
                            opCtx, chain);

                    try (OperationContext ctx = new OperationContext(session)) {
                        ctx.setInput(docShare);

                        // Audit Log
                        Map<String, Object> params = new HashMap<>();
                        params.put("event", "Access");
                        params.put("category", "Document");
                        params.put("comment", "IP: " + getIpAddr());
                        automationService.run(ctx, "Audit.Log", params);
                    }

                    // Retrieve SALT for encrypted password
                    String encryptedSalt = EasyShareEncryptionUtil.encode(EasyShareEncryptionUtil.getNextSalt(),
                            EasyShareEncryptionUtil.getDefaultSalt());

                    // Retrieve encrypted password with SALT
                    String encryptedAccessCode = EasyShareEncryptionUtil.encode(accessCode,
                            EasyShareEncryptionUtil.decode(encryptedSalt, EasyShareEncryptionUtil.getDefaultSalt()));

                    // Retrieve encrypted jsessionId
                    String encryptedJSessionId = EasyShareEncryptionUtil.encode(getRequestJSESSIONID(),
                            EasyShareEncryptionUtil.decode(encryptedSalt, EasyShareEncryptionUtil.getDefaultSalt()));

                    return getView("folderList")
                                                .arg("isFolder",
                                                        document.isFolder()
                                                                && !SHARE_DOC_TYPE.equals(document.getType())) // Backward
                                                                                                               // compatibility
                                                                                                               // to
                                                                                                               // non-collection
                                                .arg("currentPageIndex", paginable.getCurrentPageIndex())
                                                .arg("numberOfPages", paginable.getNumberOfPages())
                                                .arg("docShare", docShare)
                                                .arg("docList", paginable)
                                                .arg("previousPageAvailable", paginable.isPreviousPageAvailable())
                                                .arg("nextPageAvailable", paginable.isNextPageAvailable())
                                                .arg("currentPageStatus",
                                                        paginable.getProvider().getCurrentPageStatus())
                                                .arg("accessSecured", StringUtils.isNotEmpty(accessCode))
                                                .arg("encryptedAccessCode", encryptedAccessCode)
                                                .arg("encryptedSalt", encryptedSalt)
                                                .arg("encryptedJSessionId", encryptedJSessionId);

                } catch (Exception ex) {
                    log.error(ex.getMessage());
                    return getView("denied");
                }
            }
        };
    }

    public EasyShareUnrestrictedRunner buildUnrestrictedRunnerStream(final String jSessionId, final String accessCode,
            final String docId) {

        return new EasyShareUnrestrictedRunner() {
            @Override
            public Object run(CoreSession session, IdRef docRef) throws NuxeoException {

                // Check if docRef exists preventing user as document not found
                if (!session.exists(docRef)) {
                    return Response.serverError().status(Response.Status.NOT_FOUND).build();
                }

                DocumentModel doc = session.getDocument(docRef);
                try (OperationContext ctx = new OperationContext(session)) {
                    DocumentModel docShare = session.getDocument(new IdRef(docId));

                    if (!checkIfShareIsValid(docShare)) {
                        return Response.serverError().status(Response.Status.NOT_FOUND).build();
                    }

                    if (!checkIfAccessCodeIsValid(docShare, accessCode)) {
                        return Response.serverError().status(Response.Status.FORBIDDEN).build();
                    }

                    if (StringUtils.isBlank(jSessionId) || !jSessionId.equals(getRequestJSESSIONID())) {
                        return Response.serverError().status(Response.Status.FORBIDDEN).build();
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
            }
        };
    }

    protected static String buildQuery(DocumentModel documentModel) {

        // Backward compatibility to non-collection
        if (documentModel.isFolder() && !SHARE_DOC_TYPE.equals(documentModel.getType())) {
            return " SELECT * FROM Document WHERE ecm:parentId = '" + documentModel.getId() + "' AND "
                    + "ecm:mixinType != 'HiddenInNavigation' AND " + "ecm:mixinType != 'NotCollectionMember' AND "
                    + "ecm:isVersion = 0 AND " + "ecm:isTrashed = 0" + "ORDER BY dc:title";

        } else if (SHARE_DOC_TYPE.equals(documentModel.getType())) {
            return "SELECT * FROM Document where ecm:mixinType != 'HiddenInNavigation' AND "
                    + "ecm:isVersion = 0 AND ecm:isTrashed = 0 " + "AND collectionMember:collectionIds/* = '"
                    + documentModel.getId() + "'" + "OR ecm:parentId = '" + documentModel.getId() + "'"
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

            // Email notification
            Map<String, Object> mail = new HashMap<>();
            sendNotification("easyShareExpired", docShare, mail);

            return false;
        }
        return true;
    }

    private boolean checkIfAccessCodeIsValid(DocumentModel docShare, String accessCode) {
        // Retrieve metadata of current easyshare folder
        Boolean docAccessCodeRequired = docShare.getProperty("eshare:accessCodeRequired").getValue(Boolean.class);
        String docAccessCode = docShare.getProperty("eshare:accessCode").getValue(String.class);

        // Consider a default authorization if no password is not required in easyshare folder metadata (default
        // easyshare feature)
        if (BooleanUtils.isNotTrue(docAccessCodeRequired)) {
            // AccessCode not required
            return true;
        } else if (StringUtils.isNotBlank(accessCode) && accessCode.equals(docAccessCode)) {
            // AccessCode provided and mathing the accessCode defined in Easyshare folder
            return true;
        } else {
            log.error("accessCode is empty or does not match for share: " + docShare.getTitle() + " ("
                    + docShare.getId() + ")");
            // Email notification
            Map<String, Object> mail = new HashMap<>();
            sendNotification("easyShareWrongAccessCode", docShare, mail);
            return false;
        }
    }

    @Path("{shareId}/{folderId}")
    @GET
    public Object getFolderListing(@PathParam("shareId") String shareId, @PathParam("folderId") final String folderId,
            @DefaultValue(DEFAULT_PAGE_INDEX) @QueryParam("p") final Long pageIndex,
            @QueryParam("a") final String encryptedAccessCode, @QueryParam("s") final String encryptedSalt,
            @QueryParam("j") final String encryptedJSessionId) {

        String decryptedAccessCode = null;
        String decryptedJSessionId = null;

        try {
            if (StringUtils.isNotBlank(encryptedAccessCode) && StringUtils.isNotBlank(encryptedSalt)) {
                String decryptedSalt = EasyShareEncryptionUtil.decode(encryptedSalt,
                        EasyShareEncryptionUtil.getDefaultSalt());
                decryptedAccessCode = EasyShareEncryptionUtil.decode(encryptedAccessCode, decryptedSalt);
                decryptedJSessionId = EasyShareEncryptionUtil.decode(encryptedJSessionId, decryptedSalt);
            }
            return buildUnrestrictedRunner(decryptedJSessionId, decryptedAccessCode, folderId,
                    pageIndex).runUnrestricted(shareId);
        } catch (NuxeoException e) {
            // Error occured during decoding process of salt, accessCode or jSessionId from URL
            return getView("denied");
        }
    }

    @Path("{shareId}/{folderId}")
    @POST
    public Object postFolderListing(@PathParam("shareId") String shareId, @PathParam("folderId") final String folderId,
            @DefaultValue(DEFAULT_PAGE_INDEX) @FormParam("p") Long pageIndex,
            @FormParam("accessCode") final String accessCode) {
        return buildUnrestrictedRunner(getRequestJSESSIONID(), accessCode, folderId, pageIndex).runUnrestricted(
                shareId);
    }

    @Path("{shareId}")
    @GET
    public Object getShareListing(@PathParam("shareId") String shareId,
            @DefaultValue(DEFAULT_PAGE_INDEX) @QueryParam("p") Long pageIndex,
            @QueryParam("a") final String encryptedAccessCode, @QueryParam("s") final String encryptedSalt,
            @QueryParam("j") final String encryptedJSessionId) {

        String decryptedAccessCode = null;
        String decryptedJSessionId = null;

        try {
            if (StringUtils.isNotBlank(encryptedAccessCode) && StringUtils.isNotBlank(encryptedSalt)) {
                String decryptedSalt = EasyShareEncryptionUtil.decode(encryptedSalt,
                        EasyShareEncryptionUtil.getDefaultSalt());
                decryptedAccessCode = EasyShareEncryptionUtil.decode(encryptedAccessCode, decryptedSalt);
                decryptedJSessionId = EasyShareEncryptionUtil.decode(encryptedJSessionId, decryptedSalt);
            }
            return buildUnrestrictedRunner(decryptedJSessionId, decryptedAccessCode, shareId,
                    pageIndex).runUnrestricted(shareId);
        } catch (NuxeoException e) {
            // Error occured during decoding process of salt, accessCode or jSessionId from URL
            return getView("denied");
        }
    }

    @Path("{shareId}")
    @POST
    public Object postShareListing(@PathParam("shareId") String shareId,
            @DefaultValue(DEFAULT_PAGE_INDEX) @FormParam("p") Long pageIndex,
            @FormParam("accessCode") final String accessCode) {
        return buildUnrestrictedRunner(getRequestJSESSIONID(), accessCode, shareId, pageIndex).runUnrestricted(shareId);
    }

    @GET
    @Path("{shareId}/{fileId}/{fileName}")
    public Response getFileStream(@PathParam("shareId") final String shareId, @PathParam("fileId") String fileId,
            @PathParam("fileName") String fileName, @QueryParam("a") final String encryptedAccessCode,
            @QueryParam("s") final String encryptedSalt, @QueryParam("j") final String encryptedJSessionId)
            throws NuxeoException {

        String decryptedAccessCode = null;
        String decryptedJSessionId = null;

        try {
            if (StringUtils.isNotBlank(encryptedAccessCode) && StringUtils.isNotBlank(encryptedSalt)) {
                String decryptedSalt = EasyShareEncryptionUtil.decode(encryptedSalt,
                        EasyShareEncryptionUtil.getDefaultSalt());
                decryptedAccessCode = EasyShareEncryptionUtil.decode(encryptedAccessCode, decryptedSalt);
                decryptedJSessionId = EasyShareEncryptionUtil.decode(encryptedJSessionId, decryptedSalt);
            }
            return (Response) buildUnrestrictedRunnerStream(decryptedJSessionId, decryptedAccessCode,
                    shareId).runUnrestricted(fileId);
        } catch (NuxeoException e) {
            // Error occured during decoding process of salt, accessCode or jSessionId from URL
            return Response.serverError().status(Response.Status.FORBIDDEN).build();
        }
    }

    @POST
    @Path("{shareId}/{fileId}/{fileName}")
    public Response postFileStream(@PathParam("shareId") final String shareId, @PathParam("fileId") String fileId,
            @FormParam("accessCode") final String accessCode) throws NuxeoException {
        return (Response) buildUnrestrictedRunnerStream(getRequestJSESSIONID(), accessCode, shareId).runUnrestricted(
                fileId);
    }

    public String getFileName(DocumentModel doc) throws NuxeoException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder != null && blobHolder.getBlob() != null) {
            return blobHolder.getBlob().getFilename();
        }
        return doc.getName();
    }

    protected String getRequestJSESSIONID() {
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader == null || cookieHeader.length() == 0 || !cookieHeader.contains("JSESSIONID")) {
            return null;
        }

        if (cookieHeader.contains(";")) {
            String pattern1 = "JSESSIONID=";
            String pattern2 = ";";
            Pattern p = Pattern.compile(Pattern.quote(pattern1) + "(.*?)" + Pattern.quote(pattern2));
            Matcher m = p.matcher(cookieHeader);
            if (m.find()) {
                return m.group(1);
            }
        } else {
            return cookieHeader.replace("JSESSIONID=", "");
        }
        return null;
    }

    public void sendNotification(String notification, DocumentModel docShare, Map<String, Object> mail) {

        Boolean hasNotification = docShare.getProperty("eshare:hasNotification").getValue(Boolean.class);

        if (hasNotification) {
            // Email notification
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
                    Notification notif = NotificationServiceHelper.getNotificationService()
                                                                  .getNotificationByName(notification);

                    if (notif.getSubjectTemplate() != null) {
                        mailProps.put(NotificationConstants.SUBJECT_TEMPLATE_KEY, notif.getSubjectTemplate());
                    }

                    mailProps.put(NotificationConstants.SUBJECT_KEY,
                            NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix() + " "
                                    + notif.getSubject());
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
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
