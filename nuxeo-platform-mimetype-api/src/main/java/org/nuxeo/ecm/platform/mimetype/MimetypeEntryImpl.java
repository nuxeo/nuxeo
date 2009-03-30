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
 * $Id: MimetypeEntryImpl.java 21445 2007-06-26 14:47:16Z sfermigier $
 */

package org.nuxeo.ecm.platform.mimetype;

import java.util.List;

import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;

/**
 * MimetypeEntry.
 * <p>
 * A mimetype instance holds mimetype meta information.
 *
 * @see MimetypeEntry
 *
 * @author <a href="ja@nuxeo.com">Julien Anguenot</a>
 */
public class MimetypeEntryImpl implements MimetypeEntry {

    private static final long serialVersionUID = 5416098564564151631L;

    protected final List<String> extensions;

    protected final String iconPath;

    protected final boolean binary;

    protected final boolean onlineEditable;

    protected final boolean oleSupported;

    protected final List<String> mimetypes;

    protected final String normalized;

    public MimetypeEntryImpl(String normalized,
            List<String> mimetypes, List<String> extensions, String iconPath,
            boolean binary, boolean onlineEditable, boolean oleSupported) {
        this.normalized = normalized;
        this.mimetypes = mimetypes;
        this.extensions = extensions;
        this.iconPath = iconPath;
        this.binary = binary;
        this.onlineEditable = onlineEditable;
        this.oleSupported = oleSupported;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public String getIconPath() {
        return iconPath;
    }

    public List<String> getMimetypes() {
        return mimetypes;
    }

    public String getMajor() {
        return normalized.split("/")[0];
    }

    public String getMinor() {
        return normalized.split("/")[1];
    }

    public String getNormalized() {
        return normalized;
    }

    public boolean isBinary() {
        return binary;
    }

    public boolean isOnlineEditable() {
        return onlineEditable;
    }

    public boolean isOleSupported() {
        return oleSupported;
    }

}
