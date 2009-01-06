package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OOoXmlContentHandler extends DefaultHandler {

      private StringBuffer sb;
      private boolean dumpText;

      public OOoXmlContentHandler() {
          sb = new StringBuffer();
          dumpText = false;
      }

      public String getContent() {
          return sb.toString();
      }

      public void startElement(String namespaceURI, String localName,
                               String rawName, Attributes atts)
              throws SAXException {
          if (rawName.startsWith("text:")) {
              dumpText = true;
          }
      }

      public void characters(char[] ch, int start, int length)
              throws SAXException {
          if (dumpText) {
              sb.append(ch, start, length);
              sb.append(" ");
          }
      }

      public void endElement(java.lang.String namespaceURI,
                             java.lang.String localName,
                             java.lang.String qName)
              throws SAXException {
          dumpText = false;
      }
}
