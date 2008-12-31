package org.nuxeo.runtime.management;

import javax.management.modelmbean.ModelMBeanInfo;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.jsesoft.mmbi.PureResource;
import org.nuxeo.runtime.management.inspector.ModelMBeanInfoFactory;

public class TestResourceInstrumentation extends TestCase {

    ModelMBeanInfoFactory instrumentatorFactory = new ModelMBeanInfoFactory();

    public void testInstrumentation() throws Exception {
        ModelMBeanInfo info = instrumentatorFactory.getModelMBeanInfo(PureResource.class);
        Assert.assertEquals(info.getClassName(),
                PureResource.class.getSimpleName());
        // TODO assert operations, attributes etc
    }

}
