/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: MimetypeDescriptor.java 20874 2007-06-19 20:26:15Z sfermigier $
 */
package org.nuxeo.ecm.platform.mimetype.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.mimetype.MimetypeEntryImpl;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * MimetypeEntry extension definition.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("mimetype")
public class MimetypeDescriptor {

    @XNode("@normalized")
    protected String normalized;

    @XNode("@binary")
    protected boolean binary = true;

    @XNode("@onlineEditable")
    protected boolean onlineEditable = false;

    @XNode("@oleSupported")
    protected boolean oleSupported = false;

    @XNode("@iconPath")
    protected String iconPath;

    @XNode("mimetypes")
    protected Element mimetypes;

    @XNode("extensions")
    protected Element extensions;

    public boolean isBinary() {
        return binary;
    }

    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    public boolean isOnlineEditable() {
        return onlineEditable;
    }

    public void setOnlineEditable(boolean onlineEditable) {
        this.onlineEditable = onlineEditable;
    }

    public boolean isOleSupported() {
        return oleSupported;
    }

    public void setOleSupported(boolean oleSupported) {
        this.oleSupported = oleSupported;
    }

    public List<String> getExtensions() {
        List<String> exts = new ArrayList<>();
        NodeList elements = mimetypes.getElementsByTagName("extension");
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            exts.add(elements.item(i).getTextContent().trim());
        }
        return exts;
    }

    public void setExtensions(Element extensions) {
        this.extensions = extensions;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public List<String> getMimetypes() {
        List<String> mtypes = new ArrayList<>();
        NodeList elements = mimetypes.getElementsByTagName("mimetype");
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            mtypes.add(elements.item(i).getTextContent().trim());
        }
        return mtypes;
    }

    public void setMimetypes(Element mimetypes) {
        this.mimetypes = mimetypes;
    }

    public MimetypeEntry getMimetype() {
        return new MimetypeEntryImpl(normalized, getMimetypes(), getExtensions(), iconPath, binary, onlineEditable,
                oleSupported);
    }

    public String getNormalized() {
        return normalized;
    }

    public void setNormalized(String normalized) {
        this.normalized = normalized;
    }

}
