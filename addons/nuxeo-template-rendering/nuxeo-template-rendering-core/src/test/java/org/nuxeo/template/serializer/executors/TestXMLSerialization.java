package org.nuxeo.template.serializer.executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.serializer.service.SerializerService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class})
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service.xml")
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service-contribution.xml")
public class TestXMLSerialization {

    private static final String XML_INTRO = "<nxdt:templateParams xmlns:nxdt=\"http://www.nuxeo.org/DocumentTemplate\"/>";
    private static final String XML_INTRO_OPEN = "<nxdt:templateParams xmlns:nxdt=\"http://www.nuxeo.org/DocumentTemplate\">";
    private static final String XML_INTRO_CLOSE = "</nxdt:templateParams>";

    public Serializer serializer;

    @Before
    public void setup() {
        serializer = Framework.getService(SerializerService.class).getSerializer("xml");
    }

    @Test
    public void whenValueIsEmpty_shouldReturnEmptyXML() {
        List<TemplateInput> params = new ArrayList<>();
        String xml = serializer.doSerialization(params);
        assertXMLEquals("", xml);
    }

    @Test
    public void whenValueHasOneString_shouldReturnListWithOneString() {
        List<TemplateInput> params = new ArrayList<>();
        params.add(new TemplateInput("field1", "Value1"));
        String xml = serializer.doSerialization(params);
        assertXMLEquals("<nxdt:field name=\"field1\" type=\"String\" value=\"Value1\"/>", xml);
    }

    @Test
    public void whenValueHasOneDate_shouldReturnListWithOneDate() {
        List<TemplateInput> params = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(2017, Calendar.JULY, 14, 13, 14, 15); // after 12 to check hour format
        cal.set(Calendar.MILLISECOND, 678);
        TemplateInput input = new TemplateInput("field2", new Date(cal.getTimeInMillis()));
        params.add(input);
        String xml = serializer.doSerialization(params);
        assertXMLEquals("<nxdt:field name=\"field2\" type=\"Date\" value=\"2017-07-14 13:14:15.678\"/>", xml);
    }

    @Test
    public void whenValueHasOneBoolean_shouldReturnListWithOneBoolean() {
        List<TemplateInput> params = new ArrayList<>();
        TemplateInput input = new TemplateInput("field3", Boolean.TRUE);
        params.add(input);
        String xml = serializer.doSerialization(params);
        assertXMLEquals("<nxdt:field name=\"field3\" type=\"Boolean\" value=\"true\"/>", xml);
    }

    @Test
    public void whenValueHasOneDocumentProperty_shouldReturnListWithASourceDescription() {
        List<TemplateInput> params = new ArrayList<>();
        TemplateInput input = new TemplateInput("field4");
        input.setType(InputType.DocumentProperty);
        input.setSource("dc:description");
        params.add(input);
        String xml = serializer.doSerialization(params);
        assertXMLEquals("<nxdt:field name=\"field4\" type=\"source\" source=\"dc:description\"/>", xml);
    }

    @Test
    public void whenValueHasOnePictureProperty_shouldReturnListWithThePictureValue() {
        List<TemplateInput> params = new ArrayList<>();
        TemplateInput input = new TemplateInput("field5");
        input.setSource("file:content");
        input.setType(InputType.PictureProperty);
        params.add(input);
        String xml = serializer.doSerialization(params);
        assertXMLEquals("<nxdt:field name=\"field5\" type=\"picture\" source=\"file:content\"/>", xml);
    }

    @Test
    public void whenValueHasOneContent_shouldReturnListWithTheSourceValue() {
        List<TemplateInput> params = new ArrayList<>();
        TemplateInput input = new TemplateInput("field6");
        input.setSource("note:note");
        input.setType(InputType.Content);
        params.add(input);
        String xml = serializer.doSerialization(params);
        assertXMLEquals("<nxdt:field name=\"field6\" type=\"content\" source=\"note:note\"/>", xml);
    }

    private void assertXMLEquals(String expected, String actual) {
        if (expected == null || expected.trim().isEmpty()) {
            assertEquals(XML_INTRO, actual);
        } else {
            assertEquals(XML_INTRO_OPEN + expected + XML_INTRO_CLOSE, actual);
        }
    }

}
