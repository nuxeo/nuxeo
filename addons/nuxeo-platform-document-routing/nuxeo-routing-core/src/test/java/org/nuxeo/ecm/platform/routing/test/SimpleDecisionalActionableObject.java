package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.routing.api.ActionableObject;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;

/**
 * <!-- @deprecated since 5.9.2 - Use only routes of type 'graph' -->
 *
 */
@Deprecated
public class SimpleDecisionalActionableObject implements ActionableObject {

    String stepDocId;

    public SimpleDecisionalActionableObject(String stepDocId) {
        this.stepDocId = stepDocId;
    }

    @Override
    public DocumentModelList getAttachedDocuments(CoreSession session) {
        return new DocumentModelListImpl();
    }

    @Override
    public DocumentRouteStep getDocumentRouteStep(CoreSession session) {
        try {
            DocumentModel docStep = session.getDocument(new IdRef(stepDocId));
            return docStep.getAdapter(DocumentRouteStep.class);
        } catch (ClientException e) {
            return null;
        }

    }

    @Override
    public String getRefuseOperationChainId() {
        return "simpleRefuse";
    }

    @Override
    public String getValidateOperationChainId() {
        return "decideNextStepAndSimpleValidate";
    }

}
