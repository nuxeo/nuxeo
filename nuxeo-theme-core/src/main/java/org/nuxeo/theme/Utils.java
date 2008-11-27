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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
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

    public static String getFileContent(String name) {
        InputStream is = null;
        StringBuilder content = new StringBuilder();
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    name);
            Reader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(is));
                int ch;
                while ((ch = in.read()) > -1) {
                    content.append((char) ch);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    in.close();
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
        return content.toString();
    }

    public static void writeFile(URL url, String text) {
        // local file system
        if (url.getProtocol().equals("file")) {
            String filepath = url.getFile();
            File file = new File(filepath);
            PrintWriter out = null;
            try {
                out = new PrintWriter(new FileWriter(file));
            } catch (IOException e) {
                log.error(e);
            }
            if (out != null) {
                out.write(text);
                out.close();
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
