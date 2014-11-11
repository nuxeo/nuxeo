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
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.ejb;

import java.util.ArrayList;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.workflow.services.CommentsModerationService;
import org.nuxeo.runtime.api.Framework;

@Stateless
@Remote(CommentsModerationService.class)
@Local(CommentsModerationService.class)
public class CommentsModerationBean implements CommentsModerationService {


    protected CommentsModerationService getCommentsModerationService() {
        return Framework.getLocalService(CommentsModerationService.class);
    }


    public void approveComent(CoreSession session, DocumentModel document,
            String commentID) throws ClientException {
        getCommentsModerationService().approveComent(session, document, commentID);
    }

    public void publishComment(CoreSession session, DocumentModel comment)
            throws ClientException {
        getCommentsModerationService().publishComment(session, comment);

    }

    public void rejectComment(CoreSession session, DocumentModel document,
            String commentID) throws ClientException {
        getCommentsModerationService().rejectComment(session, document, commentID);

    }

    public void startModeration(CoreSession session, DocumentModel document,
            String commentID, ArrayList<String> moderators)
            throws ClientException {
        getCommentsModerationService().startModeration(session, document, commentID, moderators);

    }

}
