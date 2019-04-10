/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.documentation;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Visitor;
import org.dom4j.VisitorSupport;
import org.dom4j.io.SAXReader;

public class XMLContributionParser {

    public static String prettyfy(String xml) throws DocumentException {

        StringBuilder writter = new StringBuilder();

        SAXReader reader = new SAXReader();
        Document document = reader.read(new StringReader(xml));

        Element root = document.getRootElement();

        for (Iterator<?> i = root.elementIterator(); i.hasNext();) {
            Element element = (Element) i.next();
            if (!element.getName().equals("documentation")) {
                ContributionItem fragment = parseContrib(element);
                fragment.write(writter);
            }
        }
        return writter.toString();
    }

    public static List<ContributionItem> extractContributionItems(String xml) throws DocumentException {

        List<ContributionItem> items = new ArrayList<>();

        SAXReader reader = new SAXReader();
        Document document = reader.read(new StringReader(xml));

        Element root = document.getRootElement();

        for (Iterator<?> i = root.elementIterator(); i.hasNext();) {
            Element element = (Element) i.next();
            if (!element.getName().equals("documentation")) {
                ContributionItem fragment = parseContrib(element);
                items.add(fragment);
            }
        }
        return items;
    }

    protected static ContributionItem parseContrib(Element element) {

        final ContributionItem fragment = new ContributionItem();
        fragment.tagName = element.getName();

        fragment.nameOrId = element.attributeValue("name");
        if (fragment.nameOrId == null) {
            fragment.nameOrId = element.attributeValue("id");
        }

        Visitor docExtractor = new VisitorSupport() {
            @Override
            public void visit(Element node) {
                if ("documentation".equalsIgnoreCase(node.getName())) {
                    fragment.documentation = getNodeAsString(node);
                } else if ("description".equalsIgnoreCase(node.getName())) {
                    fragment.documentation = getNodeAsString(node);
                } else {
                    super.visit(node);
                }
            }
        };
        element.accept(docExtractor);

        fragment.xml = element.asXML();

        return fragment;
    }

    protected static String getNodeAsString(Element node) {
        String data = node.asXML();
        data = data.substring(node.getName().length() + 2);
        if (data.length() > node.getName().length()) {
            data = data.substring(0, data.length() - node.getName().length() - 3);
        }
        data = data.trim();
        return data;
    }
}
