package org.nuxeo.ecm.platform.audit.service;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;

public class ExtendedInfoContext extends ExpressionContext {

    ExtendedInfoContext(DocumentMessage message, DocumentModel model, NuxeoPrincipal principal) {
        super();
        this.message = message;
        this.model = model;
        this.principal = principal;
    }
 
    private final DocumentMessage message;
    private final DocumentModel model;
    private final NuxeoPrincipal principal;
    
    public void bindVariables(ExpressionEvaluator evaluator) {
        evaluator.bindValue(this, "document", model);
        evaluator.bindValue(this, "message", message);
        evaluator.bindValue(this, "principal", principal);
    }
    
}
