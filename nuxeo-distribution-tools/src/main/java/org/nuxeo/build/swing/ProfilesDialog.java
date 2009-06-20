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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProfilesDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    protected ArtifactTable table;
    protected JTable columnTable;
    protected ColumnsDataModel model;

    public ProfilesDialog(JFrame frame, ArtifactTable table) {
        super(frame);
        this.table = table;
        setTitle("Manage Profiles");
        setAlwaysOnTop(true);
        setContentPane(createContentPane());
    }

    protected JRootPane createRootPane() {
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane rootPane = new JRootPane();
        rootPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ProfilesDialog.this.setVisible(false);
                ProfilesDialog.this.dispose();
            }
        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
      }

    protected Container createContentPane() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        List<String> cols = table.getModel().getColumns();
        HashSet<String> vcols = new HashSet<String>(table.getModel().getVisibleColumns());
        model = new ColumnsDataModel();
        for (String col : cols) {
            model.addColumn(col, vcols.contains(col) ? Boolean.TRUE : Boolean.FALSE);
        }
        columnTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(columnTable);
        JPanel btnPanel = new JPanel(new SpringLayout());

        JButton add = new JButton("Add");
        add.addActionListener(this);
        JButton remove = new JButton("Remove");
        remove.addActionListener(this);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.LINE_END);
        btnPanel.add(add);
        btnPanel.add(remove);

        SpringUtilities.makeCompactGrid(btnPanel, 2, 1, 5, 5, 5, 5);
        //setPreferredColumnWidths(columnTable, new double [] {0.01, 0.99});
        columnTable.getColumnModel().getColumn(0).setPreferredWidth(5);
        columnTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        return panel;
    }

    public void actionPerformed(ActionEvent e) {
        if ("Add".equals(e.getActionCommand())) {
            String s = (String)JOptionPane.showInputDialog(
                    this,
                    "Pofile Name: ",
                    "Create profile",
                    JOptionPane.PLAIN_MESSAGE);
            if (s != null) {
                table.getModel().addColumn(s);
                model.addColumn(s, Boolean.TRUE);
            }
        } else {
            int[] rows = columnTable.getSelectedRows();
            for (int i=0; i<rows.length; i++) {
                Column col = model.removeColumn(i);
                table.getModel().removeColumn(col.key);
            }
        }
    }

    class ColumnsDataModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        List<Column> cols;
        public ColumnsDataModel() {
            cols = new ArrayList<Column>();
        }
        public int getColumnCount() {
            return 2;
        }
        public int getRowCount() {
            return cols.size();
        }
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columnIndex == 0 ? cols.get(rowIndex).selected : cols.get(rowIndex).key;
        }
        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                Column col = cols.get(rowIndex);
                col.selected = (Boolean)value;
                if (col.selected) {
                    table.getModel().getVisibleColumns().add(col.key);
                } else {
                    table.getModel().getVisibleColumns().remove(col.key);
                }
                table.getModel().fireTableStructureChanged();
            }
        }
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }
        @Override
        public String getColumnName(int column) {
            return column == 0 ? "" : "Profile";
        }
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }
        public void addColumn(String col, boolean selected) {
            cols.add(new Column(col, selected));
            fireTableDataChanged();
        }
        public Column removeColumn(int index) {
            Column col = cols.remove(index);
            fireTableDataChanged();
            return col;
        }
        public List<String> getSelectedColumns() {
            ArrayList<String> result = new ArrayList<String>();
            for (Column col : cols) {
                if (col.selected) {
                    result.add(col.key);
                }
            }
            return result;
        }
    }

    static class Column {
        String key;
        Boolean selected;
        public Column(String key, Boolean selected) {
            this.key = key;
            this.selected = selected;
        }
    }

    public void setPreferredColumnWidths(JTable table, double[] percentages) {
        Dimension tableDim = table.getPreferredSize();

        double total = 0;
        for(int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            total += percentages[i];
        }
        for(int i = 0,len=table.getColumnModel().getColumnCount(); i < len; i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth((int) (tableDim.width * (percentages[i] / total)));
            System.out.println("888 "+column.getPreferredWidth());
        }
    }

}
