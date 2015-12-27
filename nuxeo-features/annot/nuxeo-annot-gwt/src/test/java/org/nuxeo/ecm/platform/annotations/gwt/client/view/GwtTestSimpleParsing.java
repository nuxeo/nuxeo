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

package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import org.nuxeo.ecm.platform.annotations.gwt.client.AbstractDocumentGWTTest;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;

import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author Alexandre Russel
 */
public class GwtTestSimpleParsing extends AbstractDocumentGWTTest {
    private AnnotatedDocument annotatedDocument = new AnnotatedDocument(null);

    public void testParse() {
        createDocument();
        for (Annotation annotation : getAnnotations(new String[] { "/HTML[0]/BODY[0]/DIV[0]/DIV[0]/DIV[0]/NOBR[0]/SPAN[0]/B[0],\"\",18,10" })) {
            annotatedDocument.decorate(annotation);
        }
        String resultInnerHtml = RootPanel.get("insidediv").getElement().getInnerHTML();
        assertTrue(resultInnerHtml.contains("<b>Nuxeo EP 5 - Nuxeo<span class=\"decorate decorate1\"> Annotatio</span>n</b></span></nobr></div"));
        resultInnerHtml = resultInnerHtml.replace(
                "<b>Nuxeo EP 5 - Nuxeo<span class=\"decorate decorate1\"> Annotatio</span>n</b></span></nobr></div", "");
        assertFalse(resultInnerHtml.contains("decorate1"));
    }

    public String getInnerHtml() {
        return new StringBuilder("<!-- Page 1 -->").append("<a name=\"1\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append(".ft0{font-size:30px;font-family:Helvetica;color:#000000;}").append(
                ".ft1{font-size:10px;font-family:Helvetica;color:#000000;}").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index001.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 290px; left: 59px;\"><nobr><span class=\"ft0\"><b>Nuxeo EP 5 - Nuxeo Annotation</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 329px; left: 242px;\"><nobr><span class=\"ft0\"><b>Service</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 691px; left: 199px;\"><nobr><span class=\"ft1\">Copyright © 2000-2007, Nuxeo SAS.</span></nobr></div>").append(
                "</div>").append("<!-- Page 2 -->").append("<a name=\"2\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append(".ft2{font-size:15px;font-family:Helvetica;color:#000000;}").append(
                ".ft3{font-size:10px;font-family:Times;color:#000000;}").append(
                ".ft4{font-size:10px;line-height:14px;font-family:Times;color:#000000;}").append("-->").append(
                "</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index002.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 83px; left: 51px;\"><nobr><span class=\"ft2\"><b>Table of Contents</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 112px; left: 51px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#4\">1. Requirements overview </a>.............................................................................................................1</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 127px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#4\">1.1. W3C Annotea </a>..................................................................................................................1<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#4\">1.2. Extensions to Annotea </a>....................................................................................................1</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 155px; left: 99px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#4\">1.2.1. URLs vs Document Ids </a>........................................................................................1<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#4\">1.2.2. Image annotation </a>..................................................................................................1<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#4\">1.2.3. Metadata management </a>.........................................................................................1<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#5\">1.2.4. Permission management </a>......................................................................................2</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 213px; left: 75px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#5\">1.3. Additionnal requirements </a>................................................................................................2</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 227px; left: 99px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#5\">1.3.1. Integration in Nuxeo preview system </a>..................................................................2<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#5\">1.3.2. Stand alone html client </a>.........................................................................................2</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 256px; left: 51px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#6\">2. Logical architecture overview </a>....................................................................................................3<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#8\">3. NXAS HTML Client </a>..................................................................................................................5</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 285px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#8\">3.1. Overview </a>.........................................................................................................................5<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#8\">3.2. Implementation </a>...............................................................................................................5</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 314px; left: 51px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#9\">4. Annotation Service Core </a>............................................................................................................6</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 328px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#9\">4.1. Overview </a>.........................................................................................................................6<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#9\">4.2. Implementation </a>...............................................................................................................6<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#9\">4.3. Storage </a>............................................................................................................................6<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#9\">4.4. Query Support </a>.................................................................................................................6<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#10\">4.5. Extension points </a>..............................................................................................................7</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 400px; left: 99px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#10\">4.5.1. urlResolver </a>...........................................................................................................7<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#10\">4.5.2. urlPatternFilter </a>.....................................................................................................7<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#10\">4.5.3. metadata </a>...............................................................................................................7<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#10\">4.5.4. permissionManager </a>..............................................................................................7<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#10\">4.5.5. annotabilityManager </a>............................................................................................7<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#11\">4.5.6. eventManager </a>.......................................................................................................8</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 487px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#11\">4.6. Event management </a>..........................................................................................................8<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#11\">4.7. URLs and Document </a>.......................................................................................................8<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#11\">4.8. XPointer extension </a>..........................................................................................................8</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 530px; left: 51px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#13\">5. Annotation Service Facade </a>......................................................................................................10</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 544px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#13\">5.1. Implementation </a>.............................................................................................................10<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#13\">5.2. JMS </a>...............................................................................................................................10<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#13\">5.3. Extension points </a>............................................................................................................10</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 587px; left: 99px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#13\">5.3.1. annotationsRules </a>................................................................................................10</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 602px; left: 51px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#15\">6. Annotation Service Javascript Library </a>.....................................................................................12</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 616px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#15\">6.1. overview </a>........................................................................................................................12<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#15\">6.2. Implementation </a>.............................................................................................................12<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#15\">6.3. RDF / JSON </a>..................................................................................................................12<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#15\">6.4. Annotating images </a>........................................................................................................12</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 674px; left: 51px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#16\">7. Annotation Service HTTP Gateway </a>........................................................................................13</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 688px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#16\">7.1. Overview </a>.......................................................................................................................13<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#16\">7.2. Implementation </a>.............................................................................................................13<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#16\">7.3. Authentication </a>...............................................................................................................13<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#16\">7.4. State management </a>.........................................................................................................13</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 746px; left: 51px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#17\">8. Integrating the Annotation service </a>...........................................................................................14</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 760px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#17\">8.1. Configure preview </a>........................................................................................................14<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#17\">8.2. Configure Annotation policy </a>........................................................................................14</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">ii</span></nobr></div>").append(
                "</div>").append("<!-- Page 3 -->").append("<a name=\"3\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index003.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 44px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#17\">8.3. Configure Annotation Meta-data </a>..................................................................................14<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#17\">8.4. Handle indexing </a>............................................................................................................14<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#17\">8.5. Integrate HTML/JS Client </a>............................................................................................14</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 88px; left: 51px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#18\">9. Packaging and deployment </a>......................................................................................................15</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 102px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#18\">9.1. Target platform </a>.............................................................................................................15<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#18\">9.2. Packaging </a>......................................................................................................................15</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 131px; left: 51px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#19\">10. W3C Annotea compliance </a>.....................................................................................................16</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 145px; left: 75px;\"><nobr><span class=\"ft4\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#19\">10.1. Annotea clients. </a>...........................................................................................................16<br><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#19\">10.2. Annotea Algae. </a>...........................................................................................................16</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 174px; left: 51px;\"><nobr><span class=\"ft3\"><a href=\"http://localhost:8080/nuxeo/restAPI/preview/default/a8322dc5-e360-4cb1-a676-2f3a32e2e3fa/default/index.html#20\">11. References </a>..............................................................................................................................17</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 16px; left: 200px;\"><nobr><span class=\"ft3\">Nuxeo EP 5 - Nuxeo Annotation Service</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 534px;\"><nobr><span class=\"ft3\">iii</span></nobr></div>").append(
                "</div>").append("<!-- Page 4 -->").append("<a name=\"4\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append(".ft5{font-size:19px;font-family:Helvetica;color:#000000;}").append(
                ".ft6{font-size:16px;font-family:Helvetica;color:#000000;}").append(
                ".ft7{font-size:13px;font-family:Helvetica;color:#000000;}").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index004.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 1. Requirements overview</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 95px; left: 51px;\"><nobr><span class=\"ft3\">This chapter provides an overview of the requirements for the Nuxeo Annotation Service.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 137px; left: 51px;\"><nobr><span class=\"ft6\"><b>1.1. W3C Annotea</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 172px; left: 51px;\"><nobr><span class=\"ft4\">Nuxeo Annotation service will be based on Annotea W3C specification. This basically means that<br>Nuxeo Annotation Service (NXAS) will be compliant with the specification published at<br>http://www.w3.org/2001/Annotea/User/Protocol.html .</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 242px; left: 51px;\"><nobr><span class=\"ft6\"><b>1.2. Extensions to Annotea</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 277px; left: 51px;\"><nobr><span class=\"ft4\">The annotea specification is quite old and does not handle all uses cases for annotating documents in<br>the context of Nuxeo ECM. Therefore, we propose to provide some extensions to annotea</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 327px; left: 51px;\"><nobr><span class=\"ft7\"><b>1.2.1. URLs vs Document Ids</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 359px; left: 51px;\"><nobr><span class=\"ft4\">The Annotea specification only deals with URLs. In the context of Nuxeo ECM it may be usefull to<br>track the relationship between a URL and a Document Id. Keeping this relation will be usefull for :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 396px; left: 70px;\"><nobr><span class=\"ft3\">• Managing versioning</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 422px; left: 82px;\"><nobr><span class=\"ft3\">It may be usefull to copy the annotations on a given version to the new version</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 449px; left: 70px;\"><nobr><span class=\"ft3\">• Managing Publication</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 475px; left: 82px;\"><nobr><span class=\"ft4\">Since Nuxeo ECM allow the usage of proxies the same document may be consulted via several<br>URls. With plain Annotea, a document and all it's proxies will have different URLs and then<br>different annotations. In most cases, this is not what we want.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 530px; left: 70px;\"><nobr><span class=\"ft3\">• Document Indexing</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 557px; left: 82px;\"><nobr><span class=\"ft4\">Some implementations of Nuxeo platform may want to index annotations as meta-data of the<br>document. This will require to be able to extract the annotations from the document UUID.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 607px; left: 51px;\"><nobr><span class=\"ft4\">For all these reasons, Nuxeo Annotation Service provides a plugable system to be able to keep track<br>of the relationship between a annotated URL and the underlying nuxeo document.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 657px; left: 51px;\"><nobr><span class=\"ft7\"><b>1.2.2. Image annotation</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 689px; left: 51px;\"><nobr><span class=\"ft4\">Inside Nuxeo EP we want to be able to annotate the HTML preview of a document. This preview may<br>include images. This images should be annotatble as the rest of the document, but it will require an<br>extension to the Xpointer specification since user may want to annotate just a portion (zone) of the<br>image.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 768px; left: 51px;\"><nobr><span class=\"ft7\"><b>1.2.3. Metadata management</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">1</span></nobr></div>").append(
                "</div>").append("<!-- Page 5 -->").append("<a name=\"5\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index005.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 56px; left: 51px;\"><nobr><span class=\"ft4\">Annotea specification includes very few meta-data. Inside Nuxeo we want the annotation to have a<br>pluggable meta-data schema so that each project implementation may add it's own set of meta-data.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 107px; left: 51px;\"><nobr><span class=\"ft7\"><b>1.2.4. Permission management</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 138px; left: 51px;\"><nobr><span class=\"ft4\">Annotea specification includes no permission management. Inside Nuxeo we require the permission to<br>be checked according to the global security policy.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 194px; left: 51px;\"><nobr><span class=\"ft6\"><b>1.3. Additionnal requirements</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 239px; left: 51px;\"><nobr><span class=\"ft7\"><b>1.3.1. Integration in Nuxeo preview system</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 270px; left: 51px;\"><nobr><span class=\"ft4\">The annotation service will provide an integration with the existing Nuxeo Html Preview system. The<br>goal of to use the Nuxeo preview tab to access the annotation feature.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 320px; left: 51px;\"><nobr><span class=\"ft7\"><b>1.3.2. Stand alone html client</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 352px; left: 51px;\"><nobr><span class=\"ft3\">A Standalone html client will be provided.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 16px; left: 240px;\"><nobr><span class=\"ft3\">Requirements overview</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">2</span></nobr></div>").append(
                "</div>").append("<!-- Page 6 -->").append("<a name=\"6\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append(".ft8{font-size:6px;font-family:Courier;color:#000000;}").append("-->").append(
                "</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index006.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 2. Logical architecture overview</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 95px; left: 51px;\"><nobr><span class=\"ft3\">This chapter provides an overview of the logical architecture of the Nuxeo Annotation service.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 118px; left: 70px;\"><nobr><span class=\"ft3\">• Annotation Service API</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 121px; left: 201px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-api</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 144px; left: 82px;\"><nobr><span class=\"ft4\">Provides Java API and all the needed Java artifacts needed to call the Annotation service<br>remotly.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 185px; left: 70px;\"><nobr><span class=\"ft3\">• Annotation Service Core</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 188px; left: 205px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-core</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 211px; left: 82px;\"><nobr><span class=\"ft4\">Provides the Nuxeo Service that exposes the required annotation Java Interface. This service<br>will be implemented as a Nuxeo Runtime component and will provide the needed extension<br>points .</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 266px; left: 70px;\"><nobr><span class=\"ft3\">• Annotation Service Facade</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 269px; left: 215px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-facade</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 293px; left: 82px;\"><nobr><span class=\"ft4\">Provides the service EJB3 facade for remote (RMI) access. Also includes integration with JMS<br>: sending JMS events and listen to events via MessageDrivenBeans.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 334px; left: 70px;\"><nobr><span class=\"ft3\">• Annotation Service Http gateway</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 337px; left: 246px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-restlets</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 360px; left: 82px;\"><nobr><span class=\"ft4\">The HTTP Gateway will implement the Annotea HTTP protocol. It will implemented as a set of<br>Restlets that will enable access to the annotation service via http GET/POST requests as defined<br>in the W3C Annotea specification.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 415px; left: 70px;\"><nobr><span class=\"ft3\">• Annotation Service JavaScript Client</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 418px; left: 264px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-js-client</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 442px; left: 82px;\"><nobr><span class=\"ft3\">The JS Library is the client part of the Annotea specification. It will manage :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 468px; left: 82px;\"><nobr><span class=\"ft3\">• communication with the http gateway</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 494px; left: 82px;\"><nobr><span class=\"ft3\">• extended XPointer resolution</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 521px; left: 82px;\"><nobr><span class=\"ft3\">• annotation display and edit</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 547px; left: 82px;\"><nobr><span class=\"ft3\">• ...</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 574px; left: 70px;\"><nobr><span class=\"ft3\">• Annotation Service preview plugin</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 577px; left: 254px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-preview-plugin</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 600px; left: 82px;\"><nobr><span class=\"ft4\">Access to annotations service will be available via the nuxeo preview service. This package will<br>integrate the annotation JS client into the html preview tab.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 641px; left: 70px;\"><nobr><span class=\"ft3\">• Annotation Service standalone client</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 644px; left: 262px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-html-client</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 667px; left: 82px;\"><nobr><span class=\"ft3\">This package will provide a simple stand-alone html client that embeds the JS client.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">3</span></nobr></div>").append(
                "</div>").append("<!-- Page 7 -->").append("<a name=\"7\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append(".ft9{font-size:10px;font-family:Times;color:#000000;}").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index007.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 442px; left: 51px;\"><nobr><span class=\"ft9\"><b>Figure 2.1. NXAS Components JPG</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 16px; left: 226px;\"><nobr><span class=\"ft3\">Logical architecture overview</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">4</span></nobr></div>").append(
                "</div>").append("<!-- Page 8 -->").append("<a name=\"8\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index008.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 3. NXAS HTML Client</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 110px; left: 51px;\"><nobr><span class=\"ft6\"><b>3.1. Overview</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 145px; left: 51px;\"><nobr><span class=\"ft4\">The NXAS HTML Client is the a web interface that can be used by the end user. It is a very simple<br>web application consisting of single html page. The page looks like a browser. It allows to enter URL<br>and show its content. The User can annotate places in the text or part of images, he can<br>see/add/remove/change annotations.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 215px; left: 51px;\"><nobr><span class=\"ft4\">This HTML client is very much like the annotation system integrated in the Nuxeo preview tab, but<br>without the need of Nuxeo EP.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 256px; left: 51px;\"><nobr><span class=\"ft4\">This HTML client will be usable in standalone mode outside of Nuxeo. In this standalone mode, the<br>user will have to enter : the url of the annotation service, and his login/password.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 312px; left: 51px;\"><nobr><span class=\"ft6\"><b>3.2. Implementation</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 347px; left: 51px;\"><nobr><span class=\"ft3\">Simple Html page with some JavaScript.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 373px; left: 51px;\"><nobr><span class=\"ft3\">The Html page displays 3 zones :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 396px; left: 70px;\"><nobr><span class=\"ft3\">• Server settings</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 422px; left: 82px;\"><nobr><span class=\"ft3\">This zone let the user define his login/password and the Annotation server base URL.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 449px; left: 70px;\"><nobr><span class=\"ft3\">• Page settings</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 475px; left: 82px;\"><nobr><span class=\"ft4\">This zone let the user enter the URL of the document he wants to annotate. A simple validation<br>button makes the third zone display the targeted page.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 516px; left: 70px;\"><nobr><span class=\"ft3\">• HTML Page display</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 542px; left: 82px;\"><nobr><span class=\"ft3\">Contains a IFRAME that displays the url selected by the user.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 569px; left: 82px;\"><nobr><span class=\"ft3\">Annotations tools will be available here.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 605px; left: 51px;\"><nobr><span class=\"ft4\">The Html Client uses the NXAS JS library to handle communication with the server and to manage<br>annotations on the targeted web page.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">5</span></nobr></div>").append(
                "</div>").append("<!-- Page 9 -->").append("<a name=\"9\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index009.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 4. Annotation Service Core</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 110px; left: 51px;\"><nobr><span class=\"ft6\"><b>4.1. Overview</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 145px; left: 51px;\"><nobr><span class=\"ft4\">This is the main component of NXAS, the one that contains all the logic for managing RDF based<br>annotations.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 186px; left: 51px;\"><nobr><span class=\"ft4\">This component is also responsible for exposing all the needed extension points that will be used for<br>configuration and integration.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 242px; left: 51px;\"><nobr><span class=\"ft6\"><b>4.2. Implementation</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 277px; left: 51px;\"><nobr><span class=\"ft4\">The service is implemented as a Runtime service on top of a Nuxeo Runtime component. The runtime<br>component will provide the extension point mechanisms. The API provided by the service will target<br>managing annotations on both URLs and documents.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 332px; left: 51px;\"><nobr><span class=\"ft3\">As any Nuxeo Service, the Annotation Service is accessible via the Runtime lookup method :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 358px; left: 59px;\"><nobr><span class=\"ft8\">Framework.getLocalService(AnnotationService.class)</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 407px; left: 51px;\"><nobr><span class=\"ft6\"><b>4.3. Storage</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 442px; left: 51px;\"><nobr><span class=\"ft3\">The Annotation service will store the annotations as a RDF graph.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 468px; left: 51px;\"><nobr><span class=\"ft4\">The Annotation service will contribute a new RDF Graph, a new set of RDF predicate and a new set<br>of resources adapters for the Nuxeo Relation Service. Nuxeo Relation Service is responsible for<br>storing and managing the RDF data.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 523px; left: 51px;\"><nobr><span class=\"ft3\">According to Annotea specifications, the felowing graphs and namespaces will be supported:</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 546px; left: 70px;\"><nobr><span class=\"ft3\">• http://www.w3.org/2000/10/annotation-ns</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 573px; left: 70px;\"><nobr><span class=\"ft3\">• http://www.w3.org/2001/03/thread</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 599px; left: 70px;\"><nobr><span class=\"ft3\">• http://www.w3.org/2001/12/replyType</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 623px; left: 51px;\"><nobr><span class=\"ft3\">These graphs will be extended and completed to support the extension to the specification :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 646px; left: 70px;\"><nobr><span class=\"ft3\">• XPointer extension to support Images</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 672px; left: 70px;\"><nobr><span class=\"ft3\">• Pluggable meta-data management</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 699px; left: 70px;\"><nobr><span class=\"ft3\">• Document vs URLs management</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 750px; left: 51px;\"><nobr><span class=\"ft6\"><b>4.4. Query Support</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 785px; left: 51px;\"><nobr><span class=\"ft3\">Even if Algae won't be implemented, NXAS will provide a Query API to retrive Annotations based on</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">6</span></nobr></div>").append(
                "</div>").append("<!-- Page 10 -->").append("<a name=\"10\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index010.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 56px; left: 51px;\"><nobr><span class=\"ft3\">criteria.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 83px; left: 51px;\"><nobr><span class=\"ft3\">Search criteria include :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 106px; left: 70px;\"><nobr><span class=\"ft3\">• target Document URL or DocumentLocation</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 132px; left: 70px;\"><nobr><span class=\"ft3\">• attributes of the annotation</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 158px; left: 70px;\"><nobr><span class=\"ft3\">• attributes of the author of the annotation</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 210px; left: 51px;\"><nobr><span class=\"ft6\"><b>4.5. Extension points</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 245px; left: 51px;\"><nobr><span class=\"ft3\">The runtime service will manage several exctension points in order to provide the needed flexibility.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 281px; left: 51px;\"><nobr><span class=\"ft7\"><b>4.5.1. urlResolver</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 312px; left: 51px;\"><nobr><span class=\"ft4\">The urlResolver extension point allows to contribute a class that is responsible for resolving url to<br>Document Location (repository name / document UUID).</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 353px; left: 51px;\"><nobr><span class=\"ft4\">This extension point is very important since it will provide the link between documents and URLs that<br>will be used for the versionning and proxy resolution.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 394px; left: 51px;\"><nobr><span class=\"ft4\">The default implementation will use the url codec service to try to do the translation. The preview<br>URL system will also be supported.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 444px; left: 51px;\"><nobr><span class=\"ft7\"><b>4.5.2. urlPatternFilter</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 475px; left: 51px;\"><nobr><span class=\"ft4\">The urlPatternFilter extension point allows to contribute regular expression pattern to the list of<br>allowed URL pattern or disallowed URL pattern. When a request is made to get/set annotations on an<br>URL, the server check the list. If the URL match a pattern on the disallowed list, then no action will<br>be possible. Then if the URL match a pattern on the allowed list, then annotation will be processed.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 554px; left: 51px;\"><nobr><span class=\"ft7\"><b>4.5.3. metadata</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 586px; left: 51px;\"><nobr><span class=\"ft4\">The metadata extension point allows to contribute class that provides metadata. The class implements<br>a simple interface. Its duty is to return a Map of metadata when being passed the annotation and<br>author name. The default implementation is to provide author, timestamp, annotation type and author<br>organizational unit.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 665px; left: 51px;\"><nobr><span class=\"ft7\"><b>4.5.4. permissionManager</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 696px; left: 51px;\"><nobr><span class=\"ft4\">The permissionManager extension point allows to contribute a class that will check viewAnnotation,<br>updateAnnotation and deleteAnnotation on URL. The default behavior is to map to the<br>view/update/delete permission on the corresponding document.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 761px; left: 51px;\"><nobr><span class=\"ft7\"><b>4.5.5. annotabilityManager</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 16px; left: 238px;\"><nobr><span class=\"ft3\">Annotation Service Core</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">7</span></nobr></div>").append(
                "</div>").append("<!-- Page 11 -->").append("<a name=\"11\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index011.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 56px; left: 51px;\"><nobr><span class=\"ft4\">The annotabilityManager extension point allows to contribute a class to fine grain which documents<br>can be annotated. Default implementation will allow simple filtring on Document facet 'Annotable'.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 107px; left: 51px;\"><nobr><span class=\"ft7\"><b>4.5.6. eventManager</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 138px; left: 51px;\"><nobr><span class=\"ft4\">The eventManager extension point allows to contribute a listener class handle annotation related<br>events.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 194px; left: 51px;\"><nobr><span class=\"ft6\"><b>4.6. Event management</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 229px; left: 51px;\"><nobr><span class=\"ft3\">The Annotation service will trigger several events associated to annotations :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 252px; left: 70px;\"><nobr><span class=\"ft3\">• annotationCreated</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 278px; left: 70px;\"><nobr><span class=\"ft3\">• annotationModified</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 305px; left: 70px;\"><nobr><span class=\"ft3\">• annotationDeleted</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 329px; left: 51px;\"><nobr><span class=\"ft4\">Each event will be trigger twice : once before the action and once after the action. Event listeners are<br>synchonous and have an interface that is very much like the core event listeners.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 385px; left: 51px;\"><nobr><span class=\"ft6\"><b>4.7. URLs and Document</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 420px; left: 51px;\"><nobr><span class=\"ft4\">The resolution between URLs and Document will rely in the contributed urlResolver plugins. A<br>built-in event listener will use this plugin to propagate annotations to the target document if url can be<br>resolved. As a single Nuxeo Document can have several preview URLs, the relation can not be a<br>bijection between a URL and a Document. The Bijection is between a URL and a tuple<br>(DocumentLocation, xpath of target field). Basically there will be a relation between a<br>(DocumentLocation, path) and the URL, the annotations will be linked to the URL and to the tuple<br>(DocumentLocation, path) (if available). This means than when retrieving annotation from an URL<br>the service will :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 543px; left: 70px;\"><nobr><span class=\"ft3\">• resolve URL to (DocumentLocation, path) if possible</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 570px; left: 70px;\"><nobr><span class=\"ft3\">• resolve proxy if DocumentLocation is a proxy</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 596px; left: 70px;\"><nobr><span class=\"ft3\">• get the annotations linked to this (DocumentLocation, path)</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 632px; left: 51px;\"><nobr><span class=\"ft4\">In most cases, a Nuxeo Document has only one preview URL associated to the default preview xpath<br>(see preview component doc).</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 688px; left: 51px;\"><nobr><span class=\"ft6\"><b>4.8. XPointer extension</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 723px; left: 51px;\"><nobr><span class=\"ft4\">The XPointer W3C specification is based on XPath and is made to identify a fragment of an html<br>document.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 764px; left: 51px;\"><nobr><span class=\"ft4\">The XPointer synthax will be extended to include the possibility to identify a shape inside a image<br>that is inside the HTML page. The NXPointer will typically contain the XPath to locate the image</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 16px; left: 238px;\"><nobr><span class=\"ft3\">Annotation Service Core</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">8</span></nobr></div>").append(
                "</div>").append("<!-- Page 12 -->").append("<a name=\"12\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index012.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 56px; left: 51px;\"><nobr><span class=\"ft3\">within the HTML document and a shape descriptor with pixel based values.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 16px; left: 238px;\"><nobr><span class=\"ft3\">Annotation Service Core</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 538px;\"><nobr><span class=\"ft3\">9</span></nobr></div>").append(
                "</div>").append("<!-- Page 13 -->").append("<a name=\"13\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index013.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 5. Annotation Service Facade</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 95px; left: 51px;\"><nobr><span class=\"ft4\">As for any Nuxeo Service, the facade will provide the integration of the service into JEE<br>infrastructure.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 136px; left: 51px;\"><nobr><span class=\"ft3\">This includes :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 158px; left: 70px;\"><nobr><span class=\"ft3\">• Remoting</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 185px; left: 70px;\"><nobr><span class=\"ft3\">• Transactions</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 211px; left: 70px;\"><nobr><span class=\"ft3\">• JMS</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 263px; left: 51px;\"><nobr><span class=\"ft6\"><b>5.1. Implementation</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 298px; left: 51px;\"><nobr><span class=\"ft3\">EJB3 Stateless Session Bean facade on top of the runtime service.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 324px; left: 51px;\"><nobr><span class=\"ft3\">This makes the Annotation Service remotable and accessible via the standard Nuxeo Lookup facility :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 350px; left: 59px;\"><nobr><span class=\"ft8\">Framework.getService(AnnotationService.class)</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 399px; left: 51px;\"><nobr><span class=\"ft6\"><b>5.2. JMS</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 433px; left: 51px;\"><nobr><span class=\"ft4\">The facade package also includes Listeners contributions to the core service in order to forward core<br>service events to the JMS topic.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 474px; left: 51px;\"><nobr><span class=\"ft4\">By default, all annotation related events are sent on the JMS topic : this can be useful for external<br>indexers, that may want to reindex the document when an annotation is createed or modified.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 515px; left: 51px;\"><nobr><span class=\"ft4\">The Facade will also host a MessageDrivenBean that will react to core document events (creation,<br>modification, delete). The behavior of this MDB will be configured via a dedicated Extension Point<br>(see below).</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 586px; left: 51px;\"><nobr><span class=\"ft6\"><b>5.3. Extension points</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 630px; left: 51px;\"><nobr><span class=\"ft7\"><b>5.3.1. annotationsRules</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 661px; left: 51px;\"><nobr><span class=\"ft4\">This extension point takes configuration that will be used by the MDB to define what must be done<br>depending on the event.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 702px; left: 51px;\"><nobr><span class=\"ft3\">This configuration binds actions to events. Events are :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 725px; left: 70px;\"><nobr><span class=\"ft3\">• documentModified</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 751px; left: 70px;\"><nobr><span class=\"ft3\">• documentVersionned</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 778px; left: 70px;\"><nobr><span class=\"ft3\">• documentPublished</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 532px;\"><nobr><span class=\"ft3\">10</span></nobr></div>").append(
                "</div>").append("<!-- Page 14 -->").append("<a name=\"14\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index014.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 56px; left: 70px;\"><nobr><span class=\"ft3\">• documentDeleted</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 83px; left: 70px;\"><nobr><span class=\"ft3\">• documentCopied</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 109px; left: 70px;\"><nobr><span class=\"ft3\">• documentLifeCycleChanged</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 133px; left: 51px;\"><nobr><span class=\"ft3\">Actions are :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 156px; left: 70px;\"><nobr><span class=\"ft3\">• duplicate</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 182px; left: 82px;\"><nobr><span class=\"ft4\">Duplicate annotation graph for the new document. Typically, this can be useful for document<br>versionning or document copy.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 223px; left: 70px;\"><nobr><span class=\"ft3\">• relink</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 250px; left: 82px;\"><nobr><span class=\"ft4\">Link the annotatation to the new document reference. Typically this can be usefull for<br>publishing.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 290px; left: 70px;\"><nobr><span class=\"ft3\">• delete</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 317px; left: 82px;\"><nobr><span class=\"ft3\">Remove the annotation graph. Typically, this can be usefull when document are deleted.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 343px; left: 70px;\"><nobr><span class=\"ft3\">• nop</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 370px; left: 82px;\"><nobr><span class=\"ft3\">Null operation</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 406px; left: 51px;\"><nobr><span class=\"ft4\">This configuration will let the MDB define what action must be done when an event occurs. In most<br>case, the same action could be done directly by the synchronous event listener in the NXAS Core<br>service. But for performances reason, it is safer to let these actions handled asynchronously via JMS<br>and MDB pooling.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 16px; left: 233px;\"><nobr><span class=\"ft3\">Annotation Service Facade</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 532px;\"><nobr><span class=\"ft3\">11</span></nobr></div>").append(
                "</div>").append("<!-- Page 15 -->").append("<a name=\"15\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append(".ft10{font-size:19px;line-height:26px;font-family:Helvetica;color:#000000;}").append(
                "-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index015.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft10\"><b>Chapter 6. Annotation Service Javascript<br>Library</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 137px; left: 51px;\"><nobr><span class=\"ft6\"><b>6.1. overview</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 172px; left: 51px;\"><nobr><span class=\"ft4\">This library offers a simple interface to interact with the NXAS Server. It allows to<br>create/read/update/delete annotations related to a URI, and to the URIs of the different version of the<br>corresponding document.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 227px; left: 51px;\"><nobr><span class=\"ft4\">This library also offers the UI functions to grab part of a document, including picture, and transform it<br>into URI using XPointer.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 283px; left: 51px;\"><nobr><span class=\"ft6\"><b>6.2. Implementation</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 318px; left: 51px;\"><nobr><span class=\"ft4\">Implementation will be in pure JavaScript. JQuery library will be used for DOM manipulation and the<br>JQuery Dialog toolkit will be used to managing UI.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 374px; left: 51px;\"><nobr><span class=\"ft6\"><b>6.3. RDF / JSON</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 409px; left: 51px;\"><nobr><span class=\"ft4\">The NXAS JS Library is dedciated to be used with Nuxeo Annotation service. Therefore, it does not<br>have to use RDF encoding. For optimization reason, the JS lib may use a JSON marshing rather than<br>plain RDF.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 479px; left: 51px;\"><nobr><span class=\"ft6\"><b>6.4. Annotating images</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 514px; left: 51px;\"><nobr><span class=\"ft4\">The Client JS library will handle image annotation and permit to define a shape (rectangle/elipse) in<br>the image that should hold the annotation. The shape will be handled as respect to the image size in<br>the HTML. If the HTML conversion flattern all images in the documents as one background image in<br>the HTML preview, then the annotation client will see only one image.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 532px;\"><nobr><span class=\"ft3\">12</span></nobr></div>").append(
                "</div>").append("<!-- Page 16 -->").append("<a name=\"16\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index016.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 7. Annotation Service HTTP Gateway</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 110px; left: 51px;\"><nobr><span class=\"ft6\"><b>7.1. Overview</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 145px; left: 51px;\"><nobr><span class=\"ft4\">The Annotation Service HTTP gateway provides the http API on top of the java APi of the Annotation<br>Service</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 201px; left: 51px;\"><nobr><span class=\"ft6\"><b>7.2. Implementation</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 236px; left: 51px;\"><nobr><span class=\"ft3\">The http gateway will be implemented as a set of restlets.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 263px; left: 51px;\"><nobr><span class=\"ft4\">The gateway package will also include serializer/deserializer for RDF and JSON. This allows the http<br>service to respond in RDF or JSON according to client requirements. The format parameter is simply<br>added to the standard Annota API, defaut format is RDF (for compliancy reasons).</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 318px; left: 51px;\"><nobr><span class=\"ft4\">The URL filting will be delegated to the underlying Nuxeo Service, this HTTP layer is only a gateway<br>and must handle as less logic as possible.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 374px; left: 51px;\"><nobr><span class=\"ft6\"><b>7.3. Authentication</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 409px; left: 51px;\"><nobr><span class=\"ft3\">The Annotation Restlet interface is guarded by the</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 412px; left: 296px;\"><nobr><span class=\"ft8\">NuxeoAuthenticationFilter</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 409px; left: 422px;\"><nobr><span class=\"ft3\">. This means that the</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 423px; left: 51px;\"><nobr><span class=\"ft3\">access via http to the annotation service requires a valid authentication.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 453px; left: 51px;\"><nobr><span class=\"ft8\">NuxeoAuthenticationFilter</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 450px; left: 180px;\"><nobr><span class=\"ft3\">will be configured (via it's EP) to let annotation related URLs be starting</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 464px; left: 51px;\"><nobr><span class=\"ft3\">URLs.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 490px; left: 51px;\"><nobr><span class=\"ft4\">Annonymous access to the annotation service is possible, in the Anonymous Authentication Plugin is<br>deployed.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 547px; left: 51px;\"><nobr><span class=\"ft6\"><b>7.4. State management</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 581px; left: 51px;\"><nobr><span class=\"ft4\">The restlets used to provide http access to the annotation service are stateless restlets.This means they<br>don't depend on Seam or JSF. Nevertheless, because of the Authentication system, client will have to<br>maintain a Session Cookie to avoid reauthenticating on each call.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 532px;\"><nobr><span class=\"ft3\">13</span></nobr></div>").append(
                "</div>").append("<!-- Page 17 -->").append("<a name=\"17\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index017.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 8. Integrating the Annotation service</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 95px; left: 51px;\"><nobr><span class=\"ft3\">Integrating the Nuxeo Annotation Service to your project may require several steps.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 137px; left: 51px;\"><nobr><span class=\"ft6\"><b>8.1. Configure preview</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 172px; left: 51px;\"><nobr><span class=\"ft4\">You can use Nuxeo Preview adpaters extension points to configure how the html previews of your<br>documents will be generated.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 212px; left: 51px;\"><nobr><span class=\"ft4\">Default implementation includes preview via transformers and preview based on pre-stored data.<br>Please refer to the according documentation.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 268px; left: 51px;\"><nobr><span class=\"ft6\"><b>8.2. Configure Annotation policy</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 303px; left: 51px;\"><nobr><span class=\"ft4\">The Annotation service exposes an Extension Point to define how annotations are managed during<br>versionning / publishing events. You can define your own configuration for that.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 359px; left: 51px;\"><nobr><span class=\"ft6\"><b>8.3. Configure Annotation Meta-data</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 394px; left: 51px;\"><nobr><span class=\"ft4\">You can change default meta-data associated to the annotations. For this you need to contribute a<br>simple java class.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 435px; left: 51px;\"><nobr><span class=\"ft3\">Default implementation includes : author, timestamp, annotation type and author company</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 477px; left: 51px;\"><nobr><span class=\"ft6\"><b>8.4. Handle indexing</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 512px; left: 51px;\"><nobr><span class=\"ft3\">Nuxeo default indexing does not handle Annotation indexing.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 538px; left: 51px;\"><nobr><span class=\"ft4\">Nevertheless, if you use an external indexer, you may use a MDB to listen to Annotation events and<br>trigger a reinddexation of the target document. This means that your indexing wrapper will contact the<br>Annotation service to fetch the annotations associated to the document.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 609px; left: 51px;\"><nobr><span class=\"ft6\"><b>8.5. Integrate HTML/JS Client</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 644px; left: 51px;\"><nobr><span class=\"ft3\">By default Nuxeo provides 2 clients : one integrated into the preview tab and one standalone.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 670px; left: 51px;\"><nobr><span class=\"ft4\">You may need to integrate the Preview in dedicated screens or to adapter the default screens to you<br>own graphic theme.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 532px;\"><nobr><span class=\"ft3\">14</span></nobr></div>").append(
                "</div>").append("<!-- Page 18 -->").append("<a name=\"18\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index018.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 9. Packaging and deployment</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 110px; left: 51px;\"><nobr><span class=\"ft6\"><b>9.1. Target platform</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 145px; left: 51px;\"><nobr><span class=\"ft3\">The logical target plaform is Nuxeo EP 5.2.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 172px; left: 51px;\"><nobr><span class=\"ft3\">The NXAS Addon will also be available on 5.1 branch (5.1.6).</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 213px; left: 51px;\"><nobr><span class=\"ft6\"><b>9.2. Packaging</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 248px; left: 51px;\"><nobr><span class=\"ft3\">NXAS will be packaged as a set of java artifacts that are plugins for Nuxeo EP.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 275px; left: 51px;\"><nobr><span class=\"ft3\">Java artifacts includes :</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 297px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 300px; left: 82px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-api</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 324px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 327px; left: 82px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-core</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 350px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 353px; left: 82px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-facade</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 377px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 380px; left: 82px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-restlets</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 403px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 406px; left: 82px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-preview-plugin</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 429px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 432px; left: 82px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-html-client</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 456px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 459px; left: 82px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-js-client</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 482px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 485px; left: 82px;\"><nobr><span class=\"ft8\">nuxeo-platform-annotations-tests</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 506px; left: 51px;\"><nobr><span class=\"ft3\">Some of these artifacts are resources only artifacts (for JS and HTML).</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 533px; left: 51px;\"><nobr><span class=\"ft4\">The support for NXAS inside the Nuxeo \"light packaging\" (Jetty bundle) is not directly targeted, but<br>this should not be a problem ouside of the facade artifact.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 532px;\"><nobr><span class=\"ft3\">15</span></nobr></div>").append(
                "</div>").append("<!-- Page 19 -->").append("<a name=\"19\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append("-->").append("</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index019.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 10. W3C Annotea compliance</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 95px; left: 51px;\"><nobr><span class=\"ft4\">As explained before, Annotea specification is not broad enought to include all Nuxeo EP requirements<br>for managing Annotations.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 151px; left: 51px;\"><nobr><span class=\"ft6\"><b>10.1. Annotea clients.</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 186px; left: 51px;\"><nobr><span class=\"ft3\">Nevertheless, NXAS will support Annotea basic clients like Mozilla Annozilla Annotea extension.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 212px; left: 51px;\"><nobr><span class=\"ft4\">Using a standard Annotea Client may retrain features : for examples additionnal metadata and Image<br>annotations won't be availables.</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 268px; left: 51px;\"><nobr><span class=\"ft6\"><b>10.2. Annotea Algae.</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 303px; left: 51px;\"><nobr><span class=\"ft4\">The Algae specification describe a Query Language that may be implemented by Annotea clients and<br>servers. NXAS won't implement Algae</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 532px;\"><nobr><span class=\"ft3\">16</span></nobr></div>").append(
                "</div>").append("<!-- Page 20 -->").append("<a name=\"20\"></a>").append(
                "<div style=\"position: relative; width: 595px; height: 842px;\">").append("<style type=\"text/css\">").append(
                "<!--").append(".ft11{font-size:10px;font-family:Times;color:#0000ff;}").append("-->").append(
                "</style>").append(
                "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index020.png\" alt=\"background image\" width=\"595\" height=\"842\">").append(
                "<div style=\"position: absolute; top: 58px; left: 51px;\"><nobr><span class=\"ft5\"><b>Chapter 11. References</b></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 91px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 91px; left: 82px;\"><nobr><span class=\"ft11\"><a href=\"http://www.w3.org/2001/Annotea/User/Protocol.html\"><i>W3C Annotea Protocols</i></a></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 118px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 118px; left: 82px;\"><nobr><span class=\"ft11\"><a href=\"http://doc.nuxeo.org/5.1/books/nuxeo-book/html-single/#nuxeo-platform-preview\"><i>Nuxeo preview addon</i></a></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 144px; left: 70px;\"><nobr><span class=\"ft3\">•</span></nobr></div>").append(
                "<div style=\"position: absolute; top: 144px; left: 82px;\"><nobr><span class=\"ft11\"><a href=\"http://www.w3.org/TR/xptr-framework/\"><i>XPointer</i></a></span></nobr></div>").append(
                "<div style=\"position: absolute; top: 816px; left: 532px;\"><nobr><span class=\"ft3\">17</span></nobr></div>").append(
                "</div>").append("<hr>").append("<a name=\"outline\"></a><h1>Document Outline</h1>").append(
                "<ul><li>��").append("</li><li>��").toString();
    }
}
