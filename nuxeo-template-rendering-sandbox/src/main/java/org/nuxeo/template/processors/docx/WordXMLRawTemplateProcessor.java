/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.template.processors.docx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.tree.DefaultElement;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.processors.AbstractTemplateProcessor;
import org.nuxeo.template.processors.BidirectionalTemplateProcessor;

/**
 * WordXML implementation of the {@link BidirectionalTemplateProcessor}. Uses Raw XML parsing : legacy code for now.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class WordXMLRawTemplateProcessor extends AbstractTemplateProcessor implements BidirectionalTemplateProcessor {

    public static SimpleDateFormat wordXMLDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");

    public static final String TEMPLATE_TYPE = "wordXMLTemplate";

    @Override
    @SuppressWarnings("rawtypes")
    public Blob renderTemplate(TemplateBasedDocument templateDocument, String templateName) throws IOException {

        File workingDir = getWorkingDir();

        Blob blob = templateDocument.getTemplateBlob(templateName);
        String fileName = blob.getFilename();
        List<TemplateInput> params = templateDocument.getParams(templateName);

        try (CloseableFile source = blob.getCloseableFile()) {
            ZipUtils.unzip(source.getFile(), workingDir);
        }

        File xmlCustomFile = new File(workingDir.getAbsolutePath() + "/docProps/custom.xml");

        String xmlContent = FileUtils.readFile(xmlCustomFile);

        Document xmlDoc;
        try {
            xmlDoc = DocumentHelper.parseText(xmlContent);
        } catch (DocumentException e) {
            throw new IOException(e);
        }

        List nodes = xmlDoc.getRootElement().elements();

        for (Object node : nodes) {
            DefaultElement elem = (DefaultElement) node;
            if ("property".equals(elem.getName())) {
                String name = elem.attributeValue("name");
                TemplateInput param = getParamByName(name, params);
                DefaultElement valueElem = (DefaultElement) elem.elements().get(0);
                String strValue = "";
                if (param.isSourceValue()) {
                    Property property = templateDocument.getAdaptedDoc().getProperty(param.getSource());
                    if (property != null) {
                        Serializable value = templateDocument.getAdaptedDoc().getPropertyValue(param.getSource());
                        if (value != null) {
                            if (value instanceof Date) {
                                strValue = wordXMLDateFormat.format((Date) value);
                            } else {
                                strValue = value.toString();
                            }
                        }
                    }
                } else {
                    if (InputType.StringValue.equals(param.getType())) {
                        strValue = param.getStringValue();
                    } else if (InputType.BooleanValue.equals(param.getType())) {
                        strValue = param.getBooleanValue().toString();
                    } else if (InputType.DateValue.equals(param.getType())) {
                        strValue = wordXMLDateFormat.format(param.getDateValue());
                    }
                }
                valueElem.setText(strValue);
            }
        }

        String newXMLContent = xmlDoc.asXML();

        File newZipFile = Framework.createTempFile("newWordXMLTemplate", ".docx");
        xmlCustomFile.delete();
        File newXMLFile = new File(xmlCustomFile.getAbsolutePath());
        FileUtils.writeFile(newXMLFile, newXMLContent);

        File[] files = workingDir.listFiles();
        ZipUtils.zip(files, newZipFile);

        // clean up
        org.apache.commons.io.FileUtils.deleteDirectory(workingDir);

        Blob newBlob = Blobs.createBlob(newZipFile);
        Framework.trackFile(newZipFile, newBlob);
        newBlob.setFilename(fileName);

        return newBlob;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<TemplateInput> getInitialParametersDefinition(Blob blob) throws IOException {
        List<TemplateInput> params = new ArrayList<>();

        String xmlContent = readPropertyFile(blob.getStream());

        Document xmlDoc;
        try {
            xmlDoc = DocumentHelper.parseText(xmlContent);
        } catch (DocumentException e) {
            throw new IOException(e);
        }

        List nodes = xmlDoc.getRootElement().elements();

        for (Object node : nodes) {
            DefaultElement elem = (DefaultElement) node;
            if ("property".equals(elem.getName())) {
                String name = elem.attributeValue("name");
                DefaultElement valueElem = (DefaultElement) elem.elements().get(0);
                String wordType = valueElem.getName();
                InputType nxType = InputType.StringValue;
                if (wordType.contains("lpwstr")) {
                    nxType = InputType.StringValue;
                } else if (wordType.contains("filetime")) {
                    nxType = InputType.DateValue;
                } else if (wordType.contains("bool")) {
                    nxType = InputType.BooleanValue;
                }

                TemplateInput input = new TemplateInput(name);
                input.setType(nxType);
                params.add(input);
            }
        }
        return params;
    }

    protected TemplateInput getParamByName(String name, List<TemplateInput> params) {
        for (TemplateInput param : params) {
            if (param.getName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    public String readPropertyFile(InputStream in) throws IOException {
        ZipInputStream zIn = new ZipInputStream(in);
        ZipEntry zipEntry = zIn.getNextEntry();
        String xmlContent = null;
        while (zipEntry != null) {
            if (zipEntry.getName().equals("docProps/custom.xml")) {
                StringBuilder sb = new StringBuilder();
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = zIn.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, read));
                }
                xmlContent = sb.toString();
                break;
            }
            zipEntry = zIn.getNextEntry();
        }
        zIn.close();
        return xmlContent;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public DocumentModel updateDocumentFromBlob(TemplateBasedDocument templateDocument, String templateName)
            throws IOException {

        Blob blob = templateDocument.getTemplateBlob(templateName);

        String xmlContent = readPropertyFile(blob.getStream());

        if (xmlContent == null) {
            return templateDocument.getAdaptedDoc();
        }

        Document xmlDoc;
        try {
            xmlDoc = DocumentHelper.parseText(xmlContent);
        } catch (DocumentException e) {
            throw new IOException(e);
        }

        List nodes = xmlDoc.getRootElement().elements();

        DocumentModel adaptedDoc = templateDocument.getAdaptedDoc();
        List<TemplateInput> params = templateDocument.getParams(templateName);

        for (Object node : nodes) {
            DefaultElement elem = (DefaultElement) node;
            if ("property".equals(elem.getName())) {
                String name = elem.attributeValue("name");
                TemplateInput param = getParamByName(name, params);
                DefaultElement valueElem = (DefaultElement) elem.elements().get(0);
                String xmlValue = valueElem.getTextTrim();
                if (param.isSourceValue()) {
                    // XXX this needs to be rewritten

                    if (String.class.getSimpleName().equals(param.getType())) {
                        adaptedDoc.setPropertyValue(param.getSource(), xmlValue);
                    } else if (InputType.BooleanValue.equals(param.getType())) {
                        adaptedDoc.setPropertyValue(param.getSource(), new Boolean(xmlValue));
                    } else if (Date.class.getSimpleName().equals(param.getType())) {
                        try {
                            adaptedDoc.setPropertyValue(param.getSource(), wordXMLDateFormat.parse(xmlValue));
                        } catch (PropertyException | ParseException e) {
                            throw new IOException(e);
                        }
                    }
                } else {
                    if (InputType.StringValue.equals(param.getType())) {
                        param.setStringValue(xmlValue);
                    } else if (InputType.BooleanValue.equals(param.getType())) {
                        param.setBooleanValue(new Boolean(xmlValue));
                    } else if (InputType.DateValue.equals(param.getType())) {
                        try {
                            param.setDateValue(wordXMLDateFormat.parse(xmlValue));
                        } catch (ParseException e) {
                            throw new IOException(e);
                        }
                    }
                }
            }
        }
        adaptedDoc = templateDocument.saveParams(templateName, params, false);
        return adaptedDoc;
    }

}
