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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.XValueFactory;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ComponentDescriptorReader {

    private final XMap xmap;

    public ComponentDescriptorReader() {
        xmap = new XMap();
        xmap.deferClassLoading();
        xmap.setValueFactory(new XValueFactory<ComponentName>() {
            @Override
            public ComponentName deserialize(Context context, String value) {
                return new ComponentName(value);
            }

            @Override
            public String serialize(Context context, ComponentName value) {
                if (value != null) {
                    return value.toString();
                }
                return null;
            }
        });
        xmap.setValueFactory(new XValueFactory<Version>() {
            @Override
            public Version deserialize(Context context, String value) {
                return Version.parseString(value);
            }

            @Override
            public String serialize(Context context, Version value) {
                if (value != null) {
                    return value.toString();
                }
                return null;
            }
        });
        xmap.register(RegistrationInfoImpl.class);
    }

    public RegistrationInfoImpl[] read(AbstractRuntimeContext ctx, URL url) throws IOException {
        InputStream in = url.openStream();
        try {
            String source = org.apache.commons.io.IOUtils.toString(in, "UTF-8");
            String expanded = Framework.expandVars(source);
            InputStream bin = new ByteArrayInputStream(expanded.getBytes());
            try {
                RegistrationInfoImpl impls[] =  read(ctx, bin);
                for (RegistrationInfoImpl impl:impls) {
                    impl.xmlFileUrl = url;
                }
                return impls;
            } finally {
                bin.close();
            }
        } finally {
            in.close();
        }
    }

    public RegistrationInfoImpl[] read(AbstractRuntimeContext ctx, InputStream in) throws IOException {
        Object[] result = xmap.loadAll(new XMapContext(ctx), in);
        RegistrationInfoImpl[] impls = new RegistrationInfoImpl[result.length];
        for (int i = 0; i < result.length; ++i) {
            RegistrationInfoImpl impl = (RegistrationInfoImpl)result[i];
            handleNewInfo(ctx, impl);
            impls[i] = impl;
        }
        return impls;
    }

    protected void handleNewInfo(AbstractRuntimeContext context, RegistrationInfoImpl info) {
        // requires extended services
        for (Extension xt:info.getExtensions()) {
            ComponentName target = xt.getTargetComponent();
            if (!info.getName().equals(target)) {
                info.requires.add(xt.getTargetComponent());
            }
        }
        // set runtime context
        info.context = context;
        String name = info.getBundle();
        if (name != null) {
            // this is an external component XML.
            // should use the real owner bundle as the context.
            info.context = ((OSGiRuntimeService)context.getRuntime()).getContext(name);
        }

    }

    public void flushDeferred() {
        xmap.flushDeferred();
    }
}
