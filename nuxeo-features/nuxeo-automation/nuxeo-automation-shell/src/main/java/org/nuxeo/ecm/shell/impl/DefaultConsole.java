/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.shell.impl;

import org.nuxeo.ecm.shell.ShellConsole;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class DefaultConsole implements ShellConsole {

    public void print(String msg) {
        System.out.print(msg);
    }

    public void println(String msg) {
        System.out.println(msg);
    }

    public void println() {
        System.out.println();
    }

    public String readLine(String prompt, Character mask) {
        throw new UnsupportedOperationException("Not impl");
    }

}
