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
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

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

    protected boolean logsShown = false;

    protected JButton logsButton;

    protected JScrollPane logsPanel;

    private GridBagConstraints constraints;

    protected NuxeoFrame contentPane;

    protected Component filler;

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

    protected class LogsButtonAction extends AbstractAction {

        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("showlogs" + e);
            logsButton.setEnabled(false);
            if (logsShown) {
                logsPanel.setVisible(false);
                filler.setVisible(true);
                logsShown = false;
            } else {
                logsPanel.setVisible(true);
                filler.setVisible(false);
                logsShown = true;
            }
            controller.notifyLogsObserver(logsShown);
            updateLogsButton();
            logsButton.setEnabled(true);
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
            if (controller.isRunning()) {
                mainButton.setText(getMessage("mainbutton.stop.inprogress"));
                controller.stop();
            } else {
                mainButton.setText(getMessage("mainbutton.start.inprogress"));
                controller.start();
            }
        }

    }

    protected void updateMainButton() {
        if (controller.isRunning()) {
            mainButton.setText(getMessage("mainbutton.stop.text"));
            mainButton.setToolTipText(getMessage("mainbutton.stop.tooltip"));
            mainButton.setIcon(stopIcon);
        } else {
            mainButton.setText(getMessage("mainbutton.start.text"));
            mainButton.setToolTipText(getMessage("mainbutton.start.tooltip"));
            mainButton.setIcon(startIcon);
        }
        mainButton.setEnabled(true);
        this.validate();
    }

    public NuxeoFrame(NuxeoLauncherGUI controller) throws HeadlessException {
        super("NuxeoCtl");
        this.controller = controller;
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                debug();
            }
        });

        // Main frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(55, 55, 55));
        // setLocationRelativeTo(null);
        getContentPane().setLayout(new GridBagLayout());
        constraints = new GridBagConstraints();

        // Header (with main button inside)
        constraints.fill = GridBagConstraints.HORIZONTAL;
        // constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.PAGE_START;
        JComponent header = buildHeader();
        header.setPreferredSize(new Dimension(480, 110));
        getContentPane().add(header, constraints);

        // Logs button (show/hide)
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        getContentPane().add(buildLogsButton(), constraints);

        // Logs panel
        constraints.fill = GridBagConstraints.BOTH;
        constraints.ipady = 100;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        getContentPane().add(buildLogsPanel(), constraints);

        // Transparent component filling available space when logsPanel is
        // hidden
        constraints.ipady = 100;
        filler = Box.createGlue();
        filler.setPreferredSize(new Dimension(480, 160));
        getContentPane().add(filler, constraints);

        // Footer
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.PAGE_END;
        constraints.ipady = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(10, 0, 0, 0);
        getContentPane().add(buildFooter(), constraints);
    }

    public void debug() {
        for (Component comp : this.getContentPane().getComponents()) {
            if (comp instanceof JComponent) {
                ((JComponent) comp).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.red),
                        ((JComponent) comp).getBorder()));
                log.info(comp.getClass() + " size: "
                        + ((JComponent) comp).getSize());
            }
        }
    }

    private JButton buildLogsButton() {
        logsButton = createButton(new LogsButtonAction(), null);
        logsButton.setForeground(Color.WHITE);
        logsButton.setOpaque(false);
        logsButton.setBackground(new Color(55, 55, 55));
        logsButton.setIconTextGap(0);
        logsButton.setPreferredSize(new Dimension(200, 45));
        logsButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        updateLogsButton();
        return logsButton;
    }

    private JComponent buildFooter() {
        JLabel label = new JLabel(getMessage("footer.label"));
        label.setForeground(Color.WHITE);
        label.setPreferredSize(new Dimension(470, 16));
        label.setFont(new Font(label.getFont().getName(),
                label.getFont().getStyle(), 9));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JComponent buildLogsPanel() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setAutoscrolls(true);
//        textArea.setPreferredSize(new Dimension(450, 160));
//        textArea.setVisible(false);
//        textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        textArea.setBackground(new Color(64, 64, 64));
        textArea.setText(controller.getStatus());
        textArea.setForeground(Color.WHITE);
        textArea.setLineWrap(true);
//        controller.setLogsContainer(textArea,logsPanel);
        logsPanel = new JScrollPane(logsPanel);
        logsPanel.setPreferredSize(new Dimension(450, 160));
        logsPanel.setVisible(false);
        logsPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//        logsPanel.setBackground(new Color(64, 64, 64));
        return logsPanel;
    }

    private JComponent buildMainButton() {
        mainButton = createButton(new StartStopAction(), null);
        updateMainButton();
        return mainButton;
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
