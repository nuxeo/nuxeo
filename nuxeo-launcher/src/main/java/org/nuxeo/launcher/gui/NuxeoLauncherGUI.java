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

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.NuxeoLauncher;

/**
 * Launcher controller for graphical user interface
 *
 * @author jcarsique
 * @since 5.4.1
 * @see NuxeoLauncher
 */
public class NuxeoLauncherGUI {
    static final Log log = LogFactory.getLog(NuxeoLauncherGUI.class);

    private NuxeoLauncher launcher;

    protected JFrame nuxeoFrame;

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
     */
    public void execute() {
        initFrame(this);
    }

    /**
     * @return True if the server is running.
     */
    public boolean isRunning() {
        return launcher.isRunning();
    }

}
