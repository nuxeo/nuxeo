/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.header;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractHeaderExtractor implements HeaderExtractor {

    public static final String CRLF = System.getProperty("line.separator");

    public CommandHeader extractHeader(Reader r) throws IOException,  ParseException {
        BufferedReader reader  = null;
        if (r instanceof BufferedReader) {
            reader = (BufferedReader)r;
        } else {
            reader = new BufferedReader(r);
        }

        String line = null;
        while (true) {
            line = reader.readLine();
            if (line == null) {
                return null;
            }
            line = line.trim();
            if (!isEmpty(line)) {
                break;
            }
        }

        // test if a header was found
        if (!isHeaderBoundary(line)) {
            return null;
        }
        CommandHeader header = new CommandHeader();

        // skip empty lines
        while (true) {
            line = reader.readLine();
            if (line == null) {
                return header;
            }
            line = trimLine(line);
            if (!isEmpty(line)) {
                break;
            }
        }

        StringBuilder buf = new StringBuilder();
        // find synopsis
        buf.append(line);
        while (true) {
            line = reader.readLine();
            if (line == null || isHeaderBoundary(line)) {
                header.description = buf.toString();
                return header;
            }
            line = trimLine(line);
            if (isEmpty(line)) {
                break;
            }
            // read synopsis line
            buf.append(" ").append(line);
        }
        header.description = buf.toString();

        // skip empty lines
        while (true) {
            line = reader.readLine();
            if (line == null) {
                return header;
            }
            line = trimLine(line);
            if (!isEmpty(line)) {
                break;
            }
        }

        buf.setLength(0);
        buf.append(line);
        // find syntax
        while (true) {
            line = reader.readLine();
            if (line == null || isHeaderBoundary(line)) {
                String input = buf.toString().trim();
                if (!isEmpty(input)) {
                    header.pattern = CommandPattern.parsePattern(input);
                }
                return header;
            }
            line = trimLine(line);
            if (isEmpty(line)) {
                break;
            }
            // read synopsis line
            buf.append(line).append(" ");
        }
        String input = buf.toString().trim();
        if (!isEmpty(input)) {
            header.pattern = CommandPattern.parsePattern(input);
        }

        // skip empty lines
        while (true) {
            line = reader.readLine();
            if (line == null) {
                return header;
            }
            line = trimLine(line);
            if (!isEmpty(line)) {
                break;
            }
        }

        buf.setLength(0);
        buf.append(line);
        // find help
        while (true) {
            line = reader.readLine();
            if (line == null || isHeaderBoundary(line)) {
                header.help = buf.toString().trim();
                return header;
            }
            line = trimLine(line);
            // read synopsis line
            buf.append(line).append(CRLF);
        }
    }

    protected String trimLine(String line) {
        return line.trim();
    }

    protected boolean isEmpty(String line) {
        return line == null || line.length() == 0;
    }

    protected abstract boolean isHeaderBoundary(String line);

}
