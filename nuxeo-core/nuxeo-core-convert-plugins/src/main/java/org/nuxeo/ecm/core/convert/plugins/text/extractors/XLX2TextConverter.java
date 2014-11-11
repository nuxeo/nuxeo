/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;


public class XLX2TextConverter implements Converter {

    private static final Log log = LogFactory.getLog(XLX2TextConverter.class);

    private static final String CELL_SEP = "";
    private static final String ROW_SEP = "\n";

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        InputStream stream = null;
        StringBuffer sb = new StringBuffer();
        try {
            stream = blobHolder.getBlob().getStream();
            OPCPackage p = OPCPackage.open(stream);
            XSSFWorkbook workbook = new XSSFWorkbook(p);
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                Iterator<Row> rows = sheet.rowIterator();
                while (rows.hasNext()) {
                    XSSFRow row = (XSSFRow) rows.next();
                    Iterator<Cell> cells = row.cellIterator();
                    while (cells.hasNext()) {
                        XSSFCell cell = (XSSFCell) cells.next();
                        appendTextFromCell(cell, sb);
                    }
                    sb.append(ROW_SEP);
                }
            }
            return new SimpleCachableBlobHolder(new StringBlob(sb.toString()));
        } catch (Exception e) {
            throw new ConversionException("Error during XLX2Text conversion", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Error while closing Blob stream", e);
                }
            }
        }
    }

    protected void appendTextFromCell(XSSFCell cell, StringBuffer sb) {
        String cellValue = null;
        switch (cell.getCellType()) {
        case XSSFCell.CELL_TYPE_NUMERIC:
            cellValue = Double.toString(cell.getNumericCellValue()).trim();
            break;
        case XSSFCell.CELL_TYPE_STRING:
            cellValue = cell.getStringCellValue().trim();
            break;
        }

        if (cellValue != null && cellValue.length() > 0) {
            sb.append(cellValue).append(CELL_SEP);
        }
    }

    public void init(ConverterDescriptor descriptor) {
    }

}
