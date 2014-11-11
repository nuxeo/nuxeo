package org.nuxeo.ecm.automation.core;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;


/**
 * Dummy class to test operation registration replacement
 *
 * @author <a href="mailto:bs@nuxeo.com">Thierry Martins</a>
 */
@Operation(id = DummyCreateDocument.ID, category = Constants.CAT_DOCUMENT, label = "Create", description = "Dummy class")
public class DummyCreateDocument {

    public static final String ID = "Document.Create";
    
    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        return doc;
        
    }
    
    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) throws Exception {
        return null;
    }
}
