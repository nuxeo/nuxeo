/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.excel;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public interface IExcelBuilder {
    /**
     * Set a cell content at the given indices, and apply the style if it is not null. If row(i) does not exist yet, it
     * is created, otherwise it is recycled. If cell(i,j) does not exist yet, it is created, otherwise it is recycled.
     * Reminder: excel support a maximum of 65,536 rows and 256 columns per sheet
     *
     * @param row row index
     * @param column column index
     * @param content a string to display in the cell
     * @param style a style to apply to the cell
     * @return the created or retrieved cell in case additional stuff should be done on it.
     */
    Cell setCell(int row, int column, String content, CellStyle style);

    /**
     * Set a cell text content with no styling information.
     *
     * @see {@link setCell(int i, int j, String content, CellStyle style)}
     */
    Cell setCell(int row, int column, String content);

    Sheet getCurrentSheet();

    Collection<Sheet> getAllSheets();

    int getCurrentSheetId();

    void setCurrentSheetId(int s);

    int newSheet(int index, String name);

    void setRowHeight(int row, int height);

    /** Set the width (in units of 1/256th of a character width) */
    void setColumnWidth(int column, int width);

    void setColumnWidthAuto(int column);

    void setFreezePane(int colSplit, int rowSplit);

    void setFreezePane(int colSplit, int rowSplit, int leftmostColumn, int topRow);

    void setSplitPane(int xSplitPos, int ySplitPos, int leftmostColumn, int topRow, int activePane);

    void mergeRange(int firstRow, int firstColumn, int lastRow, int lastColumn);

    Comment addComment(Cell cell, String text, int row, int col, int colWidth, int rowHeight);

    CellStyle newColoredCellStyle(ByteColor color);

    void save(String file) throws IOException;

    void save(File file) throws IOException;

    Workbook load(String file) throws InvalidFormatException, IOException;

    Workbook load(File file) throws InvalidFormatException, IOException;

    Font newFont(int size);

    Font newFont();

    Font getBoldFont();

    CellStyle newCellStyle();

    Workbook getWorkbook();

    HSSFColor getColor(ByteColor color);

    int loadPicture(String image) throws IOException;

    void setPicture(int pictureIdx, int col1, int row1, boolean resize);

}
