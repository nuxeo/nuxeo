/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

public class XL2TextConverter implements Converter {

    private static final Log log = LogFactory.getLog(XL2TextConverter.class);

    private static final String CELL_SEP = " ";

    private static final String ROW_SEP = "\n\n";

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        return new SimpleCachableBlobHolder(convert(blobHolder.getBlob(), parameters));
    }

    @Override
    public Blob convert(Blob blob, Map<String, Serializable> parameters) throws ConversionException {
        StringBuilder sb = new StringBuilder();
        try (InputStream stream = blob.getStream();
                POIFSFileSystem fs = new POIFSFileSystem(stream);
                HSSFWorkbook workbook = new HSSFWorkbook(fs)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                HSSFSheet sheet = workbook.getSheetAt(i);
                Iterator<Row> rows = sheet.rowIterator();
                while (rows.hasNext()) {
                    HSSFRow row = (HSSFRow) rows.next();
                    Iterator<?> cells = row.cellIterator();
                    while (cells.hasNext()) {
                        HSSFCell cell = (HSSFCell) cells.next();
                        appendTextFromCell(cell, sb);
                        sb.append(CELL_SEP);
                    }
                    sb.append(ROW_SEP);
                }
            }
            return Blobs.createBlob(sb.toString());
        } catch (IOException e) {
            throw new ConversionException("Error during XL2Text conversion", blob, e);
        }
    }

    protected void appendTextFromCell(HSSFCell cell, StringBuilder sb) {
        String cellValue = null;
        switch (cell.getCellType()) {
        case NUMERIC:
            cellValue = Double.toString(cell.getNumericCellValue()).trim();
            break;
        case STRING:
            cellValue = cell.getStringCellValue().trim().replaceAll("\n", " ");
            break;
        }

        if (cellValue != null && cellValue.length() > 0) {
            sb.append(cellValue);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
