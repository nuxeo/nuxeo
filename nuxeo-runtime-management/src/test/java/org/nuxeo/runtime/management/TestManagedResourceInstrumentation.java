package org.nuxeo.runtime.management;

import javax.management.modelmbean.ModelMBeanInfo;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.jsesoft.mmbi.PureResource;
import org.nuxeo.runtime.management.inspector.ModelMBeanInfoInstrumentorFactory;

public class TestManagedResourceInstrumentation extends TestCase {

    ModelMBeanInfoInstrumentorFactory instrumentatorFactory = new ModelMBeanInfoInstrumentorFactory();

    public void testInstrumentation() throws Exception {
        ModelMBeanInfo info = instrumentatorFactory.getModelMBeanInfo(PureResource.class);
        Assert.assertEquals(info.getClassName(),
                PureResource.class.getSimpleName());
        // TODO assert operations, attributes etc
    }

}
