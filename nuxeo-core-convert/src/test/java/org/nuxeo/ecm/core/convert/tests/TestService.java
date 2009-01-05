package org.nuxeo.ecm.core.convert.tests;

import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.extension.ChainedConverter;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

     @Override
     protected void setUp() throws Exception {
            super.setUp();
            deployBundle("org.nuxeo.ecm.core.api");
            deployBundle("org.nuxeo.ecm.core.convert.api");
            deployBundle("org.nuxeo.ecm.core.convert");
     }


     public void testServiceRegistration() {

         ConversionService cs = Framework.getLocalService(ConversionService.class);
         assertNotNull(cs);
     }


     public void testServiceContrib() throws Exception{

         deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib1.xml");
         ConversionService cs = Framework.getLocalService(ConversionService.class);

         Converter cv1 =  ConversionServiceImpl.getConverter("dummy1");
         assertNotNull(cv1);

         ConverterDescriptor desc1 = ConversionServiceImpl.getConverterDesciptor("dummy1");
         assertNotNull(desc1);

         assertEquals("test/me", desc1.getDestinationMimeType());
         assertTrue(desc1.getSourceMimeTypes().size()==2);
         assertTrue(desc1.getSourceMimeTypes().contains("text/plain"));
         assertTrue(desc1.getSourceMimeTypes().contains("text/xml"));

     }

     public void testConverterLookup() throws Exception {

         deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib1.xml");
         ConversionService cs = Framework.getLocalService(ConversionService.class);

         String converterName = cs.getConverterName("text/plain", "test/me");
         assertEquals("dummy1", converterName);

         converterName = cs.getConverterName("text/plain2", "test/me");
         assertNull(converterName);

         deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib2.xml");

         converterName = cs.getConverterName("test/me", "foo/bar");
         assertEquals("dummy2", converterName);

         converterName = cs.getConverterName("text/plain", "foo/bar");
         assertEquals("dummyChain", converterName);

         Converter cv =  ConversionServiceImpl.getConverter("dummyChain");
         assertNotNull(cv);
         boolean isChain = false;
         if (cv instanceof ChainedConverter) {
             isChain=true;
         }
         assertTrue(isChain);
     }


     public void testServiceConfig() throws Exception{

         deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/convert-service-config-test.xml");
         ConversionService cs = Framework.getLocalService(ConversionService.class);

         assertEquals(12,ConversionServiceImpl.getGCIntervalInMinutes());
         assertEquals(132,ConversionServiceImpl.getMaxCacheSizeInKB());
         assertFalse(ConversionServiceImpl.isCacheEnabled());

     }

}
