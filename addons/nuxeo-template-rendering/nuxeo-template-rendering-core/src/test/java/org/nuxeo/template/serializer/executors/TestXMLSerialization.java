/*
 * (C) Copyright 2019 Qastia (http://www.qastia.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Benjamin JALON
 *
 */

package org.nuxeo.template.serializer.executors;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.template.api.InputType.BooleanValue;
import static org.nuxeo.template.api.InputType.Content;
import static org.nuxeo.template.api.InputType.DateValue;
import static org.nuxeo.template.api.InputType.DocumentProperty;
import static org.nuxeo.template.api.InputType.ListValue;
import static org.nuxeo.template.api.InputType.MapValue;
import static org.nuxeo.template.api.InputType.PictureProperty;
import static org.nuxeo.template.api.InputType.StringValue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.serializer.service.TemplateSerializerService;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service.xml")
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service-contribution.xml")
public class TestXMLSerialization {

    private static final String XML_INTRO = "<nxdt:templateParams xmlns:nxdt=\"http://www.nuxeo.org/DocumentTemplate\"/>";

    private static final String XML_INTRO_OPEN = "<nxdt:templateParams xmlns:nxdt=\"http://www.nuxeo.org/DocumentTemplate\">";

    private static final String XML_INTRO_CLOSE = "</nxdt:templateParams>";

    @Inject
    protected TemplateSerializerService templateSerializerService;

    @Test
    public void whenValueIsEmpty_shouldReturnEmptyXML() {
        List<TemplateInput> params = new ArrayList<>();

        String xml = templateSerializerService.serializeXML(params);

        assertXMLEquals("", xml);
    }

    @Test
    public void whenValueHasOneString_shouldReturnListWithOneString() {
        List<TemplateInput> params = new ArrayList<>();
        params.add(TemplateInput.factory("field1", StringValue, "Value1"));

        String xml = templateSerializerService.serializeXML(params);

        assertXMLEquals("<nxdt:field name=\"field1\" type=\"String\" value=\"Value1\"/>", xml);
    }

    @Test
    public void whenValueHasOneDate_shouldReturnListWithOneDate() {
        List<TemplateInput> params = new ArrayList<>();
        Date date = createDate();
        TemplateInput input = TemplateInput.factory("field2", DateValue, date);
        params.add(input);

        String xml = templateSerializerService.serializeXML(params);

        assertXMLEquals("<nxdt:field name=\"field2\" type=\"Date\" value=\"2017-07-14 13:14:15.678\"/>", xml);
    }

    @Test
    public void whenValueHasOneBoolean_shouldReturnListWithOneBoolean() {
        List<TemplateInput> params = new ArrayList<>();
        params.add(TemplateInput.factory("field3", BooleanValue, true));

        String xml = templateSerializerService.serializeXML(params);

        assertXMLEquals("<nxdt:field name=\"field3\" type=\"Boolean\" value=\"true\"/>", xml);
    }

    @Test
    public void whenValueHasOneDocumentProperty_shouldReturnListWithASourceDescription() {
        List<TemplateInput> params = new ArrayList<>();
        params.add(TemplateInput.factory("field4", DocumentProperty, "dc:description"));

        String xml = templateSerializerService.serializeXML(params);

        assertXMLEquals("<nxdt:field name=\"field4\" type=\"source\" source=\"dc:description\"/>", xml);
    }

    @Test
    public void whenValueHasOnePictureProperty_shouldReturnListWithThePictureValue() {
        List<TemplateInput> params = new ArrayList<>();
        params.add(TemplateInput.factory("field5", PictureProperty, "file:content"));

        String xml = templateSerializerService.serializeXML(params);

        assertXMLEquals("<nxdt:field name=\"field5\" type=\"picture\" source=\"file:content\"/>", xml);
    }

    @Test
    public void whenValueHasOneContent_shouldReturnListWithTheSourceValue() {
        List<TemplateInput> params = new ArrayList<>();
        params.add(TemplateInput.factory("field6", Content, "note:note"));

        String xml = templateSerializerService.serializeXML(params);

        assertXMLEquals("<nxdt:field name=\"field6\" type=\"content\" source=\"note:note\"/>", xml);
    }

    @Test
    public void whenValueHasMap_shouldReturnListWithTheMapValue() {
        List<TemplateInput> params = new ArrayList<>();

        Map<String, TemplateInput> map = new LinkedHashMap<>();
        Date date = createDate();
        map.put("almost_field1", TemplateInput.factory("field1", StringValue, "Value1"));
        map.put("almost_field2", TemplateInput.factory("field2", DateValue, date));
        map.put("almost_field3", TemplateInput.factory("field3", BooleanValue, true));
        map.put("almost_field4", TemplateInput.factory("field4", DocumentProperty, "dc:description"));
        map.put("almost_field5", TemplateInput.factory("field5", PictureProperty, "file:content"));
        map.put("almost_field6", TemplateInput.factory("field6", Content, "note:note"));

        List<TemplateInput> childList = new ArrayList<>();
        childList.add(TemplateInput.factory("field8", StringValue, "Value8"));
        map.put("almost_field7", TemplateInput.factory("field7", ListValue, childList));

        params.add(TemplateInput.factory("field7", MapValue, map));
        String xml = templateSerializerService.serializeXML(params);

        assertXMLEquals("<nxdt:field name=\"field7\" type=\"Map\">"
                + "<nxdt:field name=\"field1\" type=\"String\" value=\"Value1\"/>"
                + "<nxdt:field name=\"field2\" type=\"Date\" value=\"2017-07-14 13:14:15.678\"/>"
                + "<nxdt:field name=\"field3\" type=\"Boolean\" value=\"true\"/>"
                + "<nxdt:field name=\"field4\" type=\"source\" source=\"dc:description\"/>"
                + "<nxdt:field name=\"field5\" type=\"picture\" source=\"file:content\"/>"
                + "<nxdt:field name=\"field6\" type=\"content\" source=\"note:note\"/>"
                + "<nxdt:field name=\"field7\" type=\"List\">"
                + "<nxdt:field name=\"field8\" type=\"String\" value=\"Value8\"/>" + "</nxdt:field>" + "</nxdt:field>",
                xml);
    }

    @Test
    public void whenValueHasList_shouldReturnListWithTheListValue() {
        List<TemplateInput> params = new ArrayList<>();

        List<TemplateInput> map = new ArrayList<>();
        Date date = createDate();
        map.add(TemplateInput.factory("field1", StringValue, "Value1"));
        map.add(TemplateInput.factory("field2", DateValue, date));
        map.add(TemplateInput.factory("field3", BooleanValue, true));
        map.add(TemplateInput.factory("field4", DocumentProperty, "dc:description"));
        map.add(TemplateInput.factory("field5", PictureProperty, "file:content"));
        map.add(TemplateInput.factory("field6", Content, "note:note"));

        Map<String, TemplateInput> childMap = new LinkedHashMap<>();
        childMap.put("almost_field8", TemplateInput.factory("field8", StringValue, "Value8"));
        map.add(TemplateInput.factory("field7", MapValue, childMap));

        params.add(TemplateInput.factory("field7", ListValue, map));
        String xml = templateSerializerService.serializeXML(params);

        assertXMLEquals("<nxdt:field name=\"field7\" type=\"List\">"
                + "<nxdt:field name=\"field1\" type=\"String\" value=\"Value1\"/>"
                + "<nxdt:field name=\"field2\" type=\"Date\" value=\"2017-07-14 13:14:15.678\"/>"
                + "<nxdt:field name=\"field3\" type=\"Boolean\" value=\"true\"/>"
                + "<nxdt:field name=\"field4\" type=\"source\" source=\"dc:description\"/>"
                + "<nxdt:field name=\"field5\" type=\"picture\" source=\"file:content\"/>"
                + "<nxdt:field name=\"field6\" type=\"content\" source=\"note:note\"/>"
                + "<nxdt:field name=\"field7\" type=\"Map\">"
                + "<nxdt:field name=\"field8\" type=\"String\" value=\"Value8\"/>" + "</nxdt:field>" + "</nxdt:field>",
                xml);
    }

    private Date createDate() {
        Calendar calendar = new GregorianCalendar(2017, Calendar.JULY, 14, 13, 14, 15);
        calendar.set(Calendar.MILLISECOND, 678);
        return calendar.getTime();
    }

    private void assertXMLEquals(String expected, String actual) {
        if (expected.isEmpty()) {
            assertEquals(XML_INTRO, actual);
        } else {
            assertEquals(XML_INTRO_OPEN + expected + XML_INTRO_CLOSE, actual);
        }
    }

}
