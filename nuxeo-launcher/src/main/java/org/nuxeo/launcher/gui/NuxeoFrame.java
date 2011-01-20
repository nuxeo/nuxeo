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
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Launcher view for graphical user interface
 *
 * @author jcarsique
 * @since 5.4.1
 * @see NuxeoLauncherGUI
 */
public class NuxeoFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    protected final ImageIcon showLogsIcon = getImageIcon("icons/show_logs.png");

    protected final ImageIcon hideLogsIcon = getImageIcon("icons/hide_logs.png");

    protected final ImageIcon startIcon = getImageIcon("icons/start.png");

    protected final ImageIcon stopIcon = getImageIcon("icons/stop.png");

    protected JButton mainButton = null;

    protected NuxeoLauncherGUI controller;

    protected boolean isRunning = false;

    protected boolean logsShown = false;

    protected JButton logsButton;

    protected JTextArea textArea;

    private GridBagConstraints constraints;

    protected NuxeoFrame parentContainer;

    protected final class ImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private Image backgroundImage;

        private Icon image;

        ImagePanel(Icon image, ImageIcon backgroundImage) {
            this.image = image;
            this.backgroundImage = GrayFilter.createDisabledImage(backgroundImage.getImage());
            setOpaque(false);
            add(new JLabel(image));
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(backgroundImage, 0, 0, this);
        }
    }

    protected class LogsButtonAction extends AbstractAction {

        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("showlogs" + e);
            logsButton.setEnabled(false);
            if (logsShown) {
                textArea.setVisible(false);
                logsShown = false;
            } else {
                textArea.setVisible(true);
                logsShown = true;
            }
            updateLogsButton();
            logsButton.setEnabled(true);
//            parentContainer.pack();
        }

    }

    protected void updateLogsButton() {
        if (logsShown) {
            logsButton.setText(getMessage("logsbutton.hide.text"));
            logsButton.setToolTipText(getMessage("logsbutton.hide.tooltip"));
            logsButton.setIcon(hideLogsIcon);
        } else {
            logsButton.setText(getMessage("logsbutton.show.text"));
            logsButton.setToolTipText(getMessage("logsbutton.show.tooltip"));
            logsButton.setIcon(showLogsIcon);
        }
    }

    static final Log log = LogFactory.getLog(NuxeoFrame.class);

    protected class StartStopAction extends AbstractAction {

        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            mainButton.setEnabled(false);
            if (isRunning) {
                log.debug("stop" + e);
                // Check is running

                // Stop and change status
                mainButton.setText(getMessage("mainbutton.stop.inprogress"));
                isRunning = false;
            } else {
                log.debug("start" + e);
                // Check not running

                // Start and change status
                mainButton.setText(getMessage("mainbutton.start.inprogress"));
                isRunning = true;
            }
            updateMainButton();
            mainButton.setEnabled(true);
        }

    }

    protected void updateMainButton() {
        // if (controller.isRunning()) {
        if (isRunning) {
            mainButton.setText(getMessage("mainbutton.stop.text"));
            mainButton.setToolTipText(getMessage("mainbutton.stop.tooltip"));
            mainButton.setIcon(stopIcon);
        } else {
            mainButton.setText(getMessage("mainbutton.start.text"));
            mainButton.setToolTipText(getMessage("mainbutton.start.tooltip"));
            mainButton.setIcon(startIcon);
        }
        this.validate();
    }

    public NuxeoFrame(NuxeoLauncherGUI controller) throws HeadlessException {
        super("NuxeoCtl");
        this.controller = controller;
        this.parentContainer = this;
        buildContainer();

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        // Header
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.PAGE_START;
        JComponent header = buildHeader();
        header.setLayout(new GridBagLayout());
        topPanel.add(header, constraints);
        add(topPanel,constraints);

        // Main button (start/stop)
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.PAGE_START;
        header.add(buildMainButton(), constraints);

        // Logs button (show/hide)
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        topPanel.add(buildLogsButton(), constraints);

        // Logs panel
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.PAGE_START;
        constraints.ipady = 100;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        add(buildLogsPanel(), constraints);

        // Footer
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.PAGE_END;
        constraints.ipady = 0;
        constraints.weightx = 0;
        constraints.weighty = 0.1;
        constraints.insets = new Insets(10,0,0,0);
        add(buildFooter(), constraints);
        debug();
    }

    private void debug() {
        for (Component comp : this.getContentPane().getComponents()) {
            if (comp instanceof JComponent) {
                ((JComponent) comp).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.red),
                        ((JComponent) comp).getBorder()));
                // log.info(comp.getClass() + " size: "
                // + ((JComponent) comp).getMaximumSize());
            }
        }
    }

    private JComponent buildLogsButton() {
        logsButton = createButton(new LogsButtonAction(), null);
        updateLogsButton();
        return logsButton;
    }

    private JComponent buildFooter() {
        JLabel label = new JLabel(getMessage("footer.label"));
        return label;
    }

    private JComponent buildLogsPanel() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setAutoscrolls(true);
        textArea.setVisible(false);
        return textArea;
    }

    private JComponent buildMainButton() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(getMainButton());
        return buttonPanel;
    }

    private JComponent buildHeader() {
        ImagePanel headerLogo = new ImagePanel(
                getImageIcon("img/nuxeo_control_panel_logo.png"),
                getImageIcon("img/nuxeo_control_panel_bg.png"));
        return headerLogo;
    }

    private void buildContainer() {
        // Main frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(new Color(50, 50, 50));
        // setSize(700, 500);
        // setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        constraints = new GridBagConstraints();
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

    protected JButton getMainButton() {
        if (mainButton == null) {
            mainButton = createButton(new StartStopAction(), null);
            updateMainButton();
        }
        return mainButton;
    }

    /**
     * Get internationalized message
     *
     * @param key Message key
     * @return Localized message value
     */
    public String getMessage(String key) {
        String message;
        try {
            message = ResourceBundle.getBundle("i18n/messages").getString(key);
        } catch (MissingResourceException e) {
            log.error(e);
            message = getMessage("missing.translation") + key;
        }
        return message;
    }

    private JButton createButton(ActionListener action, ImageIcon icon) {
        JButton button = new JButton();
        button.addActionListener(action);
        button.setIcon(icon);
        return button;
    }

}
