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

package org.nuxeo.template.processors.fm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nuxeo.template.api.InputType.BooleanValue;
import static org.nuxeo.template.api.InputType.Content;
import static org.nuxeo.template.api.InputType.DocumentProperty;
import static org.nuxeo.template.api.InputType.ListValue;
import static org.nuxeo.template.api.InputType.MapValue;
import static org.nuxeo.template.api.InputType.PictureProperty;
import static org.nuxeo.template.api.InputType.StringValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.template.api.TemplateInput;

import freemarker.template.SimpleScalar;

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
        inputParam.add(TemplateInput.factory("myListValue", ListValue,
                List.of(TemplateInput.factory("dontCare", StringValue, "myStraingueValeur"))));

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
        inputParam.add(TemplateInput.factory("myListValue", ListValue,
                List.of(TemplateInput.factory("dontCare1", BooleanValue, Boolean.TRUE),
                        TemplateInput.factory("dontCare2", BooleanValue, Boolean.FALSE))));

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

        inputParam.add(TemplateInput.factory("myListValue", ListValue,
                List.of(TemplateInput.factory("docPropValue", DocumentProperty, "my:field"))));

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

        inputParam.add(TemplateInput.factory("myListValue", ListValue,
                List.of(TemplateInput.factory("docPropValue", DocumentProperty, "my:field"))));

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

        inputParam.add(TemplateInput.factory("myListValue", ListValue,
                List.of(TemplateInput.factory("docPropValue", DocumentProperty, "my:field"))));

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

        inputParam.add(TemplateInput.factory("myListValue", ListValue,
                List.of(TemplateInput.factory("pictureProperty", PictureProperty, "my:field"))));

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

        inputParam.add(TemplateInput.factory("myListValue", ListValue,
                List.of(TemplateInput.factory("contentValue", Content, "blobContent"))));

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
        inputParam.add(TemplateInput.factory("myMapValue", MapValue,
                List.of(TemplateInput.factory("dontCare", StringValue, "myStraingueValeur"))));

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
        inputParam.add(TemplateInput.factory("myMapValue", MapValue,
                List.of(TemplateInput.factory("dontCare1", BooleanValue, Boolean.TRUE),
                        TemplateInput.factory("dontCare2", BooleanValue, Boolean.FALSE))));

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

        inputParam.add(TemplateInput.factory("myMapValue", MapValue,
                List.of(TemplateInput.factory("docPropValue", DocumentProperty, "my:field"))));

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

        inputParam.add(TemplateInput.factory("myMapValue", MapValue,
                List.of(TemplateInput.factory("docPropValue", DocumentProperty, "my:field"))));

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

        inputParam.add(TemplateInput.factory("myMapValue", MapValue,
                List.of(TemplateInput.factory("docPropValue", DocumentProperty, "my:field"))));

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

        inputParam.add(TemplateInput.factory("myMapValue", MapValue,
                List.of(TemplateInput.factory("pictureProperty", PictureProperty, "my:field"))));

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

        inputParam.add(TemplateInput.factory("myMapValue", MapValue,
                List.of(TemplateInput.factory("contentValue", Content, "blobContent"))));

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
