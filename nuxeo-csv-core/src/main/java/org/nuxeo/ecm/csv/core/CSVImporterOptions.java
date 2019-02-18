/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv.core;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImporterOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LEGACY_DATE_FORMAT_PROP = "nuxeo.csv.import.legacyDateFormat";

    public static final String LEGACY_DATE_FORMAT = "MM/dd/yyyy";

    public static final CSVImporterOptions DEFAULT_OPTIONS = new Builder().build();

    public static class Builder {

        private CSVImporterDocumentFactory CSVImporterDocumentFactory = new DefaultCSVImporterDocumentFactory();

        private String dateFormat;

        private String listSeparatorRegex = "\\|";

        private Character commentMarker;

        private Character escapeCharacter = '\\';

        private boolean updateExisting = true;

        private boolean checkAllowedSubTypes = true;

        private boolean sendEmail;

        private int batchSize = 50;

        private ImportMode importMode = ImportMode.CREATE;

        public Builder documentModelFactory(CSVImporterDocumentFactory factory) {
            CSVImporterDocumentFactory = factory;
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

        public Builder commentMarker(Character commentMarker) {
            this.commentMarker = commentMarker;
            return this;
        }

        public Builder escapeCharacter(Character escapeCharacter) {
            this.escapeCharacter = escapeCharacter;
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

        public Builder importMode(ImportMode importMode) {
            this.importMode = importMode;
            return this;
        }

        public CSVImporterOptions build() {
            return new CSVImporterOptions(CSVImporterDocumentFactory, dateFormat, listSeparatorRegex, commentMarker,
                    escapeCharacter, updateExisting, checkAllowedSubTypes, sendEmail, batchSize, importMode);
        }
    }

    public enum ImportMode {
        CREATE, IMPORT
    }

    protected ImportMode importMode;

    protected final CSVImporterDocumentFactory CSVImporterDocumentFactory;

    protected final DateFormat dateFormat;

    protected final String listSeparatorRegex;

    protected final Character commentMarker;

    protected final Character escapeCharacter;

    protected final boolean updateExisting;

    protected final boolean checkAllowedSubTypes;

    protected final boolean sendEmail;

    protected final int batchSize;

    protected CSVImporterOptions(CSVImporterDocumentFactory CSVImporterDocumentFactory, String dateFormat,
            String listSeparatorRegex, boolean updateExisting, boolean checkAllowedSubTypes, boolean sendEmail,
            int batchSize, ImportMode importMode) {
        this(CSVImporterDocumentFactory, dateFormat, listSeparatorRegex, '\\', updateExisting, checkAllowedSubTypes,
                sendEmail, batchSize, importMode);
    }

    /**
     * @since 7.2
     */
    protected CSVImporterOptions(CSVImporterDocumentFactory CSVImporterDocumentFactory, String dateFormat,
            String listSeparatorRegex, Character escapeCharacter, boolean updateExisting, boolean checkAllowedSubTypes,
            boolean sendEmail, int batchSize, ImportMode importMode) {
        this(CSVImporterDocumentFactory, dateFormat, listSeparatorRegex, null, escapeCharacter, updateExisting,
                checkAllowedSubTypes, sendEmail, batchSize, importMode);
    }

    /**
     * @since 8.3
     */
    protected CSVImporterOptions(CSVImporterDocumentFactory CSVImporterDocumentFactory, String dateFormat,
            String listSeparatorRegex, Character commentMarker, Character escapeCharacter, boolean updateExisting,
            boolean checkAllowedSubTypes, boolean sendEmail, int batchSize, ImportMode importMode) {
        this.CSVImporterDocumentFactory = CSVImporterDocumentFactory;
        CSVImporterDocumentFactory.setImporterOptions(this);
        this.dateFormat = computeDateFormat(dateFormat);
        this.listSeparatorRegex = listSeparatorRegex;
        this.commentMarker = commentMarker;
        this.escapeCharacter = escapeCharacter;
        this.updateExisting = updateExisting;
        this.checkAllowedSubTypes = checkAllowedSubTypes;
        this.sendEmail = sendEmail;
        this.batchSize = batchSize;
        this.importMode = importMode;
    }

    protected DateFormat computeDateFormat(String dateFormat) {
        if (dateFormat != null) {
            return new SimpleDateFormat(dateFormat);
        }

        return Framework.getService(ConfigurationService.class).isBooleanTrue(LEGACY_DATE_FORMAT_PROP)
                ? new SimpleDateFormat(LEGACY_DATE_FORMAT)
                : null;
    }

    public CSVImporterDocumentFactory getCSVImporterDocumentFactory() {
        return CSVImporterDocumentFactory;
    }

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public String getListSeparatorRegex() {
        return listSeparatorRegex;
    }

    public Character getCommentMarker() {
        return commentMarker;
    }

    public Character getEscapeCharacter() {
        return escapeCharacter;
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

    public ImportMode getImportMode() {
        return importMode;
    }
}
