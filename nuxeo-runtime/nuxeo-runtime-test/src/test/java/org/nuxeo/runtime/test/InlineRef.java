/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.runtime.model.StreamRef;
import org.nuxeo.runtime.test.protocols.inline.InlineURLFactory;

public class InlineRef implements StreamRef {

    protected final String id;
    protected final String content;

    public InlineRef(String id, String content) {
        this.id = id;
        this.content = content;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public InputStream getStream() throws IOException {
        return new ByteArrayInputStream(content.getBytes());
    }

    @Override
    public URL asURL() {
        try {
            return InlineURLFactory.newURL(content);
        } catch (Exception e) {
            throw new Error("Cannot encode inline:... URL", e);
        }
    }

}
