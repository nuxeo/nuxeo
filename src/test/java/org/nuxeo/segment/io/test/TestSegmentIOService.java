package org.nuxeo.segment.io.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.segment.io.SegmentIO;
import org.nuxeo.segment.io.SegmentIOComponent;

@Deploy({ "org.nuxeo.segmentio.connector"})
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestSegmentIOService {

    @Test
    public void checkDeploy() {

        SegmentIOComponent component = (SegmentIOComponent) Framework.getRuntime().getComponent(SegmentIOComponent.class.getName());
        Assert.assertNotNull(component);

        SegmentIO service = Framework.getLocalService(SegmentIO.class);
        Assert.assertNotNull(service);

    }

}
