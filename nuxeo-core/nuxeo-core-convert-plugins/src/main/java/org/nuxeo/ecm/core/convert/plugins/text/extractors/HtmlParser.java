/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.AbstractSAXParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLString;
import org.cyberneko.html.HTMLConfiguration;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class HtmlParser extends AbstractSAXParser {

    private static final Log log = LogFactory.getLog(HtmlParser.class);

    protected StringBuilder buffer;

    protected String tagFilter;

    protected Boolean inFilter;

    protected Boolean noFilter;

    protected String skipUntillClosed;

    protected final Set<String> newLinesTags = new HashSet<String>();

    protected final Set<String> skippedTags = new HashSet<String>();

    public HtmlParser() {
        super(new HTMLConfiguration());
        init(null);
    }

    public HtmlParser(String tagFilter) {
        super(new HTMLConfiguration());
        init(tagFilter);
    }

    public void init(String tagFilter) {
        try {
            // make sure we do not download the DTD URI
            setFeature("http://xml.org/sax/features/validation", false);
            setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
        } catch (SAXException e) {
            log.debug("Could not switch parser to non-validating: "
                    + e.getMessage());
        }
        inFilter = false;
        if (tagFilter == null || "".equals(tagFilter)) {
            noFilter = true;
        } else {
            this.tagFilter = tagFilter;
            noFilter = false;
        }

        // initialize the skipped tags
        skippedTags.add("script");
        skippedTags.add("style");
        skippedTags.add("link");

        // initialize the new line tags
        newLinesTags.add("div");
        newLinesTags.add("p");
        newLinesTags.add("br");
        newLinesTags.add("pre");
        newLinesTags.add("h1");
        newLinesTags.add("h2");
        newLinesTags.add("h3");
        newLinesTags.add("h4");
        newLinesTags.add("h5");
        newLinesTags.add("h6");
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes,
            Augmentations augs) {
        super.startElement(element, attributes, augs);
        if (!noFilter && tagFilter.equalsIgnoreCase(element.localpart)) {
            inFilter = true;
        }
        if (skipUntillClosed == null
                && skippedTags.contains(element.localpart.toLowerCase())) {
            // this strategy won't work for nested skipped tags but this is not
            // the case for script and style tags. If we want to add support for
            // nested skipped tags we should use a stack instead
            skipUntillClosed = element.localpart.toLowerCase();
        }
    }

    @Override
    public void endElement(QName element, Augmentations augs) {
        super.endElement(element, augs);
        if (!noFilter && tagFilter.equals(element.localpart)) {
            inFilter = false;
        }
        if (skipUntillClosed != null
                && skipUntillClosed.equals(element.localpart.toLowerCase())) {
            skipUntillClosed = null;
        }
        if (newLinesTags.contains(element.localpart.toLowerCase())) {
            buffer.append("\n\n");
        }
    }

    @Override
    public void startDocument(XMLLocator arg0, String arg1,
            NamespaceContext arg2, Augmentations arg3) {
        super.startDocument(arg0, arg1, arg2, arg3);
        buffer = new StringBuilder();
    }

    @Override
    public void characters(XMLString xmlString, Augmentations augmentations) {
        super.characters(xmlString, augmentations);
        if ((noFilter || inFilter) && skipUntillClosed == null) {
            buffer.append(xmlString.toString());
        }
    }

    /**
     * @return the parsed content (as a String).
     */
    public String getContents() {
        // remove trailing space, multiple consecutive spaces and multiple
        // consecutive new lines (except double new lines that can be used to
        // mark new paragraphs).
        String text = buffer.toString();
        text = text.replaceAll(" *\n", "\n"); // clean trailing spaces
        text = text.replaceAll(" +", " "); // clean multiple spaces
        text = text.replaceAll("\\n\\n+", "\n\n"); // clean multiple lines
        return text;

    }

}
