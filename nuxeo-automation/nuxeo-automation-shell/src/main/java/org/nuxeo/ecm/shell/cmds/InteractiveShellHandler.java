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
package org.nuxeo.ecm.shell.cmds;

/**
 * Give a chance to embedded shells to do some customization before shell is
 * started and when exit is required.
 * <p>
 * This is currently used by shell applet to correctly handle start and stop.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public interface InteractiveShellHandler {

    /**
     * Interactive shell will be started.
     */
    void enterInteractiveMode();

    /**
     * Interactive shell should be disposed. This method should dispose the
     * current session. If no handler is defined the Java process will exit with
     * the given code (if <= 0 - normal exit, otherwise an exit with given code
     * is performed).
     * 
     * Return true to exit the shell loop, false otherwise.
     * 
     * @param code
     */
    boolean exitInteractiveMode(int code);

}
