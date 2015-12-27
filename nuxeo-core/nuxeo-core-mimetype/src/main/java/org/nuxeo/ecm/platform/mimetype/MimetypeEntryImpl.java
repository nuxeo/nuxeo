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

    public MimetypeEntryImpl(String normalized, List<String> mimetypes, List<String> extensions, String iconPath,
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
