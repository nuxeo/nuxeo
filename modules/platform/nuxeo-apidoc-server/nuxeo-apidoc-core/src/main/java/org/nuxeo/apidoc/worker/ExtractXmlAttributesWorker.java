/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.apidoc.worker;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.apidoc.listener.AttributesExtractorStater;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
public class ExtractXmlAttributesWorker extends AbstractWork {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = "apidoc-xml-extractor";

    protected ExtractXmlAttributesWorker(String repositoryName, String docId) {
        super(String.format("%s:%s:xml:extractor", repositoryName, docId));
        setDocument(repositoryName, docId);
    }

    public ExtractXmlAttributesWorker(String repositoryName, String originatingUsername, String docId) {
        this(repositoryName, docId);
        setOriginatingUsername(originatingUsername);
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public void work() {
        setStatus("Extracting");
        openSystemSession();

        try {
            DocumentModel doc = loadDocument();
            BlobHolder adapter = doc.getAdapter(BlobHolder.class);
            String attributes = extractAttributes(adapter.getBlob());
            doc.setPropertyValue(AttributesExtractorStater.ATTRIBUTES_PROPERTY, attributes);

            session.saveDocument(doc);

            setStatus("Done");
        } catch (DocumentNotFoundException cause) {
        } catch (IOException | ParserConfigurationException | SAXException e) {
            setStatus("Failed");
            throw new NuxeoException(e);
        }
    }

    protected DocumentModel loadDocument() throws DocumentNotFoundException {
        final DocumentRef docRef = getDocument().getDocRef();
        DocumentModel doc = session.getDocument(docRef);
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        return doc;
    }

    public String extractAttributes(Blob blob) throws ParserConfigurationException, SAXException, IOException {
        if (blob == null) {
            return null;
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        Set<String> attributes = new HashSet<>();
        saxParser.parse(blob.getStream(), new Handler(attributes));

        return StringUtils.join(attributes, ' ');
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return "XML Attributes extractor for fulltext search";
    }

    protected static class Handler extends DefaultHandler {
        private Set<String> attributesSet;

        public Handler(Set<String> attributesSet) {
            this.attributesSet = attributesSet;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            IntStream.range(0, attributes.getLength()).forEach(i -> attributesSet.add(attributes.getValue(i)));
        }
    }

}
