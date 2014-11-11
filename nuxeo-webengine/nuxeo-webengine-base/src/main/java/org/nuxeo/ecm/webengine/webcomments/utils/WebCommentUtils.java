/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */


package org.nuxeo.ecm.webengine.webcomments.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;


/**
 * Utilities for Comments implementation into WebEngine
 * @author mariana
 *
 */
public class WebCommentUtils {

    /**
     * @return  all users with a given permission for the corresponding workspace
     * @throws Exception
     * */
    public static ArrayList<String> getUsersWithPermission(CoreSession session,
            DocumentModel doc, Set<String> permissions) throws Exception {
        DocumentModel parentWorkspace = getFirstWorkspaceParent(session, doc);
        if (parentWorkspace != null) {
            String[] moderators = parentWorkspace.getACP().listUsernamesForAnyPermission(permissions);
            return new ArrayList<String>(Arrays.asList(moderators));
        }

        return new ArrayList<String>();
    }

    /**
     * @return true if the corresponding workspace is moderated : there is at least one
     * user with moderate permission on this workspace and the moderationType is a priori
     * @throws Exception
     */
    public static boolean isCurrentModerated(CoreSession session,
            DocumentModel doc) throws Exception {
        Set<String> moderatePermissions = new HashSet<String>();
        SecurityService securityService = getSecurityService();
        moderatePermissions.addAll(Arrays.asList(securityService.getPermissionsToCheck(WebCommentsConstants.PERMISSION_MODERATE)));
        return getUsersWithPermission(session, doc, moderatePermissions).size() >= 1 ? (true && WebCommentUtils.getModerationType(
                session, doc).equals(WebCommentsConstants.MODERATION_APRIORI) == true)
                : false;
    }

    /**
     * @return true if the current user is between moderators
     * @throws Exception
     */
    public static boolean isModeratedByCurrentUser(CoreSession session,
            DocumentModel doc) throws Exception {
        return session.hasPermission(doc.getRef(), WebCommentsConstants.PERMISSION_MODERATE);
    }

    /**
     * @return true if the current user has comment permission on this document
     * @throws Exception
     */
    public static boolean currentUserHasCommentPermision(CoreSession session,
            DocumentModel doc) throws Exception {
        return session.hasPermission(doc.getRef(), WebCommentsConstants.PERMISSION_COMMENT);
    }

   /**
    * @return true if the current user is an Administrator
    * @throws Exception
    * */
    public static boolean currentUserIsAdministaror(CoreSession session) {
        return ((NuxeoPrincipal) session.getPrincipal()).isAdministrator();
    }

    public static CommentManager getCommentManager() throws Exception {
        CommentManager commentManager = Framework.getLocalService(CommentManager.class);
        if (commentManager == null) {
            throw new Exception("unable to get commentManager");
        }
        return commentManager;
    }

    public static UserManager getUserManager() throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);
        if (userManager == null) {
            throw new Exception("unable to get userManager");
        }
        return userManager;
    }

    public static SecurityService getSecurityService(){
        return NXCore.getSecurityService();
    }

    /**
     * This method is used to retrieve the <b>WebPage</b> where this
     * <b>WebComment</b> was published
     *
     * @param comment
     * @return the <b>WebPage</b>
     * @throws Exception
     */
    public static DocumentModel getPageForComment(DocumentModel comment)
            throws Exception {
        CommentManager commentManager = WebCommentUtils.getCommentManager();
        List<DocumentModel> list = commentManager.getDocumentsForComment(comment);
        if (list.size() != 0) {
            DocumentModel page = list.get(0);
            if (page.getCurrentLifeCycleState().equals("deleted") == false){
                return page;
            }
                
        }
        return null;
    }
    
    /**
     * @return all the moderators for the corresponding workspace
     * @throws Exception
     * */
    public static ArrayList<String> getModerators(CoreSession session,
            DocumentModel doc) throws Exception {
        Set<String> moderatePermissions = new HashSet<String>();
        SecurityService securityService = getSecurityService();
        moderatePermissions.addAll(Arrays.asList(securityService.getPermissionsToCheck(WebCommentsConstants.PERMISSION_MODERATE)));
        return getUsersWithPermission(session, doc, moderatePermissions);
    }
    
    /**
     * @return the moderation type for the corresponding workspace ; default is aposteriori
     * @throws Exception
     * */
    public static String getModerationType(CoreSession session,
            DocumentModel doc) throws Exception {
        DocumentModel workspaceParent = getFirstWorkspaceParent(session, doc);
        if (workspaceParent != null) {
            return workspaceParent.getProperty("webcontainer", "moderationType").toString();
        }
        return WebCommentsConstants.MODERATION_APOSTERIORI;
    }
    

    private static DocumentModel getFirstWorkspaceParent(CoreSession session,
            DocumentModel docPage) throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(docPage.getRef());
        Collections.reverse(parents);
        for (DocumentModel currentDocumentModel : parents) {
            if (WebCommentsConstants.WORKSPACE.equals(currentDocumentModel.getType())
                    && currentDocumentModel.hasFacet("WebView")) {
                return currentDocumentModel;
            }
        }
        return null;
    }

}
