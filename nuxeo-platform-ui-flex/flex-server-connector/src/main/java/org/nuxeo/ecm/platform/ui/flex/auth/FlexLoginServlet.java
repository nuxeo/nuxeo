package org.nuxeo.ecm.platform.ui.flex.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

        resp.getWriter().write("OK");
    }
}
