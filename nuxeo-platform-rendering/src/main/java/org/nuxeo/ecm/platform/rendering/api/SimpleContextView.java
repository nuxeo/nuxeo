/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.api;

import java.util.Collection;
import java.util.HashMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SimpleContextView extends HashMap<String,Object> implements RenderingContextView {

    private static final long serialVersionUID = -7297461509416300673L;

    public Object get(String key, RenderingContext ctx) {
        Object value = get(key);
        return value == null ? UNKNOWN : value;
    }

    public Collection<String> keys(RenderingContext ctx) {
        return keySet();
    }

    public int size(RenderingContext ctx) {
        return 0;
    }

}
