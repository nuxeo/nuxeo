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

package org.nuxeo.theme.html;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.html.JSMin.UnterminatedCommentException;
import org.nuxeo.theme.html.JSMin.UnterminatedRegExpLiteralException;
import org.nuxeo.theme.html.JSMin.UnterminatedStringLiteralException;
import org.nuxeo.theme.themes.ThemeException;

public final class JSUtils {

    static final Log log = LogFactory.getLog(JSUtils.class);

    public static String compressSource(final String source)
            throws ThemeException {
        String compressedSource = source;
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new ByteArrayInputStream(source.getBytes());
            out = new ByteArrayOutputStream();

            JSMin compressor = new JSMin(in, out);
            try {
                compressor.jsmin();
            } catch (UnterminatedRegExpLiteralException e) {
                throw new ThemeException("Could not compress javascript", e);
            } catch (UnterminatedCommentException e) {
                throw new ThemeException("Could not compress javascript", e);
            } catch (UnterminatedStringLiteralException e) {
                throw new ThemeException("Could not compress javascript", e);
            }
            compressedSource = out.toString();
        } catch (IOException e) {
            throw new ThemeException("Could not compress javascript", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error(e, e);
                } finally {
                    out = null;
                }
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                log.error(e, e);
            } finally {
                in = null;
            }
        }

        return compressedSource;
    }

}
