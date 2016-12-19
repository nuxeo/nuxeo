/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */
package org.nuxeo.ecm.core.schema.types.primitives;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.types.PrimitiveType;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;

/**
 * The binary type handles values of type InputStream.
 */
public final class BinaryType extends PrimitiveType {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(BinaryType.class);

    public static final String ID = "binary";

    public static final BinaryType INSTANCE = new BinaryType();

    private BinaryType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        return true;
    }

    @Override
    public Object convert(Object value) {
        if (value instanceof CharSequence) {
            return new ByteArrayInputStream(value.toString().getBytes());
        } else if (value instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) value);
        } else if (value instanceof InputStream) {
            return value;
        }
        return null;
    }

    public static Object parseString(String str) {
        return new ByteArrayInputStream(str.getBytes());
    }

    protected Object readResolve() {
        return INSTANCE;
    }

    @Override
    public Object decode(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        return new ByteArrayInputStream(str.getBytes());
    }

    @Override
    public String encode(Object object) {
        if (object instanceof InputStream) {
            try (InputStream in = (InputStream) object){
                return IOUtils.toString(in, Charsets.UTF_8);
            } catch (IOException e) {
                log.error(e, e);
                return null;
            }
        }
        return object.toString();
    }

    @Override
    public boolean support(Class<? extends Constraint> constraint) {
        if (NotNullConstraint.class.equals(constraint)) {
            return true;
        }
        return false;
    }

}
