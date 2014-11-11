/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.wiki.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.wiki.WikiTypes;
import org.nuxeo.ecm.wiki.relation.RelationConstants;
import org.nuxeo.ecm.wiki.relation.RelationHelper;

public class WikiListener extends AbstractEventListener implements AsynchronousEventListener, WikiTypes {
    private static final Log log = LogFactory.getLog(WikiListener.class);


    public void testRelations(DocumentModel model) throws Exception{
        DocumentRef ref1 = new PathRef("/default-domain/workspaces/ws/f1");
        DocumentRef ref2 = new PathRef("/default-domain/workspaces/ws/f2");
        DocumentRef ref3 = new PathRef("/default-domain/workspaces/ws/f3");


        CoreSession session = CoreInstance.getInstance().getSession(model.getSessionId());
        RelationManager relationManager = RelationHelper.getRelationManager();

        DocumentModel doc1 = session.getDocument(ref1);
        DocumentModel doc2 = session.getDocument(ref2);
        DocumentModel doc3 = session.getDocument(ref3);


        Resource res1 = RelationHelper.getDocumentResource(doc1);
        Resource res2 = RelationHelper.getDocumentResource(doc2);
        Resource res3 = RelationHelper.getDocumentResource(doc3);

        List<Statement> stmts = new ArrayList<Statement>();
        Statement stmt = new StatementImpl(res1,
                RelationConstants.HAS_LINK_TO, res2);
        stmts.add(stmt);
        stmt = new StatementImpl(res1,
                RelationConstants.HAS_LINK_TO, res3);
        stmts.add(stmt);


        stmt = new StatementImpl(res1,
                RelationConstants.HAS_LINK_TO, new LiteralImpl("mumu"));
        stmts.add(stmt);

        relationManager.add(RelationConstants.GRAPH_NAME, stmts);






        // retrive

        DocumentModelList list = RelationHelper.getDocumentsWithLinksTo(doc2);


//        System.out.println("debug");
        DocumentModelList list2 = RelationHelper.getDocumentsWithLinksFrom(doc1);
//        System.out.println("debug");

        DocumentModelList list3 = RelationHelper.getDocumentsWithLinksTo("mumu", doc1.getSessionId());
        System.out.println();

    }


    public void handleEvent(CoreEvent coreEvent) throws Exception  {
        /* TODO: work in progress
         * this is not working yet  
         */
        if (true) return;
        
        Object source = coreEvent.getSource();

        if (source instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) source;
            testRelations(doc);
            final String type = doc.getType();
            String eventId = coreEvent.getEventId();
            if ( WIKIPAGE.equals(type) && DOCUMENT_UPDATED.equals(eventId)){
                RelationHelper.updateRelations(doc);
            }
        }

    }

}
