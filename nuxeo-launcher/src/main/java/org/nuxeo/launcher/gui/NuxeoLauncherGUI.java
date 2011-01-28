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
 * $Id$
 */

package org.nuxeo.launcher.gui;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.NuxeoLauncher;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.daemon.DaemonThreadFactory;
import org.nuxeo.launcher.gui.logs.LogsHandler;
import org.nuxeo.launcher.gui.logs.LogsSource;
import org.nuxeo.launcher.gui.logs.LogsSourceThread;

/**
 * Launcher controller for graphical user interface
 *
 * @author jcarsique
 * @since 5.4.1
 * @see NuxeoLauncher
 */
public class NuxeoLauncherGUI {
    static final Log log = LogFactory.getLog(NuxeoLauncherGUI.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(5,
            new DaemonThreadFactory("NuxeoLauncherGUITask"));

    protected NuxeoLauncher launcher;

    protected NuxeoFrame nuxeoFrame;

    private HashMap<String, LogsSourceThread> logsMap = new HashMap<String, LogsSourceThread>();

    /**
     * @param launcher Launcher being used in background
     */
    public NuxeoLauncherGUI(NuxeoLauncher launcher) {
        this.launcher = launcher;
    }

    private void initFrame(final NuxeoLauncherGUI controller) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    nuxeoFrame = new NuxeoFrame(controller);
                    nuxeoFrame.pack();
                    // Center frame
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    nuxeoFrame.setLocation(
                            screenSize.width / 2 - (nuxeoFrame.getWidth() / 2),
                            screenSize.height / 2
                                    - (nuxeoFrame.getHeight() / 2));
                    nuxeoFrame.setVisible(true);
                } catch (HeadlessException e) {
                    log.error(e);
                }
            }
        });
    }

    /**
     * Starts GUI, automatically running command passed in parameter after "gui"
     * option.
     *
     * @return Either null or a command delegated by GUI launcher to console
     *         launcher.
     */
    public String execute() {
        initFrame(this);
        String command = launcher.getCommand();
        if (command != null) {
            if ("start".equalsIgnoreCase(command)) {
                start();
            } else if ("stop".equalsIgnoreCase(command)) {
                stop();
            } else
                return command;
        }
        return null;
    }

    public void initLogsManagement(String logFile, JTextArea textArea) {
        LogsSource logsSource = new LogsSource(new File(logFile));
        logsSource.addObserver(new LogsHandler(textArea));
        LogsSourceThread logsSourceThread = new LogsSourceThread(logsSource);
        logsSourceThread.setDaemon(true);
        executor.execute(logsSourceThread);
        logsMap.put(logFile, logsSourceThread);
    }

    /**
     * @see NuxeoLauncher#stop()
     */
    public void stop() {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                launcher.stop();
                updateServerStatus();
            }
        });
    }

    /**
     * Update interface information with current server status.
     *
     * @see {@link NuxeoFrame#updateMainButton()}
     *      {@link NuxeoFrame#updateSummary()}
     */
    public void updateServerStatus() {
        // Wait for Frame being initialized
        while (nuxeoFrame == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.error(e);
            }
        }
        nuxeoFrame.updateMainButton();
        nuxeoFrame.updateSummary();
    }

    /**
     * @see NuxeoLauncher#doStart() NuxeoLauncher#doStartAndWait()
     */
    public void start() {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                launcher.doStartAndWait();
                updateServerStatus();
            }
        });
    }

    /**
     * @param logFile LogFile managed by the involved reader
     * @param isActive Set logs reader active or not
     */
    public void notifyLogsObserver(String logFile, boolean isActive) {
        LogsSourceThread logsSourceThread = logsMap.get(logFile);
        if (isActive) {
            logsSourceThread.getSource().resume();
        } else {
            logsSourceThread.getSource().pause();
        }
    }

    /**
     * @return Configuration generator used by {@link #launcher}
     */
    public ConfigurationGenerator getConfigurationGenerator() {
        return launcher.getConfigurationGenerator();
    }

}
