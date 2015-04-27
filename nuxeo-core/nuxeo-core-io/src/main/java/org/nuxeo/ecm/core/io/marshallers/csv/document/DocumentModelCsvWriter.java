package org.nuxeo.ecm.core.io.marshallers.csv.document;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.csv.AbstractCsvWriter;
import org.nuxeo.ecm.core.io.marshallers.csv.CsvContributor;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;

@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelCsvWriter extends AbstractCsvWriter<DocumentModel> {

    @Inject
    SchemaManager schemaManager;

    @Override
    public List<CsvContributor<?>> getColumns() {
        List<CsvContributor<?>> generators = new ArrayList<CsvContributor<?>>();
        // generators.add(BaseDocumentModelCsvContributor.INSTANCE);
        generators.add(new PrefixedCsvContributor<DocumentModel, DocumentModel>("properties.", DocumentModel.class,
                new PropertiesCsvContributor(ctx, schemaManager)) {
            @Override
            public DocumentModel getRelatedEntity(DocumentModel entity) {
                return entity;
            }
        });
        return generators;
    }

}
