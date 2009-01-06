package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


public class HtmlHandler extends Xml2TextHandler {

    public HtmlHandler() throws SAXException, ParserConfigurationException {
        super();
    }

    public String getText() {
        return filterAndJoin(buf.toString());
    }
    private String filterAndJoin(String text) {
        boolean space = false;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if ((c == '\n') || (c == ' ') || Character.isWhitespace(c)) {
                if (space) {
                    continue;
                } else {
                    space = true;
                    buffer.append(' ');
                    continue;
                }
            } else {
                if (!Character.isLetter(c) && !Character.isDigit(c)) {
                    if (!space) {
                        space = true;
                        buffer.append(' ');
                        continue;
                    }
                    continue;
                }
            }
            space = false;
            buffer.append(c);
        }
        return buffer.toString();
    }


}
