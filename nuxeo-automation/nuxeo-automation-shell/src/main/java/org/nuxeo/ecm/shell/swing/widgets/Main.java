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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.getInstalledLookAndFeels();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            final JFontChooser fontChooser = new JFontChooser();

            final JFrame window = new JFrame("JFontChooser Sample");
            final JButton showButton = new JButton("Select Font");
            showButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int result = fontChooser.showDialog(window);
                    if (result == JFontChooser.OK_OPTION) {
                        Font font = fontChooser.getSelectedFont();
                        showButton.setFont(font);
                        window.pack();
                        System.out.println("Selected Font : " + font);
                    }
                }
            });
            window.getContentPane().add(showButton, BorderLayout.CENTER);
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setLocationRelativeTo(null);
            window.pack();
            window.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
