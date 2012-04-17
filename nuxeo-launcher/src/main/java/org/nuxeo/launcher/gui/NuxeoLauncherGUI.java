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
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.util.PlatformUtils;
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
 * @since 5.4.2
 * @see NuxeoLauncher
 */
public class NuxeoLauncherGUI {
    static final Log log = LogFactory.getLog(NuxeoLauncherGUI.class);

    protected static final long UPDATE_FREQUENCY = 3000;

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
        // Set OS-specific decorations
        if (PlatformUtils.isMac()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes",
                    "false");
            System.setProperty("com.apple.mrj.application.live-resize", "true");
            System.setProperty("com.apple.macos.smallTabs", "true");
        }
        initFrame(this);
    }

    protected void initFrame(final NuxeoLauncherGUI controller) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    nuxeoFrame = createNuxeoFrame(controller);
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
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    updateServerStatus();
                    try {
                        Thread.sleep(UPDATE_FREQUENCY);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }.start();
    }

    /**
     * Instantiate a new {@link NuxeoFrame}. Can be overridden if needed.
     *
     * @param controller
     * @return
     */
    protected NuxeoFrame createNuxeoFrame(NuxeoLauncherGUI controller) {
        return new NuxeoFrame(controller);
    }

    public void initLogsManagement(String logFile, ColoredTextPane textArea) {
        File file = new File(logFile);
        LogsSource logsSource = new LogsSource(file);
        logsSource.skip(file.length() - NuxeoFrame.LOG_MAX_SIZE);
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
        waitForFrameLoaded();
        nuxeoFrame.stopping = true;
        nuxeoFrame.mainButton.setText(getMessage("mainbutton.stop.inprogress"));
        nuxeoFrame.mainButton.setToolTipText(NuxeoLauncherGUI.getMessage("mainbutton.stop.tooltip"));
        nuxeoFrame.mainButton.setIcon(nuxeoFrame.stopIcon);
        executor.execute(new Runnable() {

            @Override
            public void run() {
                launcher.stop();
                nuxeoFrame.stopping = false;
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
        waitForFrameLoaded();
        nuxeoFrame.updateMainButton();
        nuxeoFrame.updateLaunchBrowserButton();
        nuxeoFrame.updateSummary();
    }

    /**
     * Waits for the Launcher GUI frame being initialized. Should be called
     * before any access to {@link NuxeoFrame} from this controller.
     */
    public void waitForFrameLoaded() {
        // Wait for Frame being initialized
        while (nuxeoFrame == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.error(e);
            }
        }
    }

    /**
     * @see NuxeoLauncher#doStart() NuxeoLauncher#doStartAndWait()
     */
    public void start() {
        waitForFrameLoaded();
        nuxeoFrame.stopping = false;
        nuxeoFrame.mainButton.setText(NuxeoLauncherGUI.getMessage("mainbutton.start.inprogress"));
        nuxeoFrame.mainButton.setToolTipText(NuxeoLauncherGUI.getMessage("mainbutton.stop.tooltip"));
        nuxeoFrame.mainButton.setIcon(nuxeoFrame.stopIcon);
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

    /**
     * Get internationalized message
     *
     * @param key Message key
     * @return Localized message value
     */
    public static String getMessage(String key) {
        String message;
        try {
            message = ResourceBundle.getBundle("i18n/messages").getString(key);
        } catch (MissingResourceException e) {
            log.debug(getMessage("missing.translation") + key);
            message = ResourceBundle.getBundle("i18n/messages", Locale.ENGLISH).getString(
                    key);
        }
        return message;
    }

    /**
     * @return the NuxeoLauncher managed by the current GUI
     * @since 5.5
     */
    public NuxeoLauncher getLauncher() {
        return launcher;
    }

}
