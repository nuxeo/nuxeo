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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import jline.CompletionHandler;
import jline.ConsoleReader;
import jline.CursorBuffer;

import org.nuxeo.ecm.shell.Shell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SwingCompletionHandler implements CompletionHandler {

    protected Console console;

    public SwingCompletionHandler(Console console) {
        this.console = console;
    }

    public boolean complete(ConsoleReader reader, List candidates, int position)
            throws IOException {
        CursorBuffer buf = reader.getCursorBuffer();

        // if there is only one completion, then fill in the buffer
        if (candidates.size() == 1) {
            String value = candidates.get(0).toString();

            // fail if the only candidate is the same as the current buffer
            if (value.equals(buf.toString())) {
                console.beep();
                return false;
            }

            position = console.getCmdLine().setCompletionWord(value);
            console.setCaretPosition(console.getCmdLine().getCmdStart()
                    + position);
            return true;
        } else if (candidates.size() > 1) {
            String value = getUnambiguousCompletions(candidates);
            position = console.getCmdLine().setCompletionWord(value);
        } else if (candidates.isEmpty()) {
            console.beep();
            return false;
        }
        String text = console.getCmdLine().getText();
        console.append("\n");
        printCandidates(candidates);
        console.append("\n");
        console.append(Shell.get().getActiveRegistry().getPrompt(Shell.get()));
        console.cline = null;
        console.getCmdLine().setText(text);
        console.setCaretPosition(console.getCmdLine().getCmdStart() + position);

        return true;
    }

    /**
     * Returns a root that matches all the {@link String} elements of the
     * specified {@link List}, or null if there are no commalities. For example,
     * if the list contains <i>foobar</i>, <i>foobaz</i>, <i>foobuz</i>, the
     * method will return <i>foob</i>.
     */
    private final String getUnambiguousCompletions(final List candidates) {
        if ((candidates == null) || (candidates.size() == 0)) {
            return null;
        }

        // convert to an array for speed
        String[] strings = (String[]) candidates.toArray(new String[candidates.size()]);

        String first = strings[0];
        StringBuffer candidate = new StringBuffer();

        for (int i = 0; i < first.length(); i++) {
            if (startsWith(first.substring(0, i + 1), strings)) {
                candidate.append(first.charAt(i));
            } else {
                break;
            }
        }

        return candidate.toString();
    }

    /**
     * @return true is all the elements of <i>candidates</i> start with
     *         <i>starts</i>
     */
    private final boolean startsWith(final String starts,
            final String[] candidates) {
        for (int i = 0; i < candidates.length; i++) {
            if (!candidates[i].startsWith(starts)) {
                return false;
            }
        }

        return true;
    }

    protected void printCandidates(List<String> candidates) {
        Set<String> distinct = new HashSet<String>(candidates);

        if (distinct.size() > console.reader.getAutoprintThreshhold()) {
            // if (!eagerNewlines)
            if (JOptionPane.showConfirmDialog(console, "Display all "
                    + distinct.size() + " possibilities? (y or n) ",
                    "Completion Warning", JOptionPane.YES_NO_OPTION) == 1) {
                return;
            }
        }

        // copy the values and make them distinct, without otherwise
        // affecting the ordering. Only do it if the sizes differ.
        if (distinct.size() != candidates.size()) {
            List<String> copy = new ArrayList<String>();

            for (Iterator<String> i = candidates.iterator(); i.hasNext();) {
                String next = i.next();

                if (!(copy.contains(next))) {
                    copy.add(next);
                }
            }

            candidates = copy;
        }

        console.printColumns(candidates);

    }
}
