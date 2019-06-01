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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nuxeo.template.api.InputType.BooleanValue;
import static org.nuxeo.template.api.InputType.Content;
import static org.nuxeo.template.api.InputType.DateValue;
import static org.nuxeo.template.api.InputType.DocumentProperty;
import static org.nuxeo.template.api.InputType.PictureProperty;
import static org.nuxeo.template.api.InputType.StringValue;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.schema.types.AnyType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.template.api.TemplateInput;

import freemarker.template.SimpleScalar;

public class TestFMBindingResolver extends TestFMBindingAbstract {

    @Test
    public void whenParamIsContentAndSourceHTMLPreviewPropValue_shouldAddHTMLPreviewInContext() throws IOException {
        definePropertyInDoc(doc, "file:content", new StringBlob("<h1>Hello<h1> wolrd !", "text/html"));

        TemplateInput param = TemplateInput.factory("myHtmlPreview", Content, "htmlPreview");
        inputParam.add(param);
        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertEquals("<h1>Hello<h1> wolrd !", ctx.get("myHtmlPreview"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(1)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsContentAndSourceBlobContentPropValue_shouldAddContentInContext() {
        definePropertyInDoc(doc, "blobContent", new StringBlob("Hello buddies !"));

        TemplateInput param = TemplateInput.factory("myContent", Content, "blobContent");
        inputParam.add(param);
        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myContent"));
        assertEquals("Hello buddies !", ctx.get("myContent"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(1)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsContentAndSourceDocPropValueAndValueIsString_shouldAddStringCleaningHTMLIntroduction() {
        definePropertyInDoc(doc, "my:field", "<html><body>Boring to say hello everytime !</body></html>");

        inputParam.add(TemplateInput.factory("stringFieldInSource", Content, "my:field"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("stringFieldInSource"));
        assertEquals("Boring to say hello everytime !", ctx.get("stringFieldInSource"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(1)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsPicturePropertyAndSourceDocPropIsBlob_shouldAddNullAsNotManaged() {
        definePropertyInDoc(doc, "my:field", new StringBlob("This is a picture", "image/png"));

        inputParam.add(TemplateInput.factory("pictureFieldInSource", PictureProperty, "my:picture"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("pictureFieldInSource"));
        assertNull(ctx.get("pictureFieldInSource"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(1)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsPicturePropertyAndSourceDocPropIsBlobWithoutMimeType_shouldAddNullAsNotManaged() {
        Blob blob = spy(new StringBlob("This is a picture", null));
        definePropertyInDoc(doc, "my:picture", blob);

        inputParam.add(TemplateInput.factory("pictureFieldInSource", PictureProperty, "my:picture"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("pictureFieldInSource"));
        assertNull(ctx.get("pictureFieldInSource"));

        verify(blob, times(1)).setMimeType("image/jpeg");
        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(1)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());
    }

    @Test
    public void whenParamIsPicturePropertyAndSourceDocPropIsBlobAndAutoLoop_shouldAddWrappedValue() {
        definePropertyInDoc(doc, "my:loop", "Value that can be anything (eventually a loop)");

        inputParam.add(TemplateInput.factory("loopInSource", PictureProperty, "my:loop", null, false, true));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("loopInSource"));
        assertTrue(ctx.get("loopInSource") instanceof SimpleScalar);
        assertEquals("Value that can be anything (eventually a loop)",
                ((SimpleScalar) ctx.get("loopInSource")).getAsString());

        verify(resolver, times(1)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsPicturePropertyAndSourceDocPropIsNull_shouldAddWrappedValue() {
        definePropertyInDoc(doc, "my:loop", null, AnyType.INSTANCE);

        inputParam.add(TemplateInput.factory("loopInSource", PictureProperty, "my:loop", null, false, true));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("loopInSource"));
        assertNull(ctx.get("loopInSource"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(1)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsPicturePropertyAndSourceDocPropNotBlob_shouldAddWrappedValue() {
        definePropertyInDoc(doc, "my:field", "Should be wrapped");

        inputParam.add(TemplateInput.factory("pictureNotBlob", PictureProperty, "my:field"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("pictureNotBlob"));
        assertTrue(ctx.get("pictureNotBlob") instanceof SimpleScalar);
        assertEquals("Should be wrapped", ((SimpleScalar) ctx.get("pictureNotBlob")).getAsString());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsPicturePropertyAndSourceDocPropThrowException_shouldAddWrappedValue() {
        when(doc.getProperty("my:fieldThatNotExists")).thenThrow(new PropertyException());
        when(doc.getPropertyValue("my:fieldThatNotExists")).thenThrow(new PropertyException());

        inputParam.add(TemplateInput.factory("pictureException", PictureProperty, "my:fieldThatNotExists"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("pictureException"));
        assertNull(ctx.get("pictureNotBlob"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(1)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDocumentPropertyAndSourceDocPropIsBlob_shouldAddWrappedValue() {
        definePropertyInDoc(doc, "my:field", new StringBlob("Should be wrapped"));

        inputParam.add(TemplateInput.factory("docPropertyBlob", DocumentProperty, "my:field"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(0, ctx.size());
        // Should be that ?? at least or BlobWrapped ?
        // assertEquals(1, ctx.size());
        // assertTrue(ctx.containsKey("docPropertyBlob"));
        // assertEquals("String", ctx.get("docPropertyBlob").getClass().getSimpleName());
        // assertEquals("", ctx.get("docPropertyBlob"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDocumentPropertyAndSourceDocPropIsNotBlob_shouldAddNullAsNotManaged() {
        definePropertyInDoc(doc, "my:field", "Should be wrapped");

        inputParam.add(TemplateInput.factory("docPropertyNotBlob", DocumentProperty, "my:field"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("docPropertyNotBlob"));
        assertTrue(ctx.get("docPropertyNotBlob") instanceof SimpleScalar);
        assertEquals("Should be wrapped", ((SimpleScalar) ctx.get("docPropertyNotBlob")).getAsString());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDocumentPropertyAndSourceDocPropIsBlobAndAutoLoop_shouldAddWrappedValue() {
        definePropertyInDoc(doc, "my:loop", "Value that can be anything (eventually a loop)");

        inputParam.add(TemplateInput.factory("loopInSource", DocumentProperty, "my:loop", null, false, true));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("loopInSource"));
        assertTrue(ctx.get("loopInSource") instanceof SimpleScalar);
        assertEquals("Value that can be anything (eventually a loop)",
                ((SimpleScalar) ctx.get("loopInSource")).getAsString());

        verify(resolver, times(1)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDocumentPropertyAndSourceDocPropBooleanNotSet_shouldAddFalseAsDefault() {
        definePropertyInDoc(doc, "my:field", null, BooleanType.INSTANCE);

        inputParam.add(TemplateInput.factory("defaultBoolean", DocumentProperty, "my:field"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("defaultBoolean"));
        assertEquals(Boolean.FALSE, ctx.get("defaultBoolean"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDocumentPropertyAndSourceDocPropDateNotSet_shouldAddCurrentDateAsDefault()
            throws InterruptedException {
        Date beforeTestExecution = new Date();

        definePropertyInDoc(doc, "my:field", null, DateType.INSTANCE);

        inputParam.add(TemplateInput.factory("defaultDate", DocumentProperty, "my:field"));

        Thread.sleep(1);
        resolver.resolve(inputParam, ctx, templateBasedDoc);
        Thread.sleep(1);

        Date afterTestExecution = new Date();

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("defaultDate"));
        assertTrue(ctx.get("defaultDate") instanceof Date);

        Date actualDate = (Date) ctx.get("defaultDate");

        System.out.println("****** before: " + beforeTestExecution.getTime());
        System.out.println("****** result: " + actualDate.getTime());
        System.out.println("****** after: " + afterTestExecution.getTime());

        assertTrue(beforeTestExecution.before(actualDate));
        assertTrue(afterTestExecution.after(actualDate));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDocumentPropertyAndSourceDocPropStringNotSet_shouldAddEmptyStringAsDefault() {
        definePropertyInDoc(doc, "my:field", null, StringType.INSTANCE);

        inputParam.add(TemplateInput.factory("defaultString", DocumentProperty, "my:field"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("defaultString"));
        assertEquals("", ctx.get("defaultString"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDocumentPropertyAndSourceDocPropBlobNotSet_shouldAddEmptyStringAsDefault() {
        definePropertyInDoc(doc, "my:field", (Blob) null);

        inputParam.add(TemplateInput.factory("defaultBlob", DocumentProperty, "my:field"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("defaultBlob"));
        assertEquals("", ctx.get("defaultBlob"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDocumentPropertyAndSourceDocPropUnknownNotSet_shouldAddNOVALUEStringAsDefault() {
        definePropertyInDoc(doc, "my:field", null, AnyType.INSTANCE);

        inputParam.add(TemplateInput.factory("defaultUnknownType", DocumentProperty, "my:field"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("defaultUnknownType"));
        assertEquals("!NOVALUE!", ctx.get("defaultUnknownType"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDocumentPropertyAndSourceDocPropNotInType_shouldAddNothing() {
        when(doc.getProperty("my:fieldThatNotExists")).thenThrow(new PropertyException());
        when(doc.getPropertyValue("my:fieldThatNotExists")).thenThrow(new PropertyException());

        inputParam.add(TemplateInput.factory("fieldNameNotExistingInType", DocumentProperty, "my:fieldThatNotExists"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(0, ctx.size());

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsString_shouldAddStringValue() {
        when(doc.getProperty(any())).thenThrow(new PropertyException());
        when(doc.getPropertyValue(any())).thenThrow(new PropertyException());

        inputParam.add(TemplateInput.factory("myString", StringValue, "stringValue"));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myString"));
        assertEquals("stringValue", ctx.get("myString"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsBoolean_shouldAddBooleanValue() {
        when(doc.getProperty(any())).thenThrow(new PropertyException());
        when(doc.getPropertyValue(any())).thenThrow(new PropertyException());

        inputParam.add(TemplateInput.factory("myTrueValue", BooleanValue, Boolean.TRUE));
        inputParam.add(TemplateInput.factory("myFalseValue", BooleanValue, Boolean.FALSE));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(2, ctx.size());
        assertTrue(ctx.containsKey("myTrueValue"));
        assertTrue(ctx.containsKey("myFalseValue"));
        assertEquals(true, ctx.get("myTrueValue"));
        assertEquals(false, ctx.get("myFalseValue"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

    @Test
    public void whenParamIsDate_shouldAddDateValue() {
        when(doc.getProperty(any())).thenThrow(new PropertyException());
        when(doc.getPropertyValue(any())).thenThrow(new PropertyException());

        Date date = new Date();
        inputParam.add(TemplateInput.factory("myDateValue", DateValue, date));

        resolver.resolve(inputParam, ctx, templateBasedDoc);

        assertEquals(1, ctx.size());
        assertTrue(ctx.containsKey("myDateValue"));
        assertEquals(date, ctx.get("myDateValue"));

        verify(resolver, times(0)).handleLoop(any(), any());
        verify(resolver, times(0)).handlePictureField(any(), any());
        verify(resolver, times(0)).handleBlobField(any(), any());
        verify(resolver, times(0)).handleHtmlField(any(), any());

    }

}
