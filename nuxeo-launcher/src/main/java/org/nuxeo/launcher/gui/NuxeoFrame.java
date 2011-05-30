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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.log4j.Log4JHelper;
import org.nuxeo.shell.Shell;
import org.nuxeo.shell.cmds.Interactive;
import org.nuxeo.shell.cmds.InteractiveShellHandler;
import org.nuxeo.shell.swing.ConsolePanel;

/**
 * Launcher view for graphical user interface
 *
 * @author jcarsique
 * @since 5.4.2
 * @see NuxeoLauncherGUI
 */
public class NuxeoFrame extends JFrame {

    private class LogsPanelListener extends ComponentAdapter {
        private String logFile;

        public LogsPanelListener(String logFile) {
            this.logFile = logFile;
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            controller.notifyLogsObserver(logFile, false);
        }

        @Override
        public void componentShown(ComponentEvent e) {
            controller.notifyLogsObserver(logFile, true);
        }

    }

    protected final class ImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private Image backgroundImage;

        ImagePanel(Icon image, ImageIcon backgroundImage) {
            this.backgroundImage = backgroundImage.getImage();
            setOpaque(false);
            add(new JLabel(image));
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(backgroundImage, 0, 0, this);
        }
    }

    private Action startAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            mainButton.setEnabled(false);
            controller.start();
        }
    };

    private Action stopAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            mainButton.setEnabled(false);
            controller.stop();
        }
    };

    protected static final Log log = LogFactory.getLog(NuxeoFrame.class);

    private static final long serialVersionUID = 1L;

    protected final ImageIcon startIcon = getImageIcon("icons/start.png");

    protected final ImageIcon stopIcon = getImageIcon("icons/stop.png");

    protected final ImageIcon appIcon = getImageIcon("icons/control_panel_icon_32.png");

    protected JButton mainButton = null;

    protected NuxeoLauncherGUI controller;

    protected boolean logsShown = false;

    protected JButton logsButton;

    private GridBagConstraints constraints;

    protected NuxeoFrame contentPane;

    protected Component filler;

    private JTabbedPane tabbedPanel;

    protected ConsolePanel consolePanel;

    private JLabel summaryStatus;

    private JLabel summaryURL;

    public NuxeoFrame(NuxeoLauncherGUI controller) throws HeadlessException {
        super("NuxeoCtl");
        this.controller = controller;

        // Main frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(appIcon.getImage());
        getContentPane().setBackground(new Color(55, 55, 55));
        getContentPane().setLayout(new GridBagLayout());
        constraints = new GridBagConstraints();

        // Header (with main button inside)
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.PAGE_START;
        JComponent header = buildHeader();
        header.setPreferredSize(new Dimension(480, 110));
        getContentPane().add(header, constraints);

        // Tabs
        constraints.fill = GridBagConstraints.BOTH;
        constraints.ipady = 100;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        getContentPane().add(buildTabbedPanel(), constraints);

        // Footer
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.PAGE_END;
        constraints.ipady = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(10, 0, 0, 0);
        getContentPane().add(buildFooter(), constraints);

        // debug((JComponent) this.getContentPane());
    }

    private Component buildConsolePanel() {
        try {
            consolePanel = new ConsolePanel();
        } catch (Exception e) {
            log.error(e);
        }
        Interactive.setConsoleReaderFactory(consolePanel.getConsole());
        Interactive.setHandler(new InteractiveShellHandler() {
            @Override
            public void enterInteractiveMode() {
                Interactive.reset();
            }

            @Override
            public boolean exitInteractiveMode(int code) {
                if (code == 1) {
                    Interactive.reset();
                    Shell.reset();
                    return true;
                } else {
                    consolePanel.getConsole().reset();
                    return false;
                }
            }
        });
        new Thread() {
            @Override
            public void run() {
                try {
                    Shell.get().main(
                            new String[] { controller.launcher.getURL()
                                    + "site/automation" });
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }.start();
        return consolePanel;
    }

    private JComponent buildFooter() {
        JLabel label = new JLabel(NuxeoLauncherGUI.getMessage("footer.label"));
        label.setForeground(Color.WHITE);
        label.setPreferredSize(new Dimension(470, 16));
        label.setFont(new Font(label.getFont().getName(),
                label.getFont().getStyle(), 9));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JComponent buildHeader() {
        ImagePanel headerLogo = new ImagePanel(
                getImageIcon("img/nuxeo_control_panel_logo.png"),
                getImageIcon("img/nuxeo_control_panel_bg.png"));
        headerLogo.setLayout(new GridBagLayout());
        // Main button (start/stop) (added to header)

        GridBagConstraints headerConstraints = new GridBagConstraints();
        headerConstraints.gridx = 0;
        headerLogo.add(buildMainButton(), headerConstraints);
        return headerLogo;
    }

    private JComponent buildLogsTab() {
        JTabbedPane logsTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        // Get Launcher log file(s)
        ArrayList<String> logFiles = Log4JHelper.getFileAppendersFiles(LogManager.getLoggerRepository());
        // Get server log file(s)
        logFiles.addAll(controller.getConfigurationGenerator().getLogFiles());
        for (String logFile : logFiles) {
            logsTabbedPane.addTab(new File(logFile).getName(),
                    buildLogPanel(logFile));
        }
        return logsTabbedPane;
    }

    /**
     * @param logFile
     * @return
     */
    private JComponent buildLogPanel(String logFile) {
        ColoredTextPane textArea = new ColoredTextPane();
        textArea.setEditable(false);
        textArea.setAutoscrolls(true);
        textArea.setBackground(new Color(64, 64, 64));

        JScrollPane logsScroller = new JScrollPane(textArea);
        logsScroller.setVisible(true);
        logsScroller.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        logsScroller.setAutoscrolls(true);
        logsScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        logsScroller.setWheelScrollingEnabled(true);
        logsScroller.setPreferredSize(new Dimension(450, 160));

        controller.initLogsManagement(logFile, textArea);
        logsScroller.addComponentListener(new LogsPanelListener(logFile));
        return logsScroller;
    }

    private JComponent buildMainButton() {
        mainButton = createButton(null);
        updateMainButton();
        return mainButton;
    }

    private Component buildSummaryPanel() {
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.PAGE_AXIS));
        summaryPanel.setBackground(new Color(55, 55, 55));
        summaryPanel.setForeground(Color.WHITE);

        summaryPanel.add(new JLabel("<html><font color=#ffffdd>"
                + NuxeoLauncherGUI.getMessage("summary.status.label")));
        summaryStatus = new JLabel(controller.launcher.status());
        summaryStatus.setForeground(Color.WHITE);
        summaryPanel.add(summaryStatus);
        summaryPanel.add(new JLabel("<html><font color=#ffffdd>"
                + NuxeoLauncherGUI.getMessage("summary.url.label")));
        summaryURL = new JLabel(controller.launcher.getURL());
        summaryURL.setForeground(Color.WHITE);
        summaryPanel.add(summaryURL);

        summaryPanel.add(new JSeparator());
        ConfigurationGenerator config = controller.launcher.getConfigurationGenerator();
        summaryPanel.add(new JLabel("<html><font color=#ffffdd>"
                + NuxeoLauncherGUI.getMessage("summary.homedir.label")));
        summaryPanel.add(new JLabel("<html><font color=white>"
                + config.getNuxeoHome().getPath()));
        summaryPanel.add(new JLabel("<html><font color=#ffffdd>"
                + NuxeoLauncherGUI.getMessage("summary.nuxeoconf.label")));
        summaryPanel.add(new JLabel("<html><font color=white>"
                + config.getNuxeoConf().getPath()));
        summaryPanel.add(new JLabel("<html><font color=#ffffdd>"
                + NuxeoLauncherGUI.getMessage("summary.datadir.label")));
        summaryPanel.add(new JLabel("<html><font color=white>"
                + config.getDataDir().getPath()));
        return summaryPanel;
    }

    private JComponent buildTabbedPanel() {
        tabbedPanel = new JTabbedPane(JTabbedPane.TOP);
        tabbedPanel.addTab(NuxeoLauncherGUI.getMessage("tab.summary.title"),
                buildSummaryPanel());
        tabbedPanel.addTab(NuxeoLauncherGUI.getMessage("tab.logs.title"),
                buildLogsTab());
        tabbedPanel.addTab(NuxeoLauncherGUI.getMessage("tab.shell.title"),
                buildConsolePanel());
        tabbedPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JTabbedPane pane = (JTabbedPane) e.getSource();
                if (pane.getSelectedIndex() == 2) {
                    consolePanel.getConsole().requestFocusInWindow();
                }
            }
        });
        return tabbedPanel;
    }

    private JButton createButton(ImageIcon icon) {
        JButton button = new JButton();
        button.setIcon(icon);
        return button;
    }

    public void debug(JComponent parent) {
        for (Component comp : parent.getComponents()) {
            if (comp instanceof JComponent) {
                ((JComponent) comp).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.red),
                        ((JComponent) comp).getBorder()));
                log.info(comp.getClass() + " size: "
                        + ((JComponent) comp).getSize());
            }
        }
    }

    protected ImageIcon getImageIcon(String resourcePath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(getClass().getClassLoader().getResource(
                    resourcePath));
        } catch (IOException e) {
            log.error(e);
        }
        return new ImageIcon(image);
    }

    protected void updateMainButton() {
        if (controller.launcher.isRunning()) {
            mainButton.setAction(stopAction);
            mainButton.setText(NuxeoLauncherGUI.getMessage("mainbutton.stop.text"));
            mainButton.setToolTipText(NuxeoLauncherGUI.getMessage("mainbutton.stop.tooltip"));
            mainButton.setIcon(stopIcon);
        } else {
            mainButton.setAction(startAction);
            mainButton.setText(NuxeoLauncherGUI.getMessage("mainbutton.start.text"));
            mainButton.setToolTipText(NuxeoLauncherGUI.getMessage("mainbutton.start.tooltip"));
            mainButton.setIcon(startIcon);
        }
        mainButton.setEnabled(true);
        mainButton.validate();
    }

    /**
     * Update information displayed in summary tab
     */
    public void updateSummary() {
        summaryStatus.setText(controller.launcher.status());
        summaryURL.setText(controller.launcher.getURL());
    }

}
