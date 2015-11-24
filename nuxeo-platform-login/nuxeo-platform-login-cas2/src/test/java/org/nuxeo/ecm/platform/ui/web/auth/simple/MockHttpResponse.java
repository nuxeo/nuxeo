/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.ui.web.auth.simple;

import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.mock.MockHttpServletResponse;

public class MockHttpResponse extends MockHttpServletResponse {

    protected final Map<String, String> headers = new HashMap<String, String>();

    @Override
    public void setHeader(String key, String value) {
        if (value == null) {
            headers.remove(value);
        } else {
            headers.put(key, value);
        }
    }

}
