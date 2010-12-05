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

import javax.swing.text.BadLocationException;

/**
 * The last line in the console - where the user type the commands. Must be
 * instantiated each time a new line is used. (after CRLF)
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class CmdLine {

    protected int index;

    protected int start;

    protected String prompt;

    protected int cmdStart;

    protected Console console;

    public CmdLine(Console console) {
        try {
            this.console = console;
            this.index = console.getLineCount() - 1;
            start = console.getLineStartOffset(index);
            prompt = console.reader.getDefaultPrompt();
            cmdStart = start + prompt.length();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getIndex() {
        return index;
    }

    public int getStart() {
        return start;
    }

    public int getCmdStart() {
        return cmdStart;
    }

    public int getEnd() {
        try {
            return console.getLineEndOffset(index);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLineText() {
        try {
            return console.getDocument().getText(start, getEnd() - start);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPrompt() {
        return prompt;
    }

    /**
     * Get the caret position relative to the beginning of the command text (see
     * getCmdStart())
     * 
     * @return
     */
    public int getLocalCaretPosition() {
        int cp = console.getCaretPosition() - cmdStart;
        return cp < 0 ? 0 : cp;
    }

    public String getText() {
        try {
            return console.getDocument().getText(cmdStart, getEnd() - cmdStart);
        } catch (BadLocationException e) {
            throw new RuntimeException("Failed to get line text", e);
        }
    }

    public void setText(String text) {
        int end = getEnd();
        console.replaceRange(text, cmdStart, end);
        console.setCaretPosition(console.getDocument().getLength());
    }

    public void setTextFromCaret(String text) {
        int end = getEnd();
        console.replaceRange(text, console.getCaretPosition(), end);
        console.setCaretPosition(end);
    }

    public void write(String text) {
        int cp = console.getCaretPosition();
        int end = getEnd();
        int len = text.length();
        if (cp < end) {
            int e = cp + len;
            if (e > end) {
                console.replaceRange(text, cp, end);
            } else {
                console.replaceRange(text, cp, e);
            }
        } else {
            console.insert(text, cp);
        }
        console.setCaretPosition(cp + len);
        // console.repaint();
    }

    public void sync() {
        StringBuffer sb = console.reader.getCursorBuffer().getBuffer();
        sb.setLength(0);
        sb.append(getText());
        console.reader.getCursorBuffer().cursor = getLocalCaretPosition();
    }

    public void rsync() {
        setText(console.reader.getCursorBuffer().getBuffer().toString());
        console.setCaretPosition(cmdStart
                + console.reader.getCursorBuffer().cursor);
    }

    public boolean canMoveCaret(int where) {
        int cp = console.getCaretPosition() + where;
        return cp >= cmdStart && cp <= getEnd();
    }

    public int setCompletionWord(String word) {
        int p = getLocalCaretPosition();
        String text = getText();
        Word w = getWord(text, p);
        // replace w with word
        int ws = cmdStart + w.index;
        int we = ws + w.length();
        console.replaceRange(word, ws, we);
        console.setCaretPosition(ws + word.length());
        return getLocalCaretPosition();
    }

    /**
     * Get the word from the command line which is near the given relative caret
     * position. Used for completion.
     * 
     * @param pos
     * @return
     */
    public Word getWord(String text, int pos) {
        if (text.length() == 0) {
            return new Word("", 0);
        }
        if (pos <= 0) {
            // get next word
            int i = text.indexOf(' ');
            String w = i <= 0 ? "" : text.substring(0, i);
            return new Word(w, 0);
        }
        // get the previous word.
        int i = text.lastIndexOf(' ', pos);
        if (i == -1) {
            i = 0;
            int e = text.indexOf(' ');
            if (e == -1) {
                return new Word(text, 0);
            } else {
                return new Word(text.substring(0, e), 0);
            }
        } else {
            i++;
            int e = text.indexOf(' ', i);
            if (e == -1) {
                return new Word(text.substring(i), i);
            } else {
                return new Word(text.substring(i, e), i);
            }
        }
    }

    static class Word {
        public int index;

        public String text;

        public Word(String text, int index) {
            this.text = text;
            this.index = index;
        }

        public int length() {
            return text.length();
        }

        @Override
        public String toString() {
            return text + " [" + index + "]";
        }
    }
}
