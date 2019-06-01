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
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.template.api.InputType.ListValue;
import static org.nuxeo.template.api.InputType.MapValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.serializer.service.TemplateSerializerService;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service.xml")
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service-contribution.xml")
public class TestXMLDeserialization {

    @Inject
    protected TemplateSerializerService templateSerializerService;

    private String addRootXMLEntity(String content) {
        return "<content xmlns:dt=\"http://www.nuxeo.org/DocumentTemplate\">" + content + "</content>";
    }

    @Test
    public void whenValueIsEmpty_shouldGenerateEmptyList() {
        List<TemplateInput> result = templateSerializerService.deserializeXML("<content/>");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void whenDescribeOneString_shouldReturnAListWithOneString() {
        String XMLContent = "<dt:field name=\"field1\" type=\"String\" value=\"Value1\"/>";

        List<TemplateInput> result = templateSerializerService.deserializeXML(addRootXMLEntity(XMLContent));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field1", result.get(0).getName());
        assertEquals(InputType.StringValue, result.get(0).getType());
        assertEquals("Value1", result.get(0).getStringValue());
    }

    @Test
    public void whenDescribeOneDate_shouldReturnAListWithOneDate() {
        String XMLContent = "<dt:field name=\"field2\" type=\"Date\" value=\"2017-07-14 13:14:15.678\"/>";

        List<TemplateInput> result = templateSerializerService.deserializeXML(addRootXMLEntity(XMLContent));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field2", result.get(0).getName());
        assertEquals(InputType.DateValue, result.get(0).getType());
        SimpleDateFormat dateFormat = new SimpleDateFormat(XMLTemplateSerializer.DATE_FORMAT);
        assertEquals("2017-07-14 13:14:15.678", dateFormat.format(result.get(0).getDateValue()));
    }

    @Test
    public void whenDescribeOneBoolean_shouldReturnAListWithOneBoolean() {
        String XMLContent = "<dt:field name=\"field3\" type=\"Boolean\" value=\"true\"/>";

        List<TemplateInput> result = templateSerializerService.deserializeXML(addRootXMLEntity(XMLContent));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field3", result.get(0).getName());
        assertEquals(InputType.BooleanValue, result.get(0).getType());
        assertEquals(true, result.get(0).getBooleanValue());
    }

    @Test
    public void whenDescribeOneDocumentProperty_shouldReturnAListWithOneSourceDescription() {
        String XMLContent = "<dt:field name=\"field4\" type=\"source\" source=\"dc:description\"/>";

        List<TemplateInput> result = templateSerializerService.deserializeXML(addRootXMLEntity(XMLContent));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field4", result.get(0).getName());
        assertEquals(InputType.DocumentProperty, result.get(0).getType());
        assertEquals("dc:description", result.get(0).getSource());
    }

    @Test
    public void whenDescribeOnePictureProperty_shouldReturnAListWithOnePictureValue() {
        String XMLContent = "<dt:field name=\"field5\" type=\"picture\" source=\"file:content\"/>";

        List<TemplateInput> result = templateSerializerService.deserializeXML(addRootXMLEntity(XMLContent));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field5", result.get(0).getName());
        assertEquals(InputType.PictureProperty, result.get(0).getType());
        assertEquals("file:content", result.get(0).getSource());
    }

    @Test
    public void whenDescribeOneContent_shouldReturnAListWithOneSourceValue() {
        String XMLContent = "<dt:field name=\"field6\" type=\"content\" source=\"note:note\"/>";

        List<TemplateInput> result = templateSerializerService.deserializeXML(addRootXMLEntity(XMLContent));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field6", result.get(0).getName());
        assertEquals(InputType.Content, result.get(0).getType());
        assertEquals("note:note", result.get(0).getSource());
    }

    @Test
    public void whenDescribeMapString_shouldReturnAListWithAMap() {
        String XMLContent = "<dt:field name=\"field7\" type=\"Map\">"
                + "  <dt:field name=\"field8\" type=\"String\" value=\"Value1\"/>"
                + "  <dt:field name=\"field9\" type=\"Date\" value=\"2017-07-14 13:14:15.678\"/>"
                + "  <dt:field name=\"field10\" type=\"source\" source=\"dc:description\"/>"
                + "  <dt:field name=\"field11\" type=\"List\">"
                + "    <dt:field name=\"field8\" type=\"String\" value=\"Value1\"/>" + "  </dt:field>" + "</dt:field>";

        List<TemplateInput> result = templateSerializerService.deserializeXML(addRootXMLEntity(XMLContent));

        assertNotNull(result);
        assertEquals(1, result.size());

        TemplateInput map = result.get(0);
        assertEquals("field7", map.getName());
        assertEquals(MapValue, map.getType());

        Map<String, TemplateInput> mapValue = map.getMapValue();
        assertEquals(4, mapValue.size());
        assertEquals("field8", mapValue.get("field8").getName());
        assertEquals("String", mapValue.get("field8").getTypeAsString());

        assertEquals("field9", mapValue.get("field9").getName());
        assertEquals("Date", mapValue.get("field9").getTypeAsString());

        assertEquals("field10", mapValue.get("field10").getName());
        assertEquals("source", mapValue.get("field10").getTypeAsString());

        assertEquals("field11", mapValue.get("field11").getName());
        assertEquals("List", mapValue.get("field11").getTypeAsString());
        assertEquals(1, mapValue.get("field11").getListValue().size());
    }

    @Test
    public void whenDescribeListString_shouldReturnAListWithAList() throws ParseException {
        String XMLContent = "<dt:field name=\"field7\" type=\"List\">"
                + "  <dt:field name=\"0\" type=\"String\" value=\"Value1\"/>"
                + "  <dt:field name=\"1\" type=\"Date\" value=\"2017-07-14 13:14:15.678\"/>"
                + "  <dt:field name=\"2\" type=\"source\" source=\"dc:description\"/>"
                + "  <dt:field name=\"3\" type=\"Map\">" + "    <dt:field name=\"0\" type=\"String\" value=\"Value1\"/>"
                + "  </dt:field>" + "</dt:field>";

        List<TemplateInput> result = templateSerializerService.deserializeXML(addRootXMLEntity(XMLContent));

        assertNotNull(result);
        assertEquals(1, result.size());

        TemplateInput list = result.get(0);
        assertEquals("field7", list.getName());
        assertEquals(ListValue, list.getType());

        List<TemplateInput> listValue = list.getListValue();
        assertEquals(4, listValue.size());
        assertEquals("String", listValue.get(0).getTypeAsString());
        assertEquals("Value1", listValue.get(0).getStringValue());

        assertEquals("Date", listValue.get(1).getTypeAsString());
        Date expectedDate = new SimpleDateFormat(XMLTemplateSerializer.DATE_FORMAT).parse("2017-07-14 13:14:15.678");
        assertEquals(expectedDate, listValue.get(1).getDateValue());

        assertEquals("source", listValue.get(2).getTypeAsString());
        assertEquals("dc:description", listValue.get(2).getSource());

        assertEquals("Map", listValue.get(3).getTypeAsString());
        assertEquals(1, listValue.get(3).getMapValue().size());
    }

}
