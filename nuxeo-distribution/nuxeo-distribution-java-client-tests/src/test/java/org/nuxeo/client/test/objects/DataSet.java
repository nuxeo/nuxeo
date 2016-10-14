package org.nuxeo.client.test.objects;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.client.api.objects.Document;

/**
 * @since 1.0
 */
public class DataSet extends Document {

    protected List<Field> fields = new ArrayList<>();

    public DataSet(String file, String dataSet) {
        super(file, dataSet);
    }

    public DataSet(Document document) {
        super(document);
        for (Object field : (List) document.getPropertyValue("ds:fields")) {
            Field field1 = new Field(field);
            fields.add(field1);
        }
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }
}
