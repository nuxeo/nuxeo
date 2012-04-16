package org.nuxeo.ecm.platform.template.tests;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.nuxeo.template.XMLSerializer;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;

public class TestXMLSerialization extends TestCase {

    @Test
    public void testXMLSerialization() throws Exception {

        List<TemplateInput> params = new ArrayList<TemplateInput>();

        TemplateInput input1 = new TemplateInput("field1", "Value1");
        input1.setDesciption("Some description");
        params.add(input1);

        Date date = new Date();
        TemplateInput input2 = new TemplateInput("field2", date);
        params.add(input2);

        TemplateInput input3 = new TemplateInput("field3", new Boolean(true));
        params.add(input3);

        TemplateInput input4 = new TemplateInput("field4");
        input4.setType(InputType.DocumentProperty);
        input4.setSource("dc:description");
        params.add(input4);

        TemplateInput input5 = new TemplateInput("field5");
        input5.setSource("file:content");
        input5.setType(InputType.PictureProperty);
        params.add(input5);

        TemplateInput input6 = new TemplateInput("field6");
        input6.setSource("note:note");
        input6.setType(InputType.Content);
        params.add(input6);

        String xml = XMLSerializer.serialize(params);

        // System.out.println(xml);

        List<TemplateInput> params2 = XMLSerializer.readFromXml(xml);
        assertNotNull(params2);
        assertEquals(6, params2.size());

        assertEquals("field1", params2.get(0).getName());
        assertEquals(InputType.StringValue, params2.get(0).getType());
        assertEquals("Some description", params2.get(0).getDesciption());
        assertEquals("Value1", params2.get(0).getStringValue());

        assertEquals("field2", params2.get(1).getName());
        assertEquals(InputType.DateValue, params2.get(1).getType());
        assertEquals(XMLSerializer.dateFormat.format(date),
                XMLSerializer.dateFormat.format(params2.get(1).getDateValue()));

        assertEquals("field3", params2.get(2).getName());
        assertEquals(InputType.BooleanValue, params2.get(2).getType());
        assertEquals(new Boolean(true), params2.get(2).getBooleanValue());

        assertEquals("field4", params2.get(3).getName());
        assertEquals(InputType.DocumentProperty, params2.get(3).getType());
        assertEquals("dc:description", params2.get(3).getSource());

        assertEquals("field5", params2.get(4).getName());
        assertEquals(InputType.PictureProperty, params2.get(4).getType());
        assertEquals("file:content", params2.get(4).getSource());

        assertEquals("field6", params2.get(5).getName());
        assertEquals(InputType.Content, params2.get(5).getType());
        assertEquals("note:note", params2.get(5).getSource());
    }

}
