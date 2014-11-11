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
package org.nuxeo.ecm.shell.swing.widgets;

import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.nuxeo.ecm.shell.swing.Console;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@SuppressWarnings("serial")
public class HistoryFinder extends JTextField implements DocumentListener {

    protected Console console;

    public HistoryFinder(Console console) {
        this.console = console;
        getDocument().addDocumentListener(this);
    }

    @Override
    protected void processComponentKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_ENTER) {
            setVisible(false);
            getParent().validate();
            console.requestFocus();
            e.consume();
        } else if (code == KeyEvent.VK_ESCAPE) {
            console.getCmdLine().setText("");
            setVisible(false);
            getParent().validate();
            console.requestFocus();
            e.consume();
        }
    }

    @SuppressWarnings("unchecked")
    public String getMatch() {
        String text = getText();
        List<String> list = console.getHistory().getHistoryList();
        for (int i = list.size() - 1; i >= 0; i--) {
            String entry = list.get(i);
            int k = entry.indexOf(text);
            if (k > -1) {
                return entry;
            }
        }
        return null;
    }

    public void changedUpdate(DocumentEvent e) {
        String text = getMatch();
        if (text != null) {
            console.getCmdLine().setText(text);
        }
    }

    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

}
