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
package org.nuxeo.ecm.shell.swing;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public interface Constants {

    /**
     * CTRL-B: move to the previous character
     */
    public final static int PREV_CHAR = 2;

    /**
     * CTRL-G: move to the previous word
     */
    public final static int PREV_WORD = 7;

    /**
     * CTRL-F: move to the next character
     */
    public final static int NEXT_CHAR = 6;

    /**
     * CTRL-A: move to the beginning of the line
     */
    public final static int MOVE_TO_BEG = 1;

    /**
     * CTRL-D: close out the input stream
     */
    public final static int EXIT = 4;

    /**
     * CTRL-E: move the cursor to the end of the line
     */
    public final static int MOVE_TO_END = 5;

    /**
     * BACKSPACE, CTRL-H: delete the previous character 8 is the ASCII code for
     * backspace and therefore deleting the previous character
     */
    public final static int DELETE_PREV_CHAR = 8;

    /**
     * TAB, CTRL-I: signal that console completion should be attempted
     */
    public final static int COMPLETE = 9;

    /**
     * CTRL-J, CTRL-M: newline
     */
    public final static int CR = 10;

    /**
     * CTRL-K: erase the current line
     */
    public final static int KILL_LINE = 11;

    /**
     * ENTER: newline
     */
    public final static int ENTER = 13;

    /**
     * CTRL-L: clear screen
     */
    public final static int CLEAR_SCREEN = 12;

    /**
     * CTRL-N: scroll to the next element in the history buffer
     */
    public final static int NEXT_HISTORY = 14;

    /**
     * CTRL-P: scroll to the previous element in the history buffer
     */
    public final static int PREV_HISTORY = 16;

    /**
     * CTRL-R: redraw the current line
     */
    public final static int REDISPLAY = 18;

    /**
     * CTRL-U: delete all the characters before the cursor position
     */
    public final static int KILL_LINE_PREV = 21;

    /**
     * CTRL-V: paste the contents of the clipboard (useful for Windows terminal)
     */
    public final static int PASTE = 22;

    /**
     * CTRL-W: delete the word directly before the cursor
     */
    public final static int DELETE_PREV_WORD = 23;

    /**
     * DELETE, CTRL-?: delete the previous character 127 is the ASCII code for
     * delete
     */
    public final static int DELETE_CHAR = 127;

}
