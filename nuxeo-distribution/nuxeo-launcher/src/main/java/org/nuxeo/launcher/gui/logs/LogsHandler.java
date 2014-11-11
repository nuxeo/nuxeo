/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.launcher.gui.logs;

import java.awt.Color;
import java.util.Observable;
import java.util.Observer;

import org.nuxeo.launcher.gui.ColoredTextPane;

/**
 * @author jcarsique
 * @since 5.4.2
 */
public class LogsHandler implements Observer {

    private ColoredTextPane textArea;

    /**
     * @param nuxeoLauncherGUI
     */
    public LogsHandler(ColoredTextPane textArea) {
        this.textArea = textArea;
    }

    @Override
    public void update(Observable obj, Object arg) {
        if (arg instanceof String) {
            notifyLogsView((String) arg);
        }
    }

    /**
     * @param logLine Line read from log file being sent to view
     */
    public void notifyLogsView(String logLine) {
        Color color;
        String[] split = logLine.split(" ", 4);
        if (split.length < 3) {
            color = new Color(234, 234, 234);
        } else if ("INFO".equals(split[2])) {
            color = new Color(234, 234, 234);
        } else if ("DEBUG".equals(split[2])) {
            color = new Color(108, 183, 242);
        } else if ("WARN".equals(split[2])) {
            color = new Color(234, 138, 2);
        } else if ("ERROR".equals(split[2])) {
            color = new Color(245, 0, 63);
        } else {
            color = new Color(234, 234, 234);
        }
        textArea.append(logLine, color);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}
