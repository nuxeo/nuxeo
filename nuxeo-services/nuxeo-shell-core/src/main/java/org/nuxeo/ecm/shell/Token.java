/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.shell;

/**
 * A token is a command line argument and may be of 3 types:
 * <ul>
 * <li> A command option
 * <li> A command option value
 * <li> A command parameter
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Token {

    public static final int COMMAND = 0;
    public static final int OPTION  = 1;
    public static final int VALUE   = 2;
    public static final int PARAM   = 3;

    /**
     * The token type
     */
    public final int type; // one of the constants above

    /**
     * The index of this token in the command line (0 based)
     */
    public final int index;

    /**
     * <ul>
     * <li>for OPTIONs this is the token index for its value (0 based)
     * <li>for VALUEs this is the token index for the target option (0 based)
     * <li>for PARAMs this is the index of the parameter in the parameter list (0 based)
     * </ul>
     */
    public int info;

    /**
     * The token value
     */
    public final String value;


    public Token(int type, String value, int index, int info) {
        this.type = type;
        this.index = index;
        this.value = value;
        this.info = info;
    }

    public Token(int type, String value, int index) {
        this(type, value, index, -1);
    }

}
