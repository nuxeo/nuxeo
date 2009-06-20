/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.swing;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.nuxeo.build.swing.tree.ArtifactTreeDialog;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    static ArtifactTable table;
    static JFrame frame;

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI(File file) throws IOException {
        //Create and set up the window.
        frame = new JFrame("Assembly Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add the ubiquitous "Hello World" label.
        table = new ArtifactTable(frame);
        if (file != null) {
            table.loadFile(file);
        }
        frame.setTitle("Assembly Editor - "+(file == null ? "Untitled" : file.getName()));
        frame.getContentPane().add(table);
        frame.setJMenuBar(createMenuBar());

        //Display the window.
        frame.pack();
        frame.setSize(800, 600);
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        frame.setLocation((screenSize.width-frame.getWidth())/2, (screenSize.height-frame.getHeight())/2);
        frame.setVisible(true);
    }


    public static JMenuBar createMenuBar() {
        ActionListener actionHandler = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                String cmd = e.getActionCommand();
                if ("Open".equals(cmd)) {
                    table.open();
                } else if ("Save".equals(cmd)) {
                    table.save();
                } else if ("Save As".equals(cmd)) {
                    final JFileChooser fc = new JFileChooser();
                    int r = fc.showSaveDialog(table);
                    if (r == JFileChooser.APPROVE_OPTION) {
                        table.saveAs(fc.getSelectedFile());
                    }
                } else if ("Print".equals(cmd)) {
                    PrintUtilities.printComponent(table.table);
                } else if ("Exit".equals(cmd)) {
                    System.exit(0);
                }
                } catch (IOException ee) {
                    JOptionPane.showMessageDialog(table, ee.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        JMenuItem open = new JMenuItem("Open", KeyEvent.VK_O);
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.META_MASK));
        open.addActionListener(actionHandler);
        JMenuItem save = new JMenuItem("Save", KeyEvent.VK_S);
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.META_MASK));
        save.addActionListener(actionHandler);
        JMenuItem saveas = new JMenuItem("Save As", KeyEvent.VK_A);
        saveas.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.META_MASK));
        saveas.addActionListener(actionHandler);
        JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.META_MASK));
        exit.addActionListener(actionHandler);
        JMenuItem print = new JMenuItem("Print", KeyEvent.VK_P);
        print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.META_MASK));
        print.addActionListener(actionHandler);

        menu.add(open);
        menu.add(save);
        menu.add(saveas);
        menu.addSeparator();
        menu.add(print);
        menu.addSeparator();
        menu.add(exit);

        menu = new JMenu("Edit");
        menuBar.add(menu);
        JMenuItem addArtifact = new JMenuItem("Add Artifact", KeyEvent.VK_A);
        addArtifact.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.META_MASK));
        addArtifact.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                table.openAddArtifactDialog();
            }
        });
        JMenuItem removeArtifact = new JMenuItem("Remove Artifacts", KeyEvent.VK_R);
        removeArtifact.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, ActionEvent.META_MASK));
        removeArtifact.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                table.deleteSelectedArtifacts();
            }
        });
        JMenuItem profiles = new JMenuItem("Profiles Management", KeyEvent.VK_P);
        profiles.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.META_MASK));
        profiles.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                table.openProfilesDialog();
            }
        });
        JMenuItem browse = new JMenuItem("Artifact Browser", KeyEvent.VK_B);
        browse.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.META_MASK));
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //table.openExplorer();
                ArtifactTreeDialog dlg = ArtifactTreeDialog.getInstance();
                dlg.setLocationRelativeTo(table);
                dlg.setVisible(true);
            }
        });

        menu.add(addArtifact);
        menu.add(removeArtifact);
        menu.addSeparator();
        menu.add(profiles);
        menu.add(browse);

        return menuBar;
    }

    public static void main(String[] args) {
        final File file = args.length == 1 ? new File(args[0]) : null;

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    createAndShowGUI(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }

}
