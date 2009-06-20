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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    protected TreeMap<String, ArtifactRow> map;
    protected LinkedHashSet<String> columns;
    protected List<ArtifactRow> visibleRows;
    protected List<String> visibleColumns;

    protected Filter filter;
    protected ArtifactTable table;

    public ArtifactTableModel(ArtifactTable table) {
        map = new TreeMap<String, ArtifactRow>();
        columns = new LinkedHashSet<String>();
        visibleColumns = new ArrayList<String>();
        visibleColumns.add("Artifact");
        visibleRows = new ArrayList<ArtifactRow>();
        this.table = table;
    }

    public List<String> getColumns() {
        return new ArrayList<String>(columns);
    }

    public List<String> getVisibleColumns() {
        return visibleColumns;
    }

    public List<ArtifactRow> getVisibleRows() {
        return visibleRows;
    }

    public TreeMap<String,ArtifactRow> getMap() {
        return map;
    }

    public int getColumnCount() {
        return visibleColumns.size();
    }

    public int getRowCount() {
        return visibleRows.size();
    }

    @Override
    public String getColumnName(int column) {
        return visibleColumns.get(column);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        ArtifactRow row = visibleRows.get(rowIndex);
        if (columnIndex == 0) {
            return row.key;
        } else {
            return row.getValue(visibleColumns.get(columnIndex));
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex > 0) {
            ArtifactRow row = visibleRows.get(rowIndex);
            row.setValue(visibleColumns.get(columnIndex), (Boolean)value);
        }
        table.setDirty(true);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? String.class : Boolean.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex > 0;
    }

    public void addColumn(String column) {
        columns.add(column);
        visibleColumns.add(column);
        fireTableStructureChanged();
    }

    public void removeColumn(String column) {
        columns.remove(column);
        visibleColumns.remove(column);
        fireTableStructureChanged();
    }

    public void store(File file) throws IOException {
        OutputStream in = new FileOutputStream(file);
        try {
            store(in);
        } finally {
            in.close();
        }
    }

    public void store(OutputStream out) throws IOException {
        PrintStream printer = new PrintStream(out);
        for (ArtifactRow row : map.values()) {
            printer.print(row.key);
            int len = row.profiles.size()-1;
            if (len == -1) {
                printer.println();
                continue;
            }
            printer.print("?");
            for (int i=0; i<len; i++) {
                printer.print(row.profiles.get(i));
                printer.print(",");
            }
            printer.println(row.profiles.get(len));
        }
    }

    public void load(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            load(in);
        } finally {
            in.close();
        }
    }

    public void load(URL url) throws IOException {
        InputStream in = url.openStream();
        try {
            load(in);
        } finally {
            in.close();
        }
    }

    public void load(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = reader.readLine();
        while (line != null) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }
            int p = line.indexOf("?");
            String key = null;
            if (p == -1) {
                key = line;
                line = null;
            } else {
                key = line.substring(0, p);
                line = line.substring(p+1);
            }
            ArtifactRow row = new ArtifactRow(key);
            if (line != null) {
                readTokens(line, row);
            }
            map.put(row.key, row);
            line = reader.readLine();
        }
        build();
    }

    public synchronized void build() {
        visibleColumns = new ArrayList<String>();
        visibleColumns.add("Artifacts");
        for (String col : columns) {
            visibleColumns.add(col);
        }
        buildRows(false);
        fireTableStructureChanged();
    }

    public synchronized void buildRows() {
        buildRows(true);
    }

    public synchronized void buildRows(boolean refresh) {
        visibleRows = new ArrayList<ArtifactRow>();
        if (filter == null) {
            for (ArtifactRow row : map.values()) {
                visibleRows.add(row);
            }
        } else {
            for (ArtifactRow row : map.values()) {
                if (filter.acceptRow(row.key)) visibleRows.add(row);
            }

        }
        if (refresh) {
            fireTableDataChanged();
        }
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        if (this.filter != filter) {
            this.filter = filter;
            buildRows();
        }
    }

    private void readTokens(String text, ArtifactRow row) {
        StringTokenizer tokenizer = new StringTokenizer(text, ",");
        while(tokenizer.hasMoreElements()) {
            String tok = tokenizer.nextToken().trim();
            row.setValue(tok, Boolean.TRUE);
            columns.add(tok);
        }
    }


    static class ArtifactRow {
        protected String key;
        protected List<String> profiles;
        public ArtifactRow(String key) {
            this.key = key;
            this.profiles = new ArrayList<String>();
        }
        public Boolean getValue(String key) {
            for (int i=0,len=profiles.size(); i<len; i++) {
                if (key.equals(profiles.get(i))) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }
        public void setValue(String key, Boolean value) {
            if (!value) { // remove profile
                for (int i=0,len=profiles.size(); i<len; i++) {
                    if (key.equals(profiles.get(i))) {
                        profiles.remove(i);
                    }
                }
            } else {
                for (int i=0,len=profiles.size(); i<len; i++) {
                    if (key.equals(profiles.get(i))) {
                        return;
                    }
                }
                profiles.add(key);
            }
        }

    }

}
