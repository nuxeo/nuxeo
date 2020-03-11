/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.XMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ContributionBuilder extends AbstractContribution {

    protected final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();

    protected final List<String> extensions;

    protected String bundle;

    public ContributionBuilder(String name) {
        super(name);
        this.extensions = new ArrayList<>();
    }

    @Override
    public URL asURL() {
        return null;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(boolean isDisabled) {
        this.disabled = isDisabled;
    }

    public void addXmlExtension(String target, String point, String content) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<extension target=\"")
          .append(target)
          .append("\" point=\"")
          .append(point)
          .append("\">\n")
          .append(content)
          .append("\n</extension>");
        extensions.add(sb.toString());
    }

    public void addExtension(String target, String point, Object... contribs) {
        if (contribs != null && contribs.length > 0) {
            addExtension(target, point, Arrays.asList(contribs));
        }
    }

    public void addExtension(String target, String point, List<Object> contribs) {
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbfac.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Document doc = docBuilder.newDocument();
        // create root element
        Element root = doc.createElement("extension");
        root.setAttribute("target", target);
        root.setAttribute("point", point);
        doc.appendChild(root);

        XMap xmap = new XMap();
        for (Object contrib : contribs) {
            xmap.register(contrib.getClass());
            xmap.toXML(contrib, root);
        }
        try {
            extensions.add(DOMSerializer.toStringOmitXml(root));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getContent() {
        StringBuilder sb = new StringBuilder(1024 * 32);
        sb.append("<component name=\"").append(ContributionPersistenceComponent.getComponentName(name)).append("\" ");
        if (bundle != null) {
            sb.append("bundle=\"").append(bundle).append("\" ");
        }
        sb.append(">\n\n");
        if (description != null) {
            sb.append("<documentation>\n").append(description).append("\n</documentation>\n\n");
        }
        for (String xt : extensions) {
            sb.append(xt).append("\n\n");
        }
        sb.append("</component>\n");
        return sb.toString();
    }

    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(getContent().getBytes());
    }

}
