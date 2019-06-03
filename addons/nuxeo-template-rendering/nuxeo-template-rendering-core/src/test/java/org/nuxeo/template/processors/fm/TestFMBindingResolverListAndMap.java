package org.nuxeo.template.processors.fm;

import freemarker.template.SimpleScalar;
import org.junit.Test;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.template.api.TemplateInput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.nuxeo.template.api.InputType.*;

public class TestFMBindingResolverListAndMap extends TestFMBindingAbstract {

    @Test
    public void whenParamIsList_empty_shouldAddEmptyListValue() {
        inputParam.add(TemplateInput.factory("myListValue", ListValue, new ArrayList<>()));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myListValue"));
        assertTrue(ctx.get("myListValue") instanceof List);
        List list = (List) ctx.get("myListValue");
        assertEquals(0, list.size());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsList_string_shouldAddListValue() {
        inputParam.add(TemplateInput.factory("myListValue", ListValue, List.of(
                TemplateInput.factory("dontCare", StringValue, "myStraingueValeur")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myListValue"));
        assertTrue(ctx.get("myListValue") instanceof List);
        List list = (List) ctx.get("myListValue");
        assertEquals(1, list.size());
        assertEquals("myStraingueValeur", list.get(0));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsList_date_shouldAddListValue() {
        inputParam.add(TemplateInput.factory("myListValue", ListValue, List.of(
                TemplateInput.factory("dontCare1", BooleanValue, Boolean.TRUE),
                TemplateInput.factory("dontCare2", BooleanValue, Boolean.FALSE)
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myListValue"));
        assertTrue(ctx.get("myListValue") instanceof List);
        List list = (List) ctx.get("myListValue");
        assertEquals(2, list.size());
        assertEquals(Boolean.TRUE, list.get(0));
        assertEquals(Boolean.FALSE, list.get(1));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsList_DocProperty_String_shouldAddListValue() {
        definePropertyInDoc(doc, "my:field", "Here is the string");

        inputParam.add(TemplateInput.factory("myListValue", ListValue, List.of(
                TemplateInput.factory("docPropValue", DocumentProperty, "my:field")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myListValue"));
        assertTrue(ctx.get("myListValue") instanceof List);
        List list = (List) ctx.get("myListValue");
        assertEquals(1, list.size());
        assertTrue(list.get(0) instanceof SimpleScalar);
        assertEquals("Here is the string", ((SimpleScalar) list.get(0)).getAsString());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsList_DocProperty_Blob_shouldAddEmptyListValue() {
        definePropertyInDoc(doc, "my:field", new StringBlob("Here is the Blob"));

        inputParam.add(TemplateInput.factory("myListValue", ListValue, List.of(
                TemplateInput.factory("docPropValue", DocumentProperty, "my:field")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myListValue"));
        assertTrue(ctx.get("myListValue") instanceof List);
        List list = (List) ctx.get("myListValue");
        // DocProperty if blob are ignored ?? that's not me...
        assertEquals(0, list.size());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsList_DocProperty_NoPropertyDefined_shouldAddEmptyListValue() {
        when(doc.getProperty("my:field")).thenThrow(new PropertyException());
        when(doc.getPropertyValue("my:field")).thenThrow(new PropertyException());

        inputParam.add(TemplateInput.factory("myListValue", ListValue, List.of(
                TemplateInput.factory("docPropValue", DocumentProperty, "my:field")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myListValue"));
        assertTrue(ctx.get("myListValue") instanceof List);
        List list = (List) ctx.get("myListValue");
        assertEquals(0, list.size());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsList_PictureProperty_Blob_shouldAddListValue() {
        definePropertyInDoc(doc, "my:field", new StringBlob("Here is the Blob"));

        inputParam.add(TemplateInput.factory("myListValue", ListValue, List.of(
                TemplateInput.factory("pictureProperty", PictureProperty, "my:field")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myListValue"));
        assertTrue(ctx.get("myListValue") instanceof List);
        List list = (List) ctx.get("myListValue");
        assertEquals(1, list.size());
        assertNull(list.get(0));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(1)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsList_ContentProperty_Blob_shouldAddListValue() {
        definePropertyInDoc(doc, "blobContent", new StringBlob("Here is the Blob"));

        inputParam.add(TemplateInput.factory("myListValue", ListValue, List.of(
                TemplateInput.factory("contentValue", Content, "blobContent")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myListValue"));
        assertTrue(ctx.get("myListValue") instanceof List);
        List list = (List) ctx.get("myListValue");
        assertEquals(1, list.size());
        assertEquals("Here is the Blob", list.get(0));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(1)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }


    @Test
    public void whenParamIsLMap_empty_shouldAddEmptyMapValue() {
        inputParam.add(TemplateInput.factory("myMapValue", MapValue, List.of()));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myMapValue"));
        assertTrue(ctx.get("myMapValue") instanceof Map);

        Map map = (Map) ctx.get("myMapValue");
        assertEquals(0, map.size());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsLMap_string_shouldAddMapValue() {
        inputParam.add(TemplateInput.factory("myMapValue", MapValue, List.of(
                TemplateInput.factory("dontCare", StringValue, "myStraingueValeur")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myMapValue"));
        assertTrue(ctx.get("myMapValue") instanceof Map);

        Map map = (Map) ctx.get("myMapValue");
        assertEquals(1, map.size());
        assertEquals("myStraingueValeur", map.get("dontCare"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsLMap_date_shouldAddLMapalue() {
        inputParam.add(TemplateInput.factory("myMapValue", MapValue, List.of(
                TemplateInput.factory("dontCare1", BooleanValue, Boolean.TRUE),
                TemplateInput.factory("dontCare2", BooleanValue, Boolean.FALSE)
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myMapValue"));
        assertTrue(ctx.get("myMapValue") instanceof Map);

        Map map = (Map) ctx.get("myMapValue");
        assertEquals(2, map.size());
        assertEquals(Boolean.TRUE, map.get("dontCare1"));
        assertEquals(Boolean.FALSE, map.get("dontCare2"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsLMap_DocProperty_String_shouldAddMapValue() {
        definePropertyInDoc(doc, "my:field", "Here is the string");

        inputParam.add(TemplateInput.factory("myMapValue", MapValue, List.of(
                TemplateInput.factory("docPropValue", DocumentProperty, "my:field")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myMapValue"));
        assertTrue(ctx.get("myMapValue") instanceof Map);

        Map map = (Map) ctx.get("myMapValue");
        assertEquals(1, map.size());
        assertTrue(map.get("docPropValue") instanceof SimpleScalar);
        assertEquals("Here is the string", ((SimpleScalar) map.get("docPropValue")).getAsString());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsLMap_DocProperty_Blob_shouldAddEmptyMapValue() {
        definePropertyInDoc(doc, "my:field", new StringBlob("Here is the Blob"));

        inputParam.add(TemplateInput.factory("myMapValue", MapValue, List.of(
                TemplateInput.factory("docPropValue", DocumentProperty, "my:field")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myMapValue"));
        assertTrue(ctx.get("myMapValue") instanceof Map);

        Map map = (Map) ctx.get("myMapValue");
        // DocProperty if blob are ignored ?? that's not me...
        assertEquals(0, map.size());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsLMap_DocProperty_NoPropertyDefined_shouldAddEmptyMapValue() {
        when(doc.getProperty("my:field")).thenThrow(new PropertyException());
        when(doc.getPropertyValue("my:field")).thenThrow(new PropertyException());

        inputParam.add(TemplateInput.factory("myMapValue", MapValue, List.of(
                TemplateInput.factory("docPropValue", DocumentProperty, "my:field")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myMapValue"));
        assertTrue(ctx.get("myMapValue") instanceof Map);

        Map map = (Map) ctx.get("myMapValue");
        assertEquals(0, map.size());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsLMap_PictureProperty_Blob_shouldAddMapValue() {
        definePropertyInDoc(doc, "my:field", new StringBlob("Here is the Blob"));

        inputParam.add(TemplateInput.factory("myMapValue", MapValue, List.of(
                TemplateInput.factory("pictureProperty", PictureProperty, "my:field")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myMapValue"));
        assertTrue(ctx.get("myMapValue") instanceof Map);

        Map map = (Map) ctx.get("myMapValue");
        assertEquals(1, map.size());
        assertNull(map.get("pictureProperty"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(1)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsLMap_ContentProperty_Blob_shouldAddMapValue() {
        definePropertyInDoc(doc, "blobContent", new StringBlob("Here is the Blob"));

        inputParam.add(TemplateInput.factory("myMapValue", MapValue, List.of(
                TemplateInput.factory("contentValue", Content, "blobContent")
        )));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myMapValue"));
        assertTrue(ctx.get("myMapValue") instanceof Map);

        Map map = (Map) ctx.get("myMapValue");
        assertEquals(1, map.size());
        assertEquals("Here is the Blob", map.get("contentValue"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(1)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }


}
