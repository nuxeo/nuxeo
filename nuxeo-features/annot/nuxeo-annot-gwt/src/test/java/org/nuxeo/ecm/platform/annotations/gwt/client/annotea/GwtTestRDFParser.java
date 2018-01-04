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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

/**
 * @author Alexandre Russel
 */
public class GwtTestRDFParser extends GWTTestCase {
    private final RDFParser parser = new RDFParser();

    @Override
    public String getModuleName() {
        return "org.nuxeo.ecm.platform.annotations.gwt.AnnotationModule";
    }

    public void testGetAnnotationList() {
        assertNotNull(parser);
        assertNotNull(response);
        List<Annotation> annotations = parser.getAnnotationList(response);
        assertNotNull(annotations);
        assertEquals(3, annotations.size());
    }

    public void testGetAnnotation() {
        Document document = XMLParser.parse(annotation);
        assertNotNull(document);
        NodeList list = document.getChildNodes().item(0).getChildNodes();
        assertNotNull(list);
        Annotation annotation = parser.getAnnotation(null, list);
        assertNotNull(annotation);
        assertEquals("http://www.w3.org/2000/10/annotationType#Comment", annotation.getType());
        // assertEquals("http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[2])", annotation.getXpointer());
        assertEquals("http://annotea.example.org/Annotation/body/3ACF6D754", annotation.getBody());
    }

    public void testGetAnnotationWithBodyLiteral() {
        Document document = XMLParser.parse(bodyLiteralAnnotation);
        assertNotNull(document);
        NodeList list = document.getChildNodes().item(0).getChildNodes();
        assertNotNull(list);
        Annotation annotation = parser.getAnnotation(null, list);
        assertFalse(annotation.isBodyUrl());
        assertEquals("fqsdfqsdf", annotation.getBody());

    }

    private final String bodyLiteralAnnotation = "<?xml version=\"1.0\" ?> \n"
            + " <r:Description xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n"
            + "           xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" \n"
            + "           xmlns:d=\"http://purl.org/dc/elements/1.1/\""
            + "           r:about=\"http://localhost:/10f3ce5081dd35c3\">"
            + "     <r:type r:resource=\"http://www.w3.org/2000/10/annotation-ns#Comment\"/>"
            + "     <a:annotates r:resource=\"http://localhost:8080/nuxeo/nxdoc/default/f6c3a8c3-427f-40fc-a0a0-e7630c41fdce/view_documents?tabId=TAB_PREVIEW\"/>"
            + "     <d:title>qsdfqs</d:title>"
            + "     <a:context>http://localhost:8080/nuxeo/nxdoc/default/f6c3a8c3-427f-40fc-a0a0-e7630c41fdce/#xpointer(string-range(/HTML[1]/BODY[0]/DIV[0]/DIV[0]/NOBR[0]/SPAN[0],\"\",12,4))</a:context>"
            + "     <a:body r:datatype=\"http://www.w3.org/1999/02/22-r-syntax-ns#XMLLiteral\">fqsdfqsdf</a:body>"
            + "     <r:type r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\"/>"
            + "   </r:Description>";

    private final String annotation = "    <r:Description xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "    xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" "
            + "    xmlns:d=\"http://purl.org/dc/elements/1.1/\" >" + "        <r:type "
            + "            r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\" /> " + "        <r:type "
            + "            r:resource=\"http://www.w3.org/2000/10/annotationType#Comment\" /> "
            + "        <a:annotates " + "       r:resource=\"http://serv1.example.com/some/page.html\" />"
            + "        <d:title>Annotation of Sample Page 1</d:title>" + "        <a:context>"
            + "            http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[2])"
            + "        </a:context>" + "        <d:creator>Ralph Swick</d:creator>"
            + "        <a:created>1999-10-14T12:10Z</a:created>" + "        <d:date>1999-10-14T12:10Z</d:date>"
            + "        <a:body" + "            r:resource=\"http://annotea.example.org/Annotation/body/3ACF6D754\" />"
            + "    </r:Description>";

    private final String response = "<?xml version=\"1.0\" ?> "
            + " <r:RDF xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "    xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" "
            + "    xmlns:d=\"http://purl.org/dc/elements/1.1/\"> " + "    <r:Description "
            + "        r:about=\"http://annotea.example.org/Annotation/3ACF6D756\"> " + "        <r:type "
            + "            r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\" /> " + "        <r:type "
            + "            r:resource=\"http://www.w3.org/2000/10/annotationType#Comment\" /> "
            + "        <a:annotates " + "       r:resource=\"http://serv1.example.com/some/page.html\" />"
            + "        <d:title>Annotation of Sample Page 1</d:title>" + "        <a:context>"
            + "            http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[2])"
            + "        </a:context>" + "        <d:creator>Ralph Swick</d:creator>"
            + "        <a:created>1999-10-14T12:10Z</a:created>" + "        <d:date>1999-10-14T12:10Z</d:date>"
            + "        <a:body" + "            r:resource=\"http://annotea.example.org/Annotation/body/3ACF6D754\" />"
            + "    </r:Description>" + "    <r:Description"
            + "        r:about=\"http://annotea.example.org/Annotation/3ACF6D757\">" + "        <r:type"
            + "            r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\" />" + "        <r:type"
            + "            r:resource=\"http://www.w3.org/2000/10/annotationType#Comment\" />" + "        <a:annotates"
            + "            r:resource=\"http://serv1.example.com/some/page.html\" />"
            + "        <d:title>Annotation of Sample Page 2</d:title>" + "        <a:context>"
            + "            http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[3])"
            + "        </a:context>" + "        <d:creator>Ralph Swick</d:creator>"
            + "        <a:created>1999-10-14T12:10Z</a:created>" + "        <d:date>1999-10-14T12:10Z</d:date>"
            + "        <a:body" + "            r:resource=\"http://annotea.example.org/Annotation/body/3ACF6D754\" />"
            + "    </r:Description>" + "    <r:Description"
            + "        r:about=\"http://annotea.example.org/Annotation/3ACF6D758\">" + "        <r:type"
            + "            r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\" />" + "        <r:type"
            + "            r:resource=\"http://www.w3.org/2000/10/annotationType#Comment\" />" + "        <a:annotates"
            + "            r:resource=\"http://serv1.example.com/some/page.html\" />"
            + "        <d:title>Annotation of Sample Page 3</d:title>" + "        <a:context>"
            + "            http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[4])"
            + "        </a:context>" + "        <d:creator>Ralph Swick</d:creator>"
            + "        <a:created>1999-10-14T12:10Z</a:created>" + "        <d:date>1999-10-14T12:10Z</d:date>"
            + "        <a:body" + "            r:resource=\"http://annotea.example.org/Annotation/body/3ACF6D754\" />"
            + "    </r:Description>" + "</r:RDF>";
}
