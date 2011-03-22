/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
public class ComponentDescriptorReader {

    private final XMap xmap;

    public ComponentDescriptorReader() {
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
        xmap.register(RegistrationInfoImpl.class);
    }

    public RegistrationInfoImpl read(RuntimeContext ctx, InputStream in) throws Exception {
        Object[] result = xmap.loadAll(new XMapContext(ctx), in);
        if (result.length > 0) {
            return (RegistrationInfoImpl) result[0];
        }
        return null;
    }

}
