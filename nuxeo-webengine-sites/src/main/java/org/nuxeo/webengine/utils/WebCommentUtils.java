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


package org.nuxeo.webengine.utils;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.PERMISSION_MODERATE;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.PERMISSION_COMMENT;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.WORKSPACE;

import java.util.ArrayList;
import java.util.Arrays;
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
     * Get all the users with a given permission for the corresponding workspace
     * */
    public static ArrayList<String> getUsersWithPermission(CoreSession session,
            DocumentModel doc, Set<String> permissions) throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        for (DocumentModel documentModel : parents) {
            if (documentModel.getType().equals(WORKSPACE)) {
                // TODO: test for groups eg. administrators
                String[] moderators = documentModel.getACP().listUsernamesForAnyPermission(permissions);
                return new ArrayList<String>(Arrays.asList(moderators));
            }
        }
        return new ArrayList<String>();
    }

    /**
     * @return true if the corresponding workspace is moderated
     * @throws Exception
     */
    public static boolean isCurrentModerated(CoreSession session,
            DocumentModel doc) throws Exception {
        Set<String> moderatePermissions = new HashSet<String>();
        SecurityService securityService = getSecurityService();
        moderatePermissions.addAll(Arrays.asList(securityService.getPermissionsToCheck(PERMISSION_MODERATE)));
        return getUsersWithPermission(session, doc, moderatePermissions).size() >= 1 ? true
                : false;
    }

    /**
     * @return true if the current user is between moderators
     * @throws Exception
     */
    public static boolean isModeratedByCurrentUser(CoreSession session,
            DocumentModel doc) throws Exception {
        return session.hasPermission(doc.getRef(), PERMISSION_MODERATE);
    }

    /**
     * @return true if the current user has comment permission on this document
     * @throws Exception
     */
    public static boolean currentUserHasCommentPermision(CoreSession session,
            DocumentModel doc) throws Exception {
        return session.hasPermission(doc.getRef(), PERMISSION_COMMENT);
    }

   /**
    * @return true if the current user is an Administrator
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
    
}
