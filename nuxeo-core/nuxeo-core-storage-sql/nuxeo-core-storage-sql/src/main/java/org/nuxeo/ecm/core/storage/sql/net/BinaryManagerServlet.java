/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.net;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.apache.commons.io.IOUtils;

/**
 * Servlet receiving remote {@link BinaryManagerClient} requests and sending
 * them to an actual {@link BinaryManager}.
 */
public class BinaryManagerServlet extends HttpServlet {

    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private static final Log log = LogFactory.getLog(BinaryManagerServlet.class);

    private static final long serialVersionUID = 1L;

    private final BinaryManager binaryManager;

    public BinaryManagerServlet(BinaryManager binaryManager) {
        this.binaryManager = binaryManager;
    }

    /** Name used for the servlet holder for this servlet. */
    public static String getName(BinaryManager binaryManager) {
        return BinaryManagerServlet.class.getSimpleName() + '-'
                + System.identityHashCode(binaryManager);
    }

    /**
     * Retrieves a binary. The "digest" parameter specifies which.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String digest = getDigest(req);
            Binary binary = binaryManager.getBinary(digest);
            if (binary == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Digest '"
                        + digest + "' not found");
            } else {
                resp.setContentType(APPLICATION_OCTET_STREAM);
                resp.setHeader("Content-Length",
                        String.valueOf(binary.getLength()));
                OutputStream out = resp.getOutputStream();
                IOUtils.copy(binary.getStream(), out);
                out.flush();
            }
        } catch (IOException e) {
            log.error(e, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.toString());
        }
    }

    /**
     * Creates a new binary.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String digest = getDigest(req);
            Binary binary = binaryManager.getBinary(req.getInputStream());
            if (!binary.getDigest().equals(digest)) {
                resp.sendError(HttpServletResponse.SC_CONFLICT,
                        "Digest mismatch: '" + digest + "' vs '"
                                + binary.getDigest() + "'");
            } else {
                resp.setStatus(HttpServletResponse.SC_CREATED);
            }
        } catch (IOException e) {
            log.error(e, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.toString());
        }
    }

    protected static String getDigest(HttpServletRequest req) {
        String digest = req.getParameter("digest");
        if (digest == null) {
            digest = "";
        }
        return digest;
    }

}
