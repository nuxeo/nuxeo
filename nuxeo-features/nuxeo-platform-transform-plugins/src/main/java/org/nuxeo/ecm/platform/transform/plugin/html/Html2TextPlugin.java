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

package org.nuxeo.ecm.platform.transform.plugin.html;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * 
 */
public class Html2TextPlugin extends AbstractPlugin {

    private static final String TEXT_MIME_TYPE = "text/plain";

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        final List<TransformDocument> results = super.transform(options,
                sources);

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        HtmlParser parser = new HtmlParser();

        for (TransformDocument t : sources) {
            SAXResult result = new SAXResult(new DefaultHandler());

            SAXSource source = new SAXSource(parser, new InputSource(
                    t.getBlob().getStream()));
            transformer.transform(source, result);

            results.add(new TransformDocumentImpl(new StringBlob(
                    parser.getContents(), TEXT_MIME_TYPE)));
        }

        return results;
    }

}
