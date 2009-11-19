package org.nuxeo.dam.webapp.watermark;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.ui.web.download.DownloadServlet;

public class WaterMarkServlet extends DownloadServlet {
    private static final long serialVersionUID = -4809049542136414807L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        super.doGet(new WaterMarkRequest(req), new WaterMarkResponse(resp));
    }
}
