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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.api;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps command parameters (String or File).
 *
 * @author tiry
 * @author Vincent Dutat
 */
public class CmdParameters implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Map<String, String> params;

    public CmdParameters() {
        params = new HashMap<String, String>();
    }

    public void addNamedParameter(String name, String value) {
        params.put(name, value);
    }

    public void addNamedParameter(String name, File file) {
        addNamedParameter(name, "\"" + file.getAbsolutePath() + "\"");
    }

    public Map<String, String> getParameters() {
        return params;
    }

}
