/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.XMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContributionBuilder extends AbstractContribution {

    protected final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();

    protected final List<String> extensions;

    protected String bundle;

    public ContributionBuilder(String name) {
        super(name);
        this.extensions = new ArrayList<String>();
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

    public void addXmlExtension(String target, String point, String content)
            throws Exception {
        StringBuilder buf = new StringBuilder(1024);
        buf.append(
                "<extension target=\"" + target + "\" point=\"" + point
                        + "\">\n").append(content).append("\n</extension>");
        extensions.add(buf.toString());
    }

    public void addExtension(String target, String point, Object... contribs)
            throws Exception {
        if (contribs != null && contribs.length > 0) {
            addExtension(target, point, Arrays.asList(contribs));
        }
    }

    public void addExtension(String target, String point, List<Object> contribs)
            throws Exception {
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
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
        extensions.add(DOMSerializer.toStringOmitXml(root));
    }

    @Override
    public String getContent() {
        StringBuilder buf = new StringBuilder(1024 * 32);
        buf.append("<component name=\"").append(
                ContributionPersistenceComponent.getComponentName(name)).append(
                "\" ");
        if (bundle != null) {
            buf.append("bundle=\"").append(bundle).append("\" ");
        }
        buf.append(">\n\n");
        if (description != null) {
            buf.append("<documentation>\n").append(description).append(
                    "\n</documentation>\n\n");
        }
        for (String xt : extensions) {
            buf.append(xt).append("\n\n");
        }
        buf.append("</component>\n");
        return buf.toString();
    }

    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(getContent().getBytes());
    }

}
