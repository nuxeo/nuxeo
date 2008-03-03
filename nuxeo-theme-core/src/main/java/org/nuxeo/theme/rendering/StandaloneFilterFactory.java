/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.rendering;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.types.TypeFamily;

public final class StandaloneFilterFactory {

    public static StandaloneFilter create(String typeName) {
        StandaloneFilter filter = null;
        StandaloneFilterType filterType = (StandaloneFilterType) Manager.getTypeRegistry().lookup(
                TypeFamily.FILTER, typeName);
        try {
            filter = (StandaloneFilter) Thread.currentThread().getContextClassLoader().loadClass(
                    filterType.getClassName()).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filter;
    }

}
