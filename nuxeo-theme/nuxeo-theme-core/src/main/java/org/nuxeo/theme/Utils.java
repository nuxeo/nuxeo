/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);

    private Utils() {
        // This class is not supposed to be instantiated.
    }

    public static boolean contains(final String[] array, final String value) {
        for (String s : array) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static String cleanUp(String text) {
        return text.replaceAll("\n", " ").replaceAll("\\t+", " ").replaceAll(
                "\\s+", " ").trim();
    }

    public static byte[] readResourceAsBytes(final String path) {
        return readResource(path).toByteArray();
    }

    public static String readResourceAsString(final String path) {
        return readResource(path).toString();
    }

    private static ByteArrayOutputStream readResource(final String path) {
        InputStream is = null;
        ByteArrayOutputStream os = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    path);
            if (is == null) {
                log.warn("Resource not found: " + path);
            } else {
                try {
                    os = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int i;
                    while ((i = is.read(buffer)) != -1) {
                        os.write(buffer, 0, i);
                    }
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (os != null) {
                        os.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    is = null;
                }
            }
        }
        return os;
    }

    public static String fetchUrl(URL url) {
        String content = null;
        try {
            final InputStream in = url.openStream();
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int i;
            while ((i = in.read(buffer)) != -1) {
                os.write(buffer, 0, i);
            }
            content = os.toString();
            in.close();
            os.close();
        } catch (IOException e) {
            log.error("Could not retrieve URL: " + url.toString());
        }
        return content;
    }

    public static void writeFile(URL url, String text) throws IOException {
        // local file system
        if (url.getProtocol().equals("file")) {
            String filepath = url.getFile();
            File file = new File(filepath);
            PrintWriter out = null;
            try {
                out = new PrintWriter(new FileWriter(file));
                out.write(text);
            } catch (IOException e) {
                throw e;
            } finally {
                if (out != null) {
                    out.close();
                }
            }

        } else {
            OutputStream os = null;
            URLConnection urlc;
            try {
                urlc = url.openConnection();
                os = urlc.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }

            if (os != null) {
                try {
                    os.write(text.getBytes());
                    os.flush();
                } catch (IOException e) {
                    log.error(e);
                } finally {
                    try {
                        os.close();
                    } catch (IOException e) {
                        log.error(e);
                    } finally {
                        os = null;
                    }
                }
            }

        }

    }

}
