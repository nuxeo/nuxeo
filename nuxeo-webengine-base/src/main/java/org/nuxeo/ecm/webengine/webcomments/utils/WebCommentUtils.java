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
import static org.nuxeo.ecm.webengine.webcomments.utils.WebCommentsConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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
            DocumentModel doc, String permission) throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        for (DocumentModel documentModel : parents) {
            if (documentModel.getType().equals(WORKSPACE)) {
                // TODO: test for groups eg. administrators
                String[] moderators = documentModel.getACP().listUsernamesForPermission(
                        permission);
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
        return getUsersWithPermission(session, doc, PERMISSION_MODERATE).size() >= 1 ? true : false;
    }

    /**
     * @return true if the current user is between moderators
     * @throws Exception
     */
    public static boolean isModeratedByCurrentUser(CoreSession session,
            DocumentModel doc) throws Exception {
        ArrayList<String> moderators = getUsersWithPermission(session, doc, PERMISSION_MODERATE);
        if (moderators.contains(session.getPrincipal().getName())) {
            return true;
        }

        return false;
    }

    /**
     * @return true if the current user has comment permission on this document
     * @throws Exception
     */
    public static boolean currentUserHasCommentPermision(CoreSession session,
            DocumentModel doc) throws Exception {
        ArrayList<String> users = getUsersWithPermission(session, doc, PERMISSION_COMMENT);
        if (users.contains(session.getPrincipal().getName())) {
            return true;
        }

        return false;

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
    /**
     * Get all the moderators for the corresponding workspace
     * */
    public static ArrayList<String> getModerators(CoreSession session,
            DocumentModel doc) throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        for (DocumentModel documentModel : parents) {
            if (documentModel.getType().equals("Workspace")) {
                // TO DO: test for groups eg. administrators
                String[] moderators = documentModel.getACP().listUsernamesForPermission(
                        "Moderate");
                return new ArrayList<String>(Arrays.asList(moderators));
            }

        }
        return new ArrayList<String>();
    }

}
