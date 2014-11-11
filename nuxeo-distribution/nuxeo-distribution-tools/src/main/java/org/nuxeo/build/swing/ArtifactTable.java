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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.nuxeo.build.swing.ArtifactTableModel.ArtifactRow;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactTable extends JPanel {

    private static final long serialVersionUID = 1L;

    protected JFrame frame;
    protected boolean isDirty;
    protected JToolBar tbar;
    protected JTable table;
    JTextField textFilter;
    JToggleButton btnFilter;
    protected ArtifactTableModel model;
    protected File file;

    public ArtifactTable(JFrame frame) {
        super(new BorderLayout(5,5));
        this.frame = frame;
        tbar = new JToolBar();
        AbstractAction action = new AbstractAction("Add", IconUtils.createImageIcon(getClass(), "add.gif")) {
            public void actionPerformed(ActionEvent e) {
                openAddArtifactDialog();
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Add Artifact");
        tbar.add(action);
        action = new AbstractAction("Remove", IconUtils.createImageIcon(getClass(), "delete.gif")) {
            public void actionPerformed(ActionEvent e) {
                deleteSelectedArtifacts();
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Remove Selected Artifacts");
        tbar.add(action);
        action = new AbstractAction("Profiles", IconUtils.createImageIcon(getClass(), "profiles.gif")) {
            public void actionPerformed(ActionEvent e) {
                openProfilesDialog();
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Manage Profiles");
        tbar.add(action);


        JComboBox presets = new JComboBox(new String[] {"All", "Runtime", "Core", "Features", "Toolkits", "Libraries"});
        presets.setToolTipText("Default Filters");
        presets.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String preset = (String)((JComboBox)e.getSource()).getSelectedItem();
                System.out.println("preset: "+preset);
                if ("All".equals(preset)) {
                    removePresetFilter();
                } else if ("Runtime".equals(preset)) {
                    applyPresetFilter(new MultiPrefixFilter("nuxeo-runtime", "org.nuxeo.runtime", "nuxeo-common", "org.nuxeo.common"));
                } else if ("Core".equals(preset)) {
                    applyPresetFilter(new MultiPrefixFilter("nuxeo-core", "org.nuxeo.ecm.core"));
                } else if ("Features".equals(preset)) {
                    applyPresetFilter(new MultiPrefixFilter("nuxeo-platform", "org.nuxeo.ecm.platform"));
                } else if ("Toolkits".equals(preset)) {
                    applyPresetFilter(new MultiPrefixFilter("nuxeo-webengine", "org.nuxeo.ecm.webengine", "nuxeo-theme", "org.nuxeo.theme"));
                } else if ("Libraries".equals(preset)) {
                    applyPresetFilter(new MultiExclusionFilter("nuxeo-", "org.nuxeo"));
                }
            }
        });
        tbar.add(presets);

        textFilter = new JTextField();
        tbar.add(textFilter);
        btnFilter = new JToggleButton(null, IconUtils.createImageIcon(getClass(), "search.gif"));
        btnFilter.setSelected(false);
        btnFilter.setToolTipText("Apply Filter");
        tbar.add(btnFilter);

        textFilter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!btnFilter.isSelected()) {
                    btnFilter.doClick();//setSelected(true);
                } else {
                    String text = textFilter.getText().trim();
                    if (text.length() > 0) {
                        applyPrefixFilter(text);
                    }
                }
            }
        });
        btnFilter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                togglePrefixFilter();
            }
        });

        add(tbar, BorderLayout.PAGE_START);

        model = new ArtifactTableModel(this);
        table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        //Add the scroll pane to this panel.
        add(scrollPane, BorderLayout.CENTER);

        installKeybordShortcuts();
    }

    public void applyPrefixFilter(String prefix) {
        PrefixFilter filter = new PrefixFilter(prefix);
        CompositeFilter cf = (CompositeFilter)model.getFilter();
        if (cf != null) {
            cf.setPrefixFilter(filter);
            model.buildRows();
        } else {
            cf = new CompositeFilter();
            cf.setPrefixFilter(filter);
            model.setFilter(cf);
        }
    }

    public void removePrefixFilter() {
        CompositeFilter cf = (CompositeFilter)model.getFilter();
        if (cf != null) {
            cf.setPrefixFilter(null);
            model.buildRows();
        }
    }

    public void applyPresetFilter(Filter filter) {
        CompositeFilter cf = (CompositeFilter)model.getFilter();
        if (cf != null) {
            cf.setPresetFilter(filter);
            model.buildRows();
        } else {
            cf = new CompositeFilter();
            cf.setPresetFilter(filter);
            model.setFilter(cf);
        }
    }

    public void removePresetFilter() {
        CompositeFilter cf = (CompositeFilter)model.getFilter();
        if (cf != null) {
            cf.setPresetFilter(null);
            model.buildRows();
        }
    }

    public void removeFilters() {
        model.setFilter(null);
    }


    public void setDirty(boolean value) {
        isDirty = value;
        if (isDirty) {
            frame.setTitle("* Assembly Editor - "+(file == null ? "Untitled" : file.getName()));
        } else {
            frame.setTitle("Assembly Editor - "+(file == null ? "Untitled" : file.getName()));
        }
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void loadFile(File file) throws IOException {
        model.load(file);
        this.file = file;
        setDirty(false);
    }

    protected boolean selectFile(boolean save) {
        //Create a file chooser
        final JFileChooser fc = new JFileChooser();
        //In response to a button click:
        int r = 0;
        if (save) {
            r = fc.showSaveDialog(this);
        } else {
            r = fc.showOpenDialog(this);
        }
        if (r == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            return true;
        }
        return false;
    }

    public void open() throws IOException {
        if (selectFile(false)) {
            model.load(file);
            setDirty(false);
        }
    }

    public void save() throws IOException {
        if (file == null) {
            if (selectFile(true)) {
                model.store(file);
            }
        } else {
            model.store(file);
        }
        setDirty(false);
    }

    public void saveAs(File file) throws IOException {
        model.store(file);
    }

    public ArtifactTableModel getModel() {
        return model;
    }

    public void openProfilesDialog() {
        ProfilesDialog dlg = new ProfilesDialog(null, ArtifactTable.this);
        dlg.pack();
        dlg.setSize(400, 400);
        dlg.setLocationRelativeTo(ArtifactTable.this);
        dlg.setVisible(true);
    }

    public void openAddArtifactDialog() {
        String s = (String)JOptionPane.showInputDialog(
                ArtifactTable.this,
                "Artifact Key: ",
                "Add Artifact",
                JOptionPane.PLAIN_MESSAGE);
        if (s != null) {
            ArtifactRow row = new ArtifactRow(s);
            model.map.put(s, row);
            model.getVisibleRows().add(row);
            model.fireTableDataChanged();
            setDirty(true);
        }
    }

    public void deleteSelectedArtifacts() {
        int[] rows = table.getSelectedRows();
        ArtifactRow[] _rows = new ArtifactRow[rows.length];
        for (int i=0; i<rows.length; i++) {
            _rows[i] = model.getVisibleRows().get(rows[i]);
        }
        for (int i=0; i<rows.length; i++) {
            model.getVisibleRows().remove(_rows[i]);
            model.map.remove(_rows[i].key);
            setDirty(true);
        }
        model.fireTableDataChanged();
    }

    public void togglePrefixFilter() {
        if (btnFilter.isSelected()) {
            String text = textFilter.getText().trim();
            if (text.length() > 0) {
                applyPrefixFilter(text);
            }
        } else {
            removePrefixFilter();
        }
    }

    public void installKeybordShortcuts() {
//        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_MASK);
//        registerKeyboardAction(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                openAddArtifactDialog();
//            }
//        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
//        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.META_MASK);
//        registerKeyboardAction(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                openProfilesDialog();
//            }
//        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
//        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.META_MASK);
//        registerKeyboardAction(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                deleteSelectedArtifacts();
//            }
//        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (btnFilter.isSelected()) {
                    btnFilter.doClick();
                }
            }
        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
