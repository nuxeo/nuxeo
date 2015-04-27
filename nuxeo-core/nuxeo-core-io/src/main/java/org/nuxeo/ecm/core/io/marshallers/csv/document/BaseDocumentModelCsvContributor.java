package org.nuxeo.ecm.core.io.marshallers.csv.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.csv.CsvContributor;
import org.nuxeo.ecm.core.schema.utils.DateParser;

public class BaseDocumentModelCsvContributor extends CsvContributor<DocumentModel> {

    static final BaseDocumentModelCsvContributor INSTANCE = new BaseDocumentModelCsvContributor();

    private static final List<String> HEADERS = Arrays.asList("repository", "uid", "path", "type", "state",
            "parentRef", "isCheckedOut", "changeToken", "title", "lastModified");

    private BaseDocumentModelCsvContributor() {
        super(DocumentModel.class);
    }

    @Override
    public List<String> getHeaders() {
        return HEADERS;
    }

    @Override
    public List<String> getOrderedValues(DocumentModel doc) {
        List<String> result = new ArrayList<String>(getHeaders().size());
        result.add(doc.getRepositoryName());
        result.add(doc.getId());
        result.add(doc.getPathAsString());
        result.add(doc.getType());
        result.add(doc.getRef() != null ? doc.getCurrentLifeCycleState() : null);
        result.add(doc.getParentRef() != null ? doc.getParentRef().toString() : null);
        result.add(Boolean.toString(doc.isCheckedOut()));
        result.add(doc.getChangeToken());
        result.add(doc.getTitle());
        if (doc.hasSchema("dublincore")) {
            Calendar cal = (Calendar) doc.getPropertyValue("dc:modified");
            if (cal != null) {
                result.add(DateParser.formatW3CDateTime(cal.getTime()));
            } else {
                result.add(null);
            }
        } else {
            result.add(null);
        }
        return result;
    }

}