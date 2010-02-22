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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.watermark;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.dam.core.watermark.WatermarkService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;

public class WaterMarkResponse extends HttpServletResponseWrapper {

    private final Logger log = Logger.getLogger(WaterMarkResponse.class);

    protected File tmpFile = new File(System.getProperty("java.io.tmpdir"),
            UUID.randomUUID().toString());

    protected ServletOutputStream out = null;

    private static WatermarkService watermarkService;

    public WaterMarkResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (out == null) {
            out = new WaterMarkOutStream(tmpFile);
        }
        return out;
    }

    @Override
    public void flushBuffer() throws IOException {
        try {
            getOutputStream().close();
        } catch (Exception e) {
            // probably has been closed once.
        }
        File wtmkdFile = null;
        try {

            wtmkdFile = getWatermarkService().performWatermarkOnFile(tmpFile);
            if (wtmkdFile == null) {
                throw new IOException("Watermark failed.");
            }
            FileInputStream fis = new FileInputStream(wtmkdFile);
            OutputStream os = getResponse().getOutputStream();

            FileUtils.copy(fis, os);
            fis.close();
            os.close();
            super.flushBuffer();
        }

        catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e.getMessage());
        }

        finally {
            tmpFile.delete();
            if (wtmkdFile != null) {
                wtmkdFile.delete();
            }
        }
    }

    protected WatermarkService getWatermarkService() throws ClientException {
        if (watermarkService == null) {
            try {
                watermarkService = Framework.getService(WatermarkService.class);
            } catch (Exception e) {
                log.error("Unable to get Watermark Service.", e);
            }
        }
        if (watermarkService == null) {
            throw new ClientException("Unable to get Watermark Service: null");
        }
        return watermarkService;
    }
}
