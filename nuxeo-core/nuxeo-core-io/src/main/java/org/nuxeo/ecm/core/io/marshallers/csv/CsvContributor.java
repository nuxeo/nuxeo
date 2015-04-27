package org.nuxeo.ecm.core.io.marshallers.csv;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.io.registry.MarshallingException;

public abstract class CsvContributor<EntityType> {

    private Class<EntityType> clazz;

    public CsvContributor(Class<EntityType> clazz) {
        this.clazz = clazz;
    }

    public abstract List<String> getHeaders();

    public final Map<String, String> getValues(Object object) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        boolean hasValue = clazz.isAssignableFrom(object.getClass());
        List<String> values = null;
        if (hasValue) {
            values = getOrderedValues(clazz.cast(object));
        }
        List<String> headers = getHeaders();
        if (hasValue) {
            if (headers.size() != values.size()) {
                throw new MarshallingException("Expected as many headers as values: headers=" + headers.toString()
                        + " - values=" + values.toString());
            }
        }
        for (int i = 0; i < headers.size(); i++) {
            if (hasValue) {
                result.put(headers.get(i), values.get(i));
            } else {
                result.put(headers.get(i), null);
            }
        }
        return result;
    }

    public abstract List<String> getOrderedValues(EntityType entity);

}
