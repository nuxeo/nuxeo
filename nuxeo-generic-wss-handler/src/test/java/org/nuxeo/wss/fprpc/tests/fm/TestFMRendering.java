package org.nuxeo.wss.fprpc.tests.fm;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashMap;

import junit.framework.TestCase;

import org.nuxeo.wss.fm.FreeMarkerRenderer;
import org.nuxeo.wss.fprpc.FPRPCConts;

public class TestFMRendering extends TestCase {


    public void testSimpleRender() throws Exception {

        FreeMarkerRenderer renderer = FreeMarkerRenderer.instance();
        StringWriter writer = new StringWriter();

        java.util.Map<String, Object> params = new HashMap<String, Object>();

        params.put("test", "success");
        renderer.render("simpletest.ftl", params, writer);

        String result = writer.getBuffer().toString();

        String[] lines = result.split("\n");

        assertEquals("ABCD", lines[0]);
        assertEquals("success", lines[1]);
        assertEquals(FPRPCConts.METHOD_PARAM, lines[2]);

    }

    public void testIncludeRender() throws Exception {

        FreeMarkerRenderer renderer = FreeMarkerRenderer.instance();
        StringWriter writer = new StringWriter();

        java.util.Map<String, Object> params = new HashMap<String, Object>();

        params.put("test", "success");
        renderer.render("simpleinclude.ftl", params, writer);

        String result = writer.getBuffer().toString();

        String[] lines = result.split("\n");

        assertEquals("BEFORE", lines[0]);
        assertEquals("ABCD", lines[1]);
        assertEquals("success", lines[2]);
        assertEquals(FPRPCConts.METHOD_PARAM, lines[3]);
        assertEquals("AFTER", lines[5]);

    }
    public void testInfoRender() throws Exception {

        FreeMarkerRenderer renderer = FreeMarkerRenderer.instance();
        StringWriter writer = new StringWriter();

        java.util.Map<String, Object> params = new HashMap<String, Object>();

        renderer.render("_vti_inf.html", params, writer);

        String result = writer.getBuffer().toString();

        String[] lines = result.split("\n");

        assertEquals("FPVersion=\"12.0.0.000\"", lines[1]);
        assertEquals("FPShtmlScriptUrl=\"_vti_bin/shtml.dll/_vti_rpc\"", lines[2]);
        assertEquals("FPAuthorScriptUrl=\"_vti_bin/_vti_aut/author.dll\"", lines[3]);
        assertEquals("FPAdminScriptUrl=\"_vti_bin/_vti_adm/admin.dll\"", lines[4]);
        assertEquals("TPScriptUrl=\"_vti_bin/owssvr.dll\"", lines[5]);


    }


    public void testSplit()  {


        System.out.println("nuxeo/toto".split("/")[0]);
        System.out.println("/nuxeo/toto".split("/")[1]);
        System.out.println("/nuxeo".split("/")[1]);
        System.out.println("nuxeo/".split("/")[0]);
        System.out.println("nuxeo".split("/")[0]);

    }
}
