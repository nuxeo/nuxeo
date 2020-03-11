/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.runtime.jtajca.java;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.nuxeo.runtime.jtajca.NamingContext;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * Ensures the installation of the Nuxeo JTA/JCA {@link NamingContext} by {@link NuxeoContainer}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class javaURLContextFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) {
        if (obj != null || name != null || nameCtx != null) {
            throw new UnsupportedOperationException(
                    "This is case is not handled yet (see https://jira.nuxeo.com/browse/NXP-10331).");
        }
        return NuxeoContainer.getRootContext();
    }
}
