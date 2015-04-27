package org.nuxeo.ecm.core.io.marshallers.csv.document;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.csv.DefaultListCsvWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelListCsvWriter extends DefaultListCsvWriter<DocumentModel> {

    public DocumentModelListCsvWriter() {
        super(DocumentModel.class);
    }

}
