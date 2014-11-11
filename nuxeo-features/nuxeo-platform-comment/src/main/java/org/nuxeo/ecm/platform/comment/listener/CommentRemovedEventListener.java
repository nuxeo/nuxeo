package org.nuxeo.ecm.platform.comment.listener;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;

public class CommentRemovedEventListener extends AbstractCommentListener
        implements EventListener {

    private static final Log log = LogFactory.getLog(CommentRemovedEventListener.class);

    @Override
    protected void doProcess(CoreSession coreSession,
            RelationManager relationManager, CommentServiceConfig config,
            DocumentModel docMessage) throws Exception {
        log.debug("Processing relations cleanup on Comment removal");
        if ("Comment".equals(docMessage.getDocumentType().getName()))
            onCommentRemoved(relationManager, config, docMessage);
    }

    private void onCommentRemoved(RelationManager relationManager,
            CommentServiceConfig config, DocumentModel docModel)
            throws ClientException {
        Resource commentRes = relationManager.getResource(
                config.commentNamespace, docModel);
        if (commentRes == null) {
            log.error("Could not adapt document model to relation resource; "
                    + "check the service relation adapters configuration");
            return;
        }
        Statement pattern = new StatementImpl(commentRes, null, null);
        List<Statement> statementList = relationManager.getStatements(
                config.graphName, pattern);
        relationManager.remove(config.graphName, statementList);
    }

}
