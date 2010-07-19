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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.nuxeo.theme.themes.ThemeException;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

public final class JSUtils {

    static final Log log = LogFactory.getLog(JSUtils.class);

    public static String compressSource(final String source)
            throws ThemeException {
        String compressedSource = source;
        Reader in = null;
        Writer out = null;

        final boolean munge = true;
        final boolean verbose = false;
        final boolean preserveAllSemiColons = false;
        final boolean disableOptimizations = false;
        final int linebreakpos = -1;

        final JavaScriptCompressor compressor;
        try {
            in = new StringReader(source);

            compressor = new JavaScriptCompressor(in, new ErrorReporter() {

                public void warning(String message, String sourceName,
                        int line, String lineSource, int lineOffset) {
                    log.warn(String.format("%s: %s (%s) ", line, lineSource,
                            message));
                }

                public void error(String message, String sourceName, int line,
                        String lineSource, int lineOffset) {
                    log.error(String.format("%s: %s (%s) ", line, lineSource,
                            message));
                }

                public EvaluatorException runtimeError(String message,
                        String sourceName, int line, String lineSource,
                        int lineOffset) {
                    log.error(String.format("%s: %s (%s) ", line, lineSource,
                            message));
                    return null;
                }
            });

            out = new StringWriter();
            compressor.compress(out, linebreakpos, munge, verbose,
                    preserveAllSemiColons, disableOptimizations);
            compressedSource = out.toString();
        } catch (IOException e) {
            throw new ThemeException("Could not compress javascript", e);
        } catch (EvaluatorException e) {
            throw new ThemeException("Could not compress javascript", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    out = null;
                }
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                in = null;
            }
        }

        return compressedSource;
    }

}
