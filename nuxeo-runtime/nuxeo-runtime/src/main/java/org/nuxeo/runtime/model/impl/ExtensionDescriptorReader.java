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
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.io.InputStream;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.XValueFactory;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
                if ( value != null ) {
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
                if ( value != null ) {
                    return value.toString();
                }
                return null;
            }
        });
        xmap.register(ExtensionImpl.class);
    }

    public ExtensionImpl read(RuntimeContext ctx, InputStream in) throws Exception {
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
