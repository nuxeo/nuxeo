package org.nuxeo.template.serializer.executors;

import org.dom4j.DocumentException;
import org.junit.Test;
import org.nuxeo.template.XMLSerializer;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;

import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestXMLDeserialization {

    public XMLSerializer serializer = new XMLSerializer();

    private String addRootXMLEntity(String content) {
        return "<content xmlns:dt=\"http://www.nuxeo.org/DocumentTemplate\">" +
                content +
                "</content>";
    }

    @Test
    public void whenValueIsEmpty_shouldGenerateEmptyList() throws DocumentException {
        List<TemplateInput> result = serializer.readFromXml("<content/>");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void whenDescribeOneString_shouldReturnAListWithOneString() throws DocumentException {
        String XMLContent = "<dt:field name=\"field1\" type=\"String\" value=\"Value1\"/>";
        List<TemplateInput> result = serializer.readFromXml(addRootXMLEntity(XMLContent));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field1", result.get(0).getName());
        assertEquals(InputType.StringValue, result.get(0).getType());
        assertEquals("Value1", result.get(0).getStringValue());
    }


    @Test
    public void whenDescribeOneDate_shouldReturnAListWithOneDate() throws DocumentException {
        String XMLContent = "<dt:field name=\"field2\" type=\"Date\" value=\"2017-07-14 13:14:15.678\"/>";
        List<TemplateInput> result = serializer.readFromXml(addRootXMLEntity(XMLContent));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field2", result.get(0).getName());
        assertEquals(InputType.DateValue, result.get(0).getType());
        SimpleDateFormat dateFormat = new SimpleDateFormat(XMLSerializer.DATE_FORMAT);
        assertEquals("2017-07-14 13:14:15.678", dateFormat.format(result.get(0).getDateValue()));
    }

    @Test
    public void whenDescribeOneBoolean_shouldReturnAListWithOneBoolean() throws DocumentException {
        String XMLContent = "<dt:field name=\"field3\" type=\"Boolean\" value=\"true\"/>";
        List<TemplateInput> result = serializer.readFromXml(addRootXMLEntity(XMLContent));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field3", result.get(0).getName());
        assertEquals(InputType.BooleanValue, result.get(0).getType());
        assertEquals(true, result.get(0).getBooleanValue());
    }

    @Test
    public void whenDescribeOneDocumentProperty_shouldReturnAListWithOneSourceDescription() throws DocumentException {
        String XMLContent = "<dt:field name=\"field4\" type=\"source\" source=\"dc:description\"/>";
        List<TemplateInput> result = serializer.readFromXml(addRootXMLEntity(XMLContent));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field4", result.get(0).getName());
        assertEquals(InputType.DocumentProperty, result.get(0).getType());
        assertEquals("dc:description", result.get(0).getSource());
    }

    @Test
    public void whenDescribeOnePictureProperty_shouldReturnAListWithOnePictureValue() throws DocumentException {
        String XMLContent = "<dt:field name=\"field5\" type=\"picture\" source=\"file:content\"/>";
        List<TemplateInput> result = serializer.readFromXml(addRootXMLEntity(XMLContent));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field5", result.get(0).getName());
        assertEquals(InputType.PictureProperty, result.get(0).getType());
        assertEquals("file:content", result.get(0).getSource());
    }

    @Test
    public void whenDescribeOneContent_shouldReturnAListWithOneSourceValue() throws DocumentException {
        String XMLContent = "<dt:field name=\"field6\" type=\"content\" source=\"note:note\"/>";
        List<TemplateInput> result = serializer.readFromXml(addRootXMLEntity(XMLContent));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("field6", result.get(0).getName());
        assertEquals(InputType.Content, result.get(0).getType());
        assertEquals("note:note", result.get(0).getSource());
    }


}
