package org.nuxeo.apidoc.documentation;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Visitor;
import org.dom4j.VisitorSupport;
import org.dom4j.io.SAXReader;

public class XMLContributionParser {

    public static String prettyfy(String xml) throws Exception {

        StringBuffer writter = new StringBuffer();

        SAXReader reader = new SAXReader();
        Document document = reader.read(new StringReader(xml));

        Element root = document.getRootElement();

        for (Iterator i = root.elementIterator(); i.hasNext();) {
            Element element = (Element) i.next();
            if (!element.getName().equals("documentation")) {
                ContributionItem fragment = parseContrib(element);
                fragment.write(writter);
            }
        }
        return writter.toString();
    }

    public static List<ContributionItem> extractContributionItems(String xml)
            throws Exception {

        List<ContributionItem> items = new ArrayList<ContributionItem>();

        SAXReader reader = new SAXReader();
        Document document = reader.read(new StringReader(xml));

        Element root = document.getRootElement();

        for (Iterator i = root.elementIterator(); i.hasNext();) {
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
            data = data.substring(0, data.length() - node.getName().length()
                    - 3);
        }
        data = data.trim();
        return data;
    }
}
