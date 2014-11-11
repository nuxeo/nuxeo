/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.seam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jboss.seam.excel.ExcelWorkbook;
import org.jboss.seam.excel.ExcelWorkbookException;
import org.jboss.seam.excel.WorksheetItem;
import org.jboss.seam.excel.ui.UIWorksheet;
import org.jboss.seam.excel.ui.command.Command;

/**
 * Overrides default column for better intropection of children.
 * <p>
 * If e:column tags are not direct children, the work sheet will not find them.
 * As layout templating adds additional JSF components, the children tree has
 * to be introspected further on.
 *
 * @author Anahide Tchertchian
 * @since 5.4.1
 */
public class UIColumn extends org.jboss.seam.excel.ui.UIColumn {

    public static final String COMPONENT_TYPE = "org.nuxeo.ecm.platform.jsf.UIColumn";

    public static final String FOOTER_FACET_NAME = "footer";

    @SuppressWarnings("unchecked")
    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        // Get workbook and worksheet
        ExcelWorkbook excelWorkbook = getWorkbook(getParent());

        if (excelWorkbook == null) {
            throw new ExcelWorkbookException("Could not find excel workbook");
        }

        // Column width etc.
        excelWorkbook.applyColumnSettings(this);

        UIWorksheet sheet = (UIWorksheet) getParentByClass(getParent(),
                UIWorksheet.class);
        if (sheet == null) {
            throw new ExcelWorkbookException("Could not find worksheet");
        }

        // Add header items (if any)
        WorksheetItem headerItem = (WorksheetItem) getFacet(HEADER_FACET_NAME);
        if (headerItem != null) {
            excelWorkbook.addItem(headerItem);
        }

        // Execute commands (if any)
        List<Command> commands = getCommands(getChildren());
        for (Command command : commands) {
            excelWorkbook.executeCommand(command);
        }

        // Get UiCell template this column's data cells and iterate over sheet
        // data
        for (WorksheetItem item : getItems(getChildren())) {
            Object oldValue = null;
            Iterator iterator = null;
            // Store away the old value for the sheet binding var (if there is
            // one)
            if (sheet.getVar() != null) {
                oldValue = FacesContext.getCurrentInstance().getExternalContext().getRequestMap().get(
                        sheet.getVar());
                iterator = sheet.getDataIterator();
            } else {
                // No var, no iteration...
                iterator = new ArrayList().iterator();
            }
            while (iterator.hasNext()) {
                // Store the bound data in the request map and add the cell
                FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(
                        sheet.getVar(), iterator.next());
                excelWorkbook.addItem(item);
            }

            // No iteration, nothing to restore
            if (sheet.getVar() == null) {
                continue;
            }
            // Restore the previously modified request map (if there was a var)
            if (oldValue == null) {
                FacesContext.getCurrentInstance().getExternalContext().getRequestMap().remove(
                        sheet.getVar());
            } else {
                FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(
                        sheet.getVar(), oldValue);
            }
        }

        // Add footer items (if any)
        WorksheetItem footerItem = (WorksheetItem) getFacet(FOOTER_FACET_NAME);
        if (footerItem != null) {
            excelWorkbook.addItem(footerItem);
        }

        // Move column pointer to next column
        excelWorkbook.nextColumn();

    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getAllChildrenOfType(List<UIComponent> children,
            Class<T> childType) {
        List<T> matches = new ArrayList<T>();
        for (UIComponent child : children) {
            if (childType.isAssignableFrom(child.getClass())) {
                matches.add((T) child);
            } else {
                // introspect children
                List<T> subChildren = getAllChildrenOfType(child.getChildren(),
                        childType);
                if (subChildren != null && !subChildren.isEmpty()) {
                    matches.addAll(subChildren);
                }
            }
        }
        return matches;
    }

    /**
     * Returns all commands from a child list
     *
     * @param children The list to search
     * @return The commands
     */
    protected static List<Command> getCommands(List<UIComponent> children) {
        return getAllChildrenOfType(children, Command.class);
    }

    /**
     * Returns all worksheet items (cells, images, hyperlinks) from a child
     * list
     *
     * @param children The list to search
     * @return The items
     */
    protected static List<WorksheetItem> getItems(List<UIComponent> children) {
        return getAllChildrenOfType(children, WorksheetItem.class);
    }

}
