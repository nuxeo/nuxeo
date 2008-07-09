package org.nuxeo.ecm.platform.ui.flex.auth;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public class FlexLoginServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;



    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        sendLoginResponse(req,resp);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        sendLoginResponse(req,resp);
    }


    private void sendLoginResponse(HttpServletRequest req,HttpServletResponse resp) throws IOException
    {

        Principal principal = req.getUserPrincipal();
        NuxeoPrincipal nuxeoPrincipal = (NuxeoPrincipal) principal;

        resp.setContentType("test/xml");

        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<response>");
        sb.append("<status>OK</status>");
        sb.append("<user>");
        sb.append("<firstName>" + nuxeoPrincipal.getFirstName() + "</firstName>");
        sb.append("<lastName>" + nuxeoPrincipal.getLastName() + "</lastName>");
        sb.append("<login>" + nuxeoPrincipal.getPrincipalId() + "</login>");
        sb.append("<name>" + nuxeoPrincipal.getName() + "</name>");
        sb.append("<company>" + nuxeoPrincipal.getCompany() + "</company>");
        sb.append("<groups>");
        for (String group : nuxeoPrincipal.getAllGroups())
        {
        sb.append("<group> " + group + "</group>");
        }
        sb.append("</groups>");

        sb.append("</user>");
        sb.append("</response>");

        resp.getWriter().write(sb.toString());
    }
}
