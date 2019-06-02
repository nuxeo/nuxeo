
package org.nuxeo.template.serializer.service;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.serializer.executors.Serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class})
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service.xml")
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service-contribution.xml")
public class TestSerializerService {

    @Inject
    protected SerializerService serializerService;

    @Test
    public void serviceShouldThere() {
        assertNotNull(serializerService);
    }

    @Test
    public void defaultSerializerShouldBeTheXMLOne() {
        Serializer serializer = serializerService.getSerializer(null);
        assertNotNull(serializer);
        assertEquals("XMLSerializer", serializer.getClass().getSimpleName());
    }

    @Test
    public void whenRequestXMLSerializer_shouldReturnXMLSerializer() {
        Serializer serializer = serializerService.getSerializer("xml");
        assertNotNull(serializer);
        assertEquals("XMLSerializer", serializer.getClass().getSimpleName());
    }

}
