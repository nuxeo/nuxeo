package org.nuxeo.ecm.core.io.marshallers.csv.document;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.io.marshallers.csv.CsvContributor;

public abstract class PrefixedCsvContributor<EntityType, DelegatedType> extends CsvContributor<EntityType> {

    private final CsvContributor<DelegatedType> delegate;

    private List<String> headers;

    public PrefixedCsvContributor(String prefix, Class<EntityType> clazz, CsvContributor<DelegatedType> delegate) {
        super(clazz);
        this.delegate = delegate;
        headers = new ArrayList<String>();
        for (String header : delegate.getHeaders()) {
            headers.add(prefix + header);
        }
    }

    @Override
    public List<String> getHeaders() {
        return headers;
    }

    @Override
    public List<String> getOrderedValues(EntityType entity) {
        return delegate.getOrderedValues(getRelatedEntity(entity));
    }

    public abstract DelegatedType getRelatedEntity(EntityType entity);

}
