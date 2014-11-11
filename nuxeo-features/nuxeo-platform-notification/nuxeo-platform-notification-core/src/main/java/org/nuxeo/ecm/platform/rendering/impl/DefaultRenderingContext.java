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
 *     dragos
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.impl;

import java.util.HashMap;

import org.nuxeo.ecm.platform.rendering.RenderingContext;
import org.nuxeo.ecm.platform.rendering.RenderingEngine;

/**
 * Abstract RenderingContext subclass that adds capabilities of storing a
 * DocumentModel and retrieve RenderingConfig associated with the current set
 * DocumentModel.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public class DefaultRenderingContext extends HashMap<String, Object>
        implements RenderingContext {

    private static final long serialVersionUID = -5442029141468819238L;

    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(getClass())) {
            return adapter.cast(this);
        }
        return null;
    }

    public boolean accept(RenderingEngine engine) {
        return true;
    }

}
