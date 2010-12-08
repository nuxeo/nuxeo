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
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JTextArea;

import jline.ConsoleReader;
import jline.History;

import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.cmds.ConsoleReaderFactory;
import org.nuxeo.ecm.shell.swing.widgets.HistoryFinder;

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
public class Console extends JTextArea implements ConsoleReaderFactory {

    protected Theme theme;

    protected ConsoleReader reader;

    protected final In in;

    protected final Writer out;

    protected CmdLine cline;

    protected Method complete;

    protected HistoryFinder finder;

    /**
     * If not null should use a mask when typing
     */
    protected Character mask;

    protected StringBuilder pwd;

    public Console() throws Exception {
        setMargin(new Insets(6, 6, 6, 6));
        setEditable(true);
        in = new In();
        out = new Out();
        reader = new ConsoleReader(in, out, null, new SwingTerminal(this));
        reader.setCompletionHandler(new SwingCompletionHandler(this));
        complete = reader.getClass().getDeclaredMethod("complete");
        complete.setAccessible(true);
        Shell shell = Shell.get();
        shell.putContextObject(Console.class, this);
        registerThemes(shell);
        registerCommands(shell);
    }

    protected void registerCommands(Shell shell) {
        shell.getRegistry("config").addAnnotatedCommand(
                org.nuxeo.ecm.shell.swing.cmds.FontCommand.class);
        shell.getRegistry("config").addAnnotatedCommand(
                org.nuxeo.ecm.shell.swing.cmds.ThemeCommand.class);
        shell.getRegistry("config").addAnnotatedCommand(
                org.nuxeo.ecm.shell.swing.cmds.ColorCommand.class);
        shell.getRegistry("config").addAnnotatedCommand(
                org.nuxeo.ecm.shell.swing.cmds.BgColorCommand.class);
    }

    protected void registerThemes(Shell shell) {
        int len = "theme.".length();
        for (Map.Entry<Object, Object> entry : shell.getSettings().entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith("theme.")) {
                String t = key.substring(len);
                Theme.addTheme(Theme.fromString(t, entry.getValue().toString()));
            }
        }
        loadDefaultTheme(shell);
    }

    public void loadDefaultTheme(Shell shell) {
        String tname = shell.getSetting("theme", "Default");
        Theme theme = Theme.getTheme(tname);
        if (theme == null) {
            theme = Theme.getTheme("Default");
        }
        setTheme(theme);
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
        setFont(theme.getFont());
        setCaretColor(theme.getFgColor());
        setBackground(theme.getBgColor());
        setForeground(theme.getFgColor());
        Shell.get().setSetting("theme", theme.getName());
    }

    public ConsoleReader getReader() {
        return reader;
    }

    public void setFinder(HistoryFinder finder) {
        this.finder = finder;
    }

    public void setMask(Character mask) {
        if (mask != null) {
            pwd = new StringBuilder();
        } else {
            pwd = null;
        }
        this.mask = mask;
    }

    public CmdLine getCmdLine() {
        if (cline == null) {
            cline = new CmdLine(this);
        }
        return cline;
    }

    public History getHistory() {
        return reader.getHistory();
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

    public void killLine() {
        getCmdLine().setText("");
    }

    public void killLineBefore() {
        int p = getCmdLine().getLocalCaretPosition();
        getCmdLine().setText(getCmdLine().getText().substring(p));
    }

    public void killLineAfter() {
        int p = getCmdLine().getLocalCaretPosition();
        getCmdLine().setText(getCmdLine().getText().substring(0, p));
    }

    public void execute() {
        try {
            String cmd = getCmdLine().getText().trim();
            append("\n");
            setCaretPosition(getDocument().getLength());
            if (pwd != null) {
                cline = null;
                in.put(pwd.toString() + "\n");
                pwd = null;
                return;
            }
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
            // handle passwords
            if (mask != null) {
                char c = e.getKeyChar();
                if (c >= 32 && c < 127) {
                    append(mask.toString());
                    pwd.append(c);
                }
                e.consume();
            }
        } else if (mask != null) {
            e.consume(); // do not show password
        }
    }

    public void beep() {
        if (Boolean.parseBoolean((String) Shell.get().getProperty(
                "shell.visual_bell", "false"))) {
            visualBell();
        }
        audibleBell();
    }

    public void audibleBell() {
        Toolkit.getDefaultToolkit().beep();
    }

    public void visualBell() {
        setBackground(Color.GREEN);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
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
            if (event.isMetaDown()) {
                setCaretPosition(getCmdLine().getCmdStart());
                return true;
            }
            if (!getCmdLine().canMoveCaret(-1)) {
                beep();
                return true;
            }
            return false;
        case KeyEvent.VK_RIGHT:
            if (event.isMetaDown()) {
                setCaretPosition(getCmdLine().getEnd());
                return true;
            }
            if (!getCmdLine().canMoveCaret(1)) {
                beep();
                return true;
            }
            return false;
        case KeyEvent.VK_UP:
            if (event.isMetaDown()) {
                reader.getHistory().moveToFirstEntry();
                getCmdLine().setText(reader.getHistory().current());
                return true;
            }
            moveHistory(false);
            return true;
        case KeyEvent.VK_DOWN:
            if (event.isMetaDown()) {
                reader.getHistory().moveToLastEntry();
                getCmdLine().setText(reader.getHistory().current());
                return true;
            }
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
        case KeyEvent.VK_K:
            if (event.isMetaDown()) {
                killLineAfter();
                return true;
            }
        case KeyEvent.VK_U:
            if (event.isMetaDown()) {
                killLineBefore();
                return true;
            }
        case KeyEvent.VK_L:
            if (event.isMetaDown()) {
                killLine();
                return true;
            }
        case KeyEvent.VK_X:
            if (event.isMetaDown()) {
                reset();
                in.put("\n");
                return true;
            }
        case KeyEvent.VK_I:
            if (event.isMetaDown()) {
                Font font = new Font(Font.MONOSPACED, Font.PLAIN,
                        getFont().getSize() + 1);
                setFont(font);
                return true;
            }
        case KeyEvent.VK_O:
            if (event.isMetaDown()) {
                Font font = new Font(Font.MONOSPACED, Font.PLAIN,
                        getFont().getSize() - 1);
                setFont(font);
                return true;
            }
        case KeyEvent.VK_EQUALS:
            if (event.isMetaDown()) {
                Font font = new Font(Font.MONOSPACED, Font.PLAIN, 14);
                setFont(font);
                return true;
            }
        case KeyEvent.VK_S:
            if (event.isMetaDown()) {
                if (finder != null) {
                    finder.setVisible(true);
                    finder.getParent().validate();
                    finder.requestFocus();
                    return true;
                }
            }
        }
        return false;
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

    public void reset() {
        try {
            setText("");
            Shell.get().hello();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exit(int code) {
        in.put("exit " + code);
    }

}
