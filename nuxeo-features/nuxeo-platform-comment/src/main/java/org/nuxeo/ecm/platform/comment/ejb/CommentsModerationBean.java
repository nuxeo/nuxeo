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
