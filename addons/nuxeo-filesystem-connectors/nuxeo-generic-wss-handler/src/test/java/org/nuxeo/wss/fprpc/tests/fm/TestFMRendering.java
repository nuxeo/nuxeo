/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.fprpc.tests.fm;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.HashMap;

import org.junit.Test;
import org.nuxeo.wss.fm.FreeMarkerRenderer;
import org.nuxeo.wss.fprpc.FPRPCConts;
import org.nuxeo.wss.fprpc.tests.WindowsHelper;

public class TestFMRendering {

    @Test
    public void testSimpleRender() throws Exception {

        FreeMarkerRenderer renderer = FreeMarkerRenderer.instance();
        StringWriter writer = new StringWriter();

        java.util.Map<String, Object> params = new HashMap<String, Object>();

        params.put("test", "success");
        renderer.render("simpletest.ftl", params, writer);

        String result = writer.getBuffer().toString();

        String[] lines = WindowsHelper.splitLines(result);

        assertEquals("ABCD", lines[0]);
        assertEquals("success", lines[1]);
        assertEquals(FPRPCConts.METHOD_PARAM, lines[2]);

    }

    @Test
    public void testIncludeRender() throws Exception {

        FreeMarkerRenderer renderer = FreeMarkerRenderer.instance();
        StringWriter writer = new StringWriter();

        java.util.Map<String, Object> params = new HashMap<String, Object>();

        params.put("test", "success");
        renderer.render("simpleinclude.ftl", params, writer);

        String result = writer.getBuffer().toString();

        String[] lines = WindowsHelper.splitLines(result);

        assertEquals("BEFORE", lines[0]);
        assertEquals("ABCD", lines[1]);
        assertEquals("success", lines[2]);
        assertEquals(FPRPCConts.METHOD_PARAM, lines[3]);
        assertEquals("AFTER", lines[5]);

    }

    @Test
    public void testInfoRender() throws Exception {

        FreeMarkerRenderer renderer = FreeMarkerRenderer.instance();
        StringWriter writer = new StringWriter();

        java.util.Map<String, Object> params = new HashMap<String, Object>();

        renderer.render("_vti_inf.html", params, writer);

        String result = writer.getBuffer().toString();

        String[] lines = WindowsHelper.splitLines(result);

        assertEquals("FPVersion=\"12.0.0.000\"", lines[1]);
        assertEquals("FPShtmlScriptUrl=\"_vti_bin/shtml.dll/_vti_rpc\"",
                lines[2]);
        assertEquals("FPAuthorScriptUrl=\"_vti_bin/_vti_aut/author.dll\"",
                lines[3]);
        assertEquals("FPAdminScriptUrl=\"_vti_bin/_vti_adm/admin.dll\"",
                lines[4]);
        assertEquals("TPScriptUrl=\"_vti_bin/owssvr.dll\"", lines[5]);
    }

}
