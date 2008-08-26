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

package org.nuxeo.ecm.webengine.rest.types;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.adapters.ScriptObject;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;

/**
 * Temp class until configuration will be again available
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptType extends AbstractWebType {

    public String getName() {
        return "Script";
    }

    public WebType getSuperType() {
        return WebType.ROOT;
    }

    public WebObject newInstance() throws WebException {
        return new ScriptObject(this);
    }

    public Class<? extends WebObject> getObjectClass() {
        return ScriptObject.class;
    }

}
