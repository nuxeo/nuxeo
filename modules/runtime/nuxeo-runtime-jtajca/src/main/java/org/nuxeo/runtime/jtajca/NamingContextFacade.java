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
 *   Stephane Lacoin
 */
package org.nuxeo.runtime.jtajca;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * Search main context if name not found in nuxeo's context.
 *
 * @since 5.6
 */
public class NamingContextFacade extends NamingContext {

    private static final long serialVersionUID = 1L;

    protected final Context delegate;

    public NamingContextFacade(Context delegate) throws NamingException {
        super();
        this.delegate = delegate;
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        try {
            return super.lookup(name);
        } catch (NamingException e) {
            return delegate.lookup(name);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Hashtable getEnvironment() {
        Hashtable env = super.getEnvironment();
        env.put("java.naming.factory.initial", NamingContextFactory.class.getName());
        env.put("java.naming.factory.url.pkgs", NuxeoContainer.class.getPackage().getName());
        return env;
    }

}
