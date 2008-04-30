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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface RenderingContextView {

//    public final static ContextView EMPTY = new EmptyContextView();
//    public final static ContextView DEFAULT = new EmptyContextView();

    // Must be returned by get() method when the key is unknown since the caller should be able to
    // treat differently a key hit that returned null from a key that is not known by this view
    Object UNKNOWN = new Object();

    Object get(String key, RenderingContext ctx) throws Exception;

    Collection<String>  keys(RenderingContext ctx);

    int size(RenderingContext ctx);

}
