/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Benjamin JALON
 */

package org.nuxeo.runtime.model.impl;

import java.net.URL;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class XMapContext extends Context {

    private static final long serialVersionUID = -7194560385886298218L;

    final RuntimeContext ctx;

    public XMapContext(RuntimeContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        if (className.startsWith("[")) {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        }
        return ctx.loadClass(className);
    }

    @Override
    public URL getResource(String name) {
        return ctx.getResource(name);
    }

}
