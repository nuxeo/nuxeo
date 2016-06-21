/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.platform.web.common;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.Test;

/**
 * @since 5.7.2
 */
public class TestContentDisposition {

    private static final String MSIE_7 = "Mozilla/4.0 (compatible; MSIE 7.0)";

    @Test
    public void inlineContentDisposition() throws Exception {
        for (boolean useAttribute : new Boolean[] { true, false }) {

            HttpServletRequest req = getRequest(useAttribute, "true", MSIE_7);
            assertEquals("inline; filename=myfile.txt", ServletHelper.getRFC2231ContentDisposition(req, "myfile.txt"));

            req = getRequest(useAttribute, "false", MSIE_7);
            assertEquals("attachment; filename=myfile.txt",
                    ServletHelper.getRFC2231ContentDisposition(req, "myfile.txt"));

            req = getRequest(useAttribute, null, MSIE_7);
            assertEquals("attachment; filename=myfile.txt",
                    ServletHelper.getRFC2231ContentDisposition(req, "myfile.txt"));
        }
    }

    private HttpServletRequest getRequest(boolean useAttribute, String inline, String userAgent) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        if (useAttribute) {
            when(req.getAttribute("inline")).thenReturn(inline);
        } else {
            when(req.getParameter("inline")).thenReturn(inline);
        }
        when(req.getHeader("User-Agent")).thenReturn(userAgent);
        return req;
    }
}
