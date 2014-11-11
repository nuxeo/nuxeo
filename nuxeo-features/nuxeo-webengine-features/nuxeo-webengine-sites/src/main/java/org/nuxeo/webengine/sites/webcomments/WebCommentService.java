/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.webengine.sites.webcomments;

import java.util.ArrayList;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.CommentService;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Web Comment Service - extension of base's Comment Service with specifics
 * of sites module.
 *
 * @author rux
 */
@WebAdapter(name = "webcomments", type = "WebCommentService", targetType = "Document", targetFacets = {"Commentable"})
public class WebCommentService extends CommentService {

    @Override
    protected DocumentModel createCommentDocument(CoreSession session,
            DocumentModel target, DocumentModel comment) throws Exception {
        DocumentModel site = SiteUtils.getFirstWebSiteParent(session, target);
        if (site == null) {
            return super.createCommentDocument(session, target, comment);
        } else {
            return getCommentManager().createLocatedComment(target, comment,
                    site.getPathAsString());
        }
    }

    @Override
    protected void publishComment(CoreSession session, DocumentModel target,
            DocumentModel comment) throws Exception {
        //CommentsModerationService commentsModerationService = getCommentsModerationService();
        if (SiteUtils.isCurrentModerated(session, target)
                && !SiteUtils.isModeratedByCurrentUser(session, target)) {
            // if current page is moderated
            // get all moderators
            ArrayList<String> moderators = SiteUtils.getModerators(
                    session, target);
            // start the moderation process
            getCommentsModerationService().startModeration(session, target,
                    comment.getId(), moderators);
        } else {
            // simply publish the comment
            super.publishComment(session, target, comment);
        }
        session.save();
    }

}
