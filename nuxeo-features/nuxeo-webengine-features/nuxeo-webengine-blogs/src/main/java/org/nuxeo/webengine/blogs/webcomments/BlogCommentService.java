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

package org.nuxeo.webengine.blogs.webcomments;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.webengine.sites.webcomments.WebCommentService;

/**
 * Blog Comment Service - extension of base's WebCommentService with specifics
 * of sites module.
 *
 * @author rux
 */
@WebAdapter(name = "webcomments", type = "BlogCommentService", targetType = "Document", targetFacets = { "Commentable" })
public class BlogCommentService extends WebCommentService {

    @Override
    protected DocumentModel createCommentDocument(CoreSession session,
            DocumentModel target, DocumentModel comment) throws Exception {
        return super.createCommentDocument(session, target, comment);
    }

    @Override
    protected void publishComment(CoreSession session, DocumentModel target,
            DocumentModel comment) throws Exception {
        super.publishComment(session, target, comment);
    }

}
