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

package org.nuxeo.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.template.api.InputType.ListValue;
import static org.nuxeo.template.api.InputType.MapValue;
import static org.nuxeo.template.api.InputType.StringValue;
import static org.nuxeo.template.api.TemplateInput.factory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.context.extensions.ContextFunctions;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.rendition.api")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.platform.rendition.publisher")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.versioning.api")
@Deploy("org.nuxeo.ecm.platform.versioning")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.template.manager")
public class TestTemplateRenderingService {

    protected static final String TEMPLATE_NAME = "mytestTemplate";

    protected static final String WEBVIEW_RENDITION = "webView";

    @Inject
    protected CoreSession session;

    @Inject
    TemplateProcessorService tps;

    protected DocumentModel renditionContainer;

    @Before
    public void setup() {
        renditionContainer = session.createDocumentModel("/", "renditions", "Folder");
        renditionContainer = session.createDocument(renditionContainer);
    }

    @Test
    public void whenTemplateWithoutDynamicPart_shouldRenderBlobAsIt() throws IOException {
        TemplateSourceDocument templateSrc = createTemplateSourceDoc("Hello world !", WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("Hello world !", result.getString());
    }

    @Test
    public void whenTemplateWithUsename_shouldRenderBlobAsIt() throws IOException {
        TemplateSourceDocument templateSrc = createTemplateSourceDoc("Hello ${username} !", WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("Hello Administrator !", result.getString());
    }

    @Test
    public void whenTemplateWithTemplateDocTitle_shouldRenderBlobAsIt() throws IOException {
        TemplateSourceDocument templateSrc = createTemplateSourceDoc("Hello from ${doc['dc:title']} !",
                WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("Hello from MyTemplateBase !", result.getString());
    }

    @Test
    public void whenTemplateWithGivenInputTemplate_String_shouldRenderBlobAsIt() throws IOException {
        TemplateSourceDocument templateSrc = createTemplateSourceDoc("We are introducing ${myStringInCtxt} !",
                WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());
        List<TemplateInput> params = List.of(factory("myStringInCtxt", StringValue, "Beautiful String"));
        templateBase.saveParams(TEMPLATE_NAME, params, true);

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("We are introducing Beautiful String !", result.getString());
    }

    @Test
    public void whenTemplateWithGivenInputTemplate_ListString_shouldRenderBlobAsIt() throws IOException {
        String template = "Song : ${lyrics} \n<#list myListValue as item>${item}, </#list>";
        TemplateSourceDocument templateSrc = createTemplateSourceDoc(template, WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        List<TemplateInput> params = new ArrayList<>();
        params.add(factory("myListValue", InputType.ListValue, List.of(factory("Anton", StringValue, "Anton"),
                factory("Ivan", StringValue, "Ivan"), factory("Boris", StringValue, "Boris"))));
        params.add(TemplateInput.factory("lyrics", StringValue,
                "https://www.paroles.net/marie-laforet/paroles-ivan-boris-et-moi"));

        templateBase.saveParams(TEMPLATE_NAME, params, true);

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("Song : https://www.paroles.net/marie-laforet/paroles-ivan-boris-et-moi \nAnton, Ivan, Boris, ",
                result.getString());
    }

    @Test
    public void whenTemplateWithGivenInputTemplate_ComplexList_shouldRenderBlobAsIt() throws IOException {
        String template = "<#list myListValue as item>${item.firstname} ${item.lastname}, </#list>";
        TemplateSourceDocument templateSrc = createTemplateSourceDoc(template, WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        List<TemplateInput> params = new ArrayList<>();
        params.add(createPersonTemplateInputListFromString("myListValue",
                List.of("John Fitzgerald Kennedy", "Lee Harvey Oswald", "Barack Hussein Obama")));

        templateBase.saveParams(TEMPLATE_NAME, params, true);

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("John Kennedy, Lee Oswald, Barack Obama, ", result.getString());
    }

    public TemplateInput createPersonTemplateInputListFromString(String paramName, List<String> persons) {
        List<TemplateInput> result = new ArrayList<>();
        int index = 0;
        for (String person : persons) {
            List<TemplateInput> personTI = new ArrayList<>();
            String[] infos = person.split(" ");
            personTI.add(factory("firstname", StringValue, infos[0]));
            personTI.add(factory("middleName", StringValue, infos[1]));
            personTI.add(factory("lastname", StringValue, infos[2]));

            result.add(factory("" + index, MapValue, personTI));
            index++;
        }
        return factory(paramName, ListValue, result);
    }

    @Test
    public void whenTemplateWithGivenInputTemplate_MapString_shouldRenderBlobAsIt() throws IOException {
        String template = "Song : ${lyrics} \n<#list myListValue?values as item>${item}, </#list>";
        TemplateSourceDocument templateSrc = createTemplateSourceDoc(template, WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        List<TemplateInput> params = new ArrayList<>();
        params.add(factory("myListValue", MapValue, List.of(factory("Anton", StringValue, "Anton Mariano"),
                factory("Ivan", StringValue, "Ivan Sanchez"), factory("Boris", StringValue, "Boris Sirob"))));
        params.add(TemplateInput.factory("lyrics", StringValue,
                "https://www.paroles.net/marie-laforet/paroles-ivan-boris-et-moi"));

        templateBase.saveParams(TEMPLATE_NAME, params, true);

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals(
                "Song : https://www.paroles.net/marie-laforet/paroles-ivan-boris-et-moi \nIvan Sanchez, Anton Mariano, Boris Sirob, ",
                result.getString());
    }

    @Test
    public void whenTemplateWithGivenInputTemplate_ComplexMap_shouldRenderBlobAsIt() throws IOException {
        String template = "<#list myListValue?values as item>${item.firstname}, </#list>";
        TemplateSourceDocument templateSrc = createTemplateSourceDoc(template, WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        List<TemplateInput> params = new ArrayList<>();
        params.add(createPersonTemplateInputMapFromString("myListValue",
                List.of("John Fitzgerald Kennedy", "Lee Harvey Oswald", "Barack Hussein Obama")));

        templateBase.saveParams(TEMPLATE_NAME, params, true);

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("Lee, Barack, John, ", result.getString());
    }

    public TemplateInput createPersonTemplateInputMapFromString(String paramName, List<String> persons) {
        List<TemplateInput> result = new ArrayList<>();
        for (String person : persons) {
            List<TemplateInput> personTI = new ArrayList<>();
            String[] infos = person.split(" ");
            personTI.add(factory("firstname", StringValue, infos[0]));
            personTI.add(factory("middleName", StringValue, infos[1]));
            personTI.add(factory("lastname", StringValue, infos[2]));

            result.add(factory(infos[2], MapValue, personTI));
        }
        return factory(paramName, MapValue, result);
    }

    protected TemplateBasedDocument createTemplateBasedDoc(DocumentModel templateDoc) {

        DocumentModel templateBase = session.createDocumentModel("renditions", "templateBase", "Note");
        templateBase.setPropertyValue("dc:title", "MyTemplateBase");
        templateBase = session.createDocument(templateBase);

        // associate File to template
        templateBase = tps.makeTemplateBasedDocument(templateBase, templateDoc, true);

        return templateBase.getAdapter(TemplateBasedDocument.class);
    }

    protected TemplateSourceDocument createTemplateSourceDoc(String templateString, String targetRendition) {
        return createTemplateSourceDoc(templateString, targetRendition, new ArrayList<>());
    }

    protected TemplateSourceDocument createTemplateSourceDoc(String templateString, String targetRendition,
            List<TemplateInput> templateInputs) {

        // create the template
        DocumentModel templateSource = session.createDocumentModel("/", "templatedDoc", "TemplateSource");
        templateSource.setPropertyValue("dc:title", "MyTemplateSource");
        templateSource.setPropertyValue("file:content", new StringBlob(templateString, "text/x-freemarker"));
        templateSource.setPropertyValue("tmpl:templateName", TEMPLATE_NAME);

        templateSource = session.createDocument(templateSource);

        // configure targetRendition and output format
        TemplateSourceDocument source = templateSource.getAdapter(TemplateSourceDocument.class);
        // source.setTargetRenditioName(targetRendition, false);

        for (TemplateInput param : templateInputs) {
            source.addInput(param);
        }

        // update the doc and adapter
        templateSource = session.saveDocument(source.getAdaptedDoc());
        session.save();

        return templateSource.getAdapter(TemplateSourceDocument.class);
    }

    @Test
    public void testFormatDate() {
        String defaultFormat = "MM/dd/yyyy";
        String format1 = "yyyy MMM dd HH:mm:ss";
        String format2 = "EEEE dd MMMMM yyyy";
        Locale locale = Locale.getDefault();

        GregorianCalendar calendar = new GregorianCalendar(2013, 11, 31);
        Date dt = calendar.getTime();

        SimpleDateFormat defaultSdf = new SimpleDateFormat(defaultFormat, locale);
        SimpleDateFormat sdf1 = new SimpleDateFormat(format1, locale);
        SimpleDateFormat sdf2 = new SimpleDateFormat(format2, locale);

        ContextFunctions cf = new ContextFunctions(null, null);

        assertEquals(defaultSdf.format(dt), cf.formatDate(calendar));
        assertEquals(sdf1.format(dt), cf.formatDate(calendar, format1));
        assertEquals(sdf2.format(dt), cf.formatDate(calendar, format2));

    }
}
