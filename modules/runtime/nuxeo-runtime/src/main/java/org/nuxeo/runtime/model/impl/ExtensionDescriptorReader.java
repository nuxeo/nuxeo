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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.model.impl;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.XValueFactory;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ExtensionDescriptorReader {

    protected final XMap xmap;

    public ExtensionDescriptorReader() {
        xmap = new XMap();
        xmap.setValueFactory(ComponentName.class, new XValueFactory() {
            @Override
            public Object deserialize(Context context, String value) {
                return new ComponentName(value);
            }

            @Override
            public String serialize(Context context, Object value) {
                if (value != null) {
                    return value.toString();
                }
                return null;
            }
        });
        xmap.setValueFactory(Version.class, new XValueFactory() {

            @Override
            public Object deserialize(Context context, String value) {
                return Version.parseString(value);
            }

            @Override
            public String serialize(Context context, Object value) {
                if (value != null) {
                    return value.toString();
                }
                return null;
            }
        });
        xmap.register(ExtensionImpl.class);
    }

    public ExtensionImpl read(RuntimeContext ctx, InputStream in) throws IOException {
        Object[] result = xmap.loadAll(new XMapContext(ctx), in);
        if (result.length > 0) {
            return (ExtensionImpl) result[0];
        }
        return null;
    }

    public XMap getXMap() {
        return xmap;
    }

}
