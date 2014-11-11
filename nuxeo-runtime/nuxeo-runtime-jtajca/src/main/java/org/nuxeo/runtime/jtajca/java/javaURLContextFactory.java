/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Ensures the installation of the Nuxeo JTA/JCA {@link NamingContext} by
 * {@link NuxeoContainer}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class javaURLContextFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
            Hashtable<?, ?> environment) throws Exception {
        if (obj != null || name != null || nameCtx != null) {
            throw new UnsupportedOperationException(
                    "This is case is not handled yet (see https://jira.nuxeo.com/browse/NXP-10331).");
        }
        return NuxeoContainer.getRootContext();
    }
}
