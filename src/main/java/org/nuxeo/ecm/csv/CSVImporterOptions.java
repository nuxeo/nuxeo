/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv;

import java.io.Serializable;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImporterOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final CSVImporterOptions DEFAULT_OPTIONS = new Builder().build();

    public static class Builder {

        private CSVImporterDocumentFactory CSVImporterDocumentFactory = new DefaultCSVImporterDocumentFactory();

        private String dateFormat = "MM/dd/yyyy";

        private String listSeparatorRegex = "\\|";

        private boolean updateExisting = true;

        private boolean checkAllowedSubTypes = true;

        private boolean sendEmail = false;

        private int batchSize = 50;

        public Builder documentModelFactory(CSVImporterDocumentFactory factory) {
            this.CSVImporterDocumentFactory = factory;
            return this;
        }

        public Builder dateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }

        public Builder listSeparatorRegex(String listSeparatorRegex) {
            this.listSeparatorRegex = listSeparatorRegex;
            return this;
        }

        public Builder updateExisting(boolean updateExisting) {
            this.updateExisting = updateExisting;
            return this;
        }

        public Builder checkAllowedSubTypes(boolean checkAllowedSubTypes) {
            this.checkAllowedSubTypes = checkAllowedSubTypes;
            return this;
        }

        public Builder sendEmail(boolean sendEmail) {
            this.sendEmail = sendEmail;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public CSVImporterOptions build() {
            return new CSVImporterOptions(CSVImporterDocumentFactory,
                    dateFormat, listSeparatorRegex, updateExisting,
                    checkAllowedSubTypes, sendEmail, batchSize);
        }
    }

    protected final CSVImporterDocumentFactory CSVImporterDocumentFactory;

    protected final String dateFormat;

    protected final String listSeparatorRegex;

    protected final boolean updateExisting;

    protected final boolean checkAllowedSubTypes;

    protected final boolean sendEmail;

    protected final int batchSize;

    protected CSVImporterOptions(
            CSVImporterDocumentFactory CSVImporterDocumentFactory,
            String dateFormat, String listSeparatorRegex,
            boolean updateExisting, boolean checkAllowedSubTypes,
            boolean sendEmail, int batchSize) {
        this.CSVImporterDocumentFactory = CSVImporterDocumentFactory;
        this.dateFormat = dateFormat;
        this.listSeparatorRegex = listSeparatorRegex;
        this.updateExisting = updateExisting;
        this.checkAllowedSubTypes = checkAllowedSubTypes;
        this.sendEmail = sendEmail;
        this.batchSize = batchSize;
    }

    public CSVImporterDocumentFactory getCSVImporterDocumentFactory() {
        return CSVImporterDocumentFactory;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getListSeparatorRegex() {
        return listSeparatorRegex;
    }

    public boolean updateExisting() {
        return updateExisting;
    }

    public boolean checkAllowedSubTypes() {
        return checkAllowedSubTypes;
    }

    public boolean sendEmail() {
        return sendEmail;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
