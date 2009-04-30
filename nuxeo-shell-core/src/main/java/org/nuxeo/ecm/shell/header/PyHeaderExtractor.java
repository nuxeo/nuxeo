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

import java.io.File;
import java.io.FileReader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PyHeaderExtractor extends AbstractHeaderExtractor {

    @Override
    protected boolean isHeaderBoundary(String line) {
        return line.startsWith("\"\"\"");
    }

    public static void main(String[] args) throws Exception {
        CommandHeader ch =new PyHeaderExtractor().extractHeader(new FileReader(new File("/tmp/test.py")));
        System.out.println(ch.description);
        System.out.println("----------");
        System.out.println(ch.pattern);
        System.out.println("----------");
        System.out.println(ch.help);
    }

}
