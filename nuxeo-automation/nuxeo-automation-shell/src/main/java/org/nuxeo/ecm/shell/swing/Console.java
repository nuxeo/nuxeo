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

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import jline.ConsoleReader;

import org.nuxeo.ecm.shell.cmds.ConsoleReaderFactory;

/**
 * The conversation with jline ConsoleReader is limited to execute a command and
 * get the command output. All the other detials like typing, auto completion,
 * moving cursor, history etc. is using pure swing code without any transfer
 * between the jline console reader and the swing component.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@SuppressWarnings("serial")
public class Console extends JTextArea implements Constants,
        ConsoleReaderFactory {

    protected ConsoleReader reader;

    protected final In in;

    protected final Writer out;

    protected CmdLine cline;

    protected Method complete;

    public Console() throws Exception {
        in = new In();
        out = new Out();
        reader = new ConsoleReader(in, out, null, new SwingTerminal());
        reader.setCompletionHandler(new SwingCompletionHandler(this));
        complete = reader.getClass().getDeclaredMethod("complete");
        complete.setAccessible(true);
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        setCaretColor(Color.GREEN);
        setBackground(Color.black);
        setForeground(Color.GREEN);
        setFocusable(true);
        setEditable(true);
    }

    public CmdLine getCmdLine() {
        if (cline == null) {
            cline = new CmdLine(this);
        }
        return cline;
    }

    public void complete() {
        try {
            getCmdLine().sync();
            if (!((Boolean) complete.invoke(reader))) {
                beep();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            cline = null;
        }
    }

    public void execute() {
        try {
            // setCaretPosition(getDocument().getLength());
            String cmd = getCmdLine().getText().trim();
            append("\n");
            setCaretPosition(getDocument().getLength());
            if (cmd.length() > 0 && reader.getUseHistory()) {
                reader.getHistory().addToHistory(cmd);
                reader.getHistory().moveToEnd();
            }
            cline = null;
            in.put(cmd + "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ConsoleReader getConsoleReader() {
        return reader;
    }

    public InputStream in() {
        return in;
    }

    public Writer out() {
        return out;
    }

    protected void moveHistory(boolean next) {
        if (next && !reader.getHistory().next()) {
            beep();
        } else if (!next && !reader.getHistory().previous()) {
            beep();
        }

        String text = reader.getHistory().current();
        getCmdLine().setText(text);

    }

    @Override
    protected void processComponentKeyEvent(KeyEvent e) {
        if (e.isControlDown()) {
            return;
        }
        int id = e.getID();
        if (id == KeyEvent.KEY_PRESSED) {
            int code = e.getKeyCode();
            if (handleControlChars(e, code)) {
                e.consume();
                return;
            }
        }
    }

    public void beep() {
        audibleBeep();
    }

    public void audibleBeep() {
        Toolkit.getDefaultToolkit().beep();
    }

    public void visualBeep() {
        setBackground(Color.GREEN);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        setBackground(Color.BLACK);
    }

    /**
     * Return true if should consume the event.
     * 
     * @param code
     * @return
     */
    protected boolean handleControlChars(KeyEvent event, int code) {
        switch (code) {
        case KeyEvent.VK_LEFT:
            if (!getCmdLine().canMoveCaret(-1)) {
                beep();
                return true;
            }
            return false;
        case KeyEvent.VK_RIGHT:
            if (!getCmdLine().canMoveCaret(1)) {
                beep();
                return true;
            }
            return false;
        case KeyEvent.VK_UP:
            moveHistory(false);
            return true;
        case KeyEvent.VK_DOWN:
            moveHistory(true);
            return true;
        case KeyEvent.VK_ENTER:
            execute();
            return true;
        case KeyEvent.VK_BACK_SPACE:
            if (!getCmdLine().canMoveCaret(-1)) {
                beep();
                return true;
            }
            return false;
        case KeyEvent.VK_TAB:
            complete();
            return true;
        }
        return false;
    }

    /**
     * Move caret to back. This should be invoked when '\b' is sent by JLine.
     */
    public void back() {
        // force a redraw using setVisible
        setCaretPosition(getCaretPosition() - 1);
        getCaret().setVisible(false);
        getCaret().setVisible(true);
    }

    public void backspace() throws IOException {
        try {
            String text = getText();
            int p = getCaretPosition();
            if (text.length() == p) {
                setCaretPosition(p - 1);
                getDocument().remove(text.length() - 1, 1);
            } else if (p > 0) {
                setCaretPosition(p - 1);
                getDocument().remove(p - 1, 1);
            }
        } catch (BadLocationException e) {
            throw new IOException(e);
        }
    }

    class In extends InputStream {
        protected StringBuilder buf = new StringBuilder();

        public synchronized void put(int key) {
            buf.append((char) key);
            notify();
        }

        public synchronized void put(String text) {
            buf.append(text);
            notify();
        }

        @Override
        public synchronized int read() throws IOException {
            if (buf.length() > 0) {
                char c = buf.charAt(0);
                buf.deleteCharAt(0);
                return c;
            }
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (buf.length() == 0) {
                throw new IllegalStateException(
                        "invalid state for console input stream");
            }
            char c = buf.charAt(0);
            buf.deleteCharAt(0);
            return c;
        }
    }

    class Out extends Writer {

        protected void _write(char[] cbuf, int off, int len) throws IOException {
            _write(new String(cbuf, off, len));
        }

        protected void _write(String str) throws IOException {
            Console.this.append(str);
            setCaretPosition(getDocument().getLength());
        }

        protected boolean handleOutputChar(char c) {
            try {
                if (c == 7) { // beep
                    beep();
                } else if (c < 32 && c != '\n' && c != '\t') {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (len == 1) {
                char c = cbuf[off];
                if (!handleOutputChar(c)) {
                    _write(cbuf, off, len);
                }
            } else {
                StringBuilder buf = new StringBuilder();
                for (int i = off, end = off + len; i < end; i++) {
                    char c = cbuf[i];
                    if (!handleOutputChar(c)) {
                        buf.append(c);
                    }
                }
                if (buf.length() > 0) {
                    _write(buf.toString());
                }
            }
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }

    public void printColumns(List<String> items) {
        int maxlen = 0;
        for (String item : items) {
            int len = item.length();
            if (maxlen < len) {
                maxlen = len;
            }
        }
        int w = reader.getTermwidth();
        int tab = 4;
        int cols = (w + tab) / (maxlen + tab);
        Iterator<String> it = items.iterator();
        while (it.hasNext()) {
            for (int i = 0; i < cols; i++) {
                if (!it.hasNext()) {
                    break;
                }
                append(makeColumn(it.next(), maxlen));
                if (i < cols - 1) {
                    append("    ");
                }
            }
            if (it.hasNext()) {
                append("\n");
            }
        }
    }

    private String makeColumn(String text, int maxlen) {
        int pad = maxlen - text.length();
        if (pad <= 0) {
            return text;
        }
        StringBuilder buf = new StringBuilder(text);
        for (int i = 0; i < pad; i++) {
            buf.append(' ');
        }
        return buf.toString();
    }
}
