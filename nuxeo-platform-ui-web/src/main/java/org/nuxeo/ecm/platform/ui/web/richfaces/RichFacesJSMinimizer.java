/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.ui.web.richfaces;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.nuxeo.ecm.platform.web.common.resources.JSMinimizer;

/**
 * Implementation of the {@link JSMinimizer} interface based on RichFaces
 *
 * @author tiry
 */
public class RichFacesJSMinimizer implements JSMinimizer {

    public String minimize(String jsScriptContent) {

        try {
            InputStream in = new ByteArrayInputStream(
                    jsScriptContent.getBytes("UTF-8"));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JSMin jsmin = new JSMin(in, out);
            jsmin.jsmin();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return jsScriptContent;
        }
    }
}
