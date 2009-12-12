package org.nuxeo.ecm.webdav;

import net.java.dev.webdav.jaxrs.xml.elements.PropFind;
import net.java.dev.webdav.jaxrs.xml.elements.PropertyUpdate;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

public class JaxbTest extends Assert {

    @Test
    public void testPropFind() throws Exception {
        testFile("propfind1.xml", PropFind.class);
        testFile("propfind2.xml", PropFind.class);
        testFile("propfind3.xml", PropFind.class);
        testFile("propertyupdate1.xml", PropertyUpdate.class);
    }

    private void testFile(String name, Class<?> class_) throws Exception {
        JAXBContext jc = Util.getJaxbContext();
        Unmarshaller u = jc.createUnmarshaller();

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("xmlsamples/" + name);
        assertNotNull(is);
        Object o = u.unmarshal(is);
        System.out.println(o);
        assertEquals(o.getClass(), class_);        
    }

}
