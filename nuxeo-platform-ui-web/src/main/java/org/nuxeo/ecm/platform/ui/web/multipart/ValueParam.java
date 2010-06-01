/*
 * JBoss, Home of Professional Open Source
 * Copyright ${year}, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.nuxeo.ecm.platform.ui.web.multipart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ValueParam extends Param {

    private Object value = null;

    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

    private final String encoding;

    public ValueParam(String name, String encoding) {
        super(name);
        this.encoding = encoding;
    }

    @SuppressWarnings("unchecked")
    public void complete() throws IOException {
        String val = this.encoding == null ? new String(buf.toByteArray())
                : new String(buf.toByteArray(), this.encoding);
        if (value == null) {
            value = val;
        } else {
            if (!(value instanceof List<?>)) {
                List<String> v = new ArrayList<String>();
                v.add((String) value);
                value = v;
            }

            ((List<String>) value).add(val);
        }
        buf.reset();
    }

    public Object getValue() {
        return value;
    }

    public void handle(byte[] bytes, int length) throws IOException {
        // read += length;
        buf.write(bytes, 0, length);
    }
}
