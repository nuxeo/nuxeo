/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
        List<String> exts = new ArrayList<String>();
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
        List<String> mtypes = new ArrayList<String>();
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
        return new MimetypeEntryImpl(normalized,
                getMimetypes(), getExtensions(), iconPath, binary,
                onlineEditable, oleSupported);
    }

    public String getNormalized() {
        return normalized;
    }

    public void setNormalized(String normalized) {
        this.normalized = normalized;
    }

}
