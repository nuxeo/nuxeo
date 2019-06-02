package org.nuxeo.template.serializer.service;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.template.serializer.executors.Serializer;

/**
 * @Since 11.1
 */
public class SerializerServiceImpl extends DefaultComponent implements SerializerService {

    private static final String EXTENSION_POINT_NAME = "serializers";
    private static final String DEFAULT_SERIALIZER_NAME = "default";

    @Override
    public Serializer getSerializer(String id) {
        SerializerContribution contrib = getDescriptor(EXTENSION_POINT_NAME, id);

        if (contrib == null) {
            contrib = getDescriptor(EXTENSION_POINT_NAME, DEFAULT_SERIALIZER_NAME);
            if (contrib == null) {
                throw new NuxeoException("UnknownSerializer named " + id);
            }
        }

        return contrib.getImplementation();
    }
}
