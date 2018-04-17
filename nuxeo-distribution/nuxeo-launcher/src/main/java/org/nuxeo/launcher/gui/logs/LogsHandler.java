/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    }
}
