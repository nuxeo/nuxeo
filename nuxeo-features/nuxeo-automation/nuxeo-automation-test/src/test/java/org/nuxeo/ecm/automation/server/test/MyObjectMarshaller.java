/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.test;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

/**
 * TODO must use ObjectCodec on client too.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MyObjectMarshaller implements JsonMarshaller<MyObject> {

    @Override
    public Class<MyObject> getJavaType() {
        return MyObject.class;
    }

    @Override
    public String getType() {
        return "msg";
    }

    @Override
    public MyObject read(JsonParser jp) throws Exception {
        if (jp.getCodec() == null) {
            jp.setCodec(new ObjectMapper());
        }
        jp.nextValue();
        return jp.readValueAs(MyObject.class);
    }

    public void write(JsonGenerator jg, Object value) throws Exception {
        throw new UnsupportedOperationException();
    }

}
