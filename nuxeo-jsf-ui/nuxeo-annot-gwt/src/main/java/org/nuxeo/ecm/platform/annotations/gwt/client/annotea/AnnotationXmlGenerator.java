/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.AnnotationDefinition;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.AnnotationUtils;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPointer;

import com.google.gwt.user.client.Window;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class AnnotationXmlGenerator {

    private final WebConfiguration webConfiguration;

    private final Annotation annotation;

    private String annotationXml = "<?xml version=\"1.0\"?>"
            + "<r:RDF xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
            + "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:h=\"http://www.w3.org/1999/xx/http#\""
            + "    xmlns:nx=\"http://www.nuxeo.org/document/uid/\">" + "    <r:Description>"
            + "      <r:type r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\" />"
            + "      <r:type r:resource=\"${uri}\" />" + "      <a:annotates r:resource=\"${annotates}\" /> "
            + "      <a:context>${context}</a:context> " + "      <a:body r:parseType=\"Literal\">${body}</a:body>"
            + "      ${fields}" + "      ${startContainer}" + "      ${endContainer}" + "    </r:Description></r:RDF> ";

    public AnnotationXmlGenerator(WebConfiguration webConfiguration, Annotation annotation) {
        this.webConfiguration = webConfiguration;
        this.annotation = annotation;
    }

    public String generateXml() {
        replaceURI();
        replaceXPointer();
        replaceAnnotate();
        replaceBody();
        replaceFields();
        replaceStartContainer();
        replaceEndContainer();
        return annotationXml;
    }

    private void replaceURI() {
        AnnotationDefinition annotationDefinition = webConfiguration.getAnnotationDefinition(annotation.getShortType());
        annotationXml = annotationXml.replace("${uri}", annotationDefinition.getUri());

    }

    private void replaceXPointer() {
        XPointer xpointer = annotation.getXpointer();
        annotationXml = annotationXml.replace("${context}", xpointer.getXpointerString());
    }

    private void replaceAnnotate() {
        String href = Window.Location.getHref();
        if (href.contains("?")) {
            annotationXml = annotationXml.replace("${annotates}", href.substring(0, href.indexOf('?')));
        } else {
            annotationXml = annotationXml.replace("${annotates}", href);
        }
    }

    private void replaceBody() {
        String encodedBody = AnnotationUtils.escapeHtml(annotation.getBody());
        annotationXml = annotationXml.replace("${body}", encodedBody);
    }

    private void replaceFields() {
        String fields = "";
        for (String fieldName : annotation.getFields().keySet()) {
            fields += "<nx:" + fieldName + " r:parseType=\"Literal\">" + annotation.getFields().get(fieldName)
                    + "</nx:" + fieldName + ">";
        }
        annotationXml = annotationXml.replace("${fields}", fields);
    }

    private void replaceStartContainer() {
        String replacement = "";
        if (annotation.hasStartContainer()) {
            replacement = "<nx:startContainer r:parseType=\"Literal\">"
                    + annotation.getStartContainer().generateString() + "</nx:startContainer>";

        }
        annotationXml = annotationXml.replace("${startContainer}", replacement);

    }

    private void replaceEndContainer() {
        String replacement = "";
        if (annotation.hasEndContainer()) {
            replacement = "<nx:endContainer r:parseType=\"Literal\">" + annotation.getEndContainer().generateString()
                    + "</nx:endContainer>";
        }
        annotationXml = annotationXml.replace("${endContainer}", replacement);
    }

}
