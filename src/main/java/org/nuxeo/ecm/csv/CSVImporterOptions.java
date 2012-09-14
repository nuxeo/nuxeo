package org.nuxeo.ecm.csv;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImporterOptions {

    public static final CSVImporterOptions DEFAULT_OPTIONS = new Builder().build();

    public static class Builder {

        private DocumentModelFactory documentModelFactory = new DefaultDocumentModelFactory();

        private String dateFormat = "MM/dd/yyyy";

        private String listSeparatorRegex = "\\|";

        private boolean updateExisting = true;

        private boolean sendEmail = false;

        private int batchSize = 50;

        public Builder documentModelFactory(DocumentModelFactory factory) {
            this.documentModelFactory = factory;
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

        public Builder sendEmail(boolean sendEmail) {
            this.sendEmail = sendEmail;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public CSVImporterOptions build() {
            return new CSVImporterOptions(documentModelFactory, dateFormat,
                    listSeparatorRegex, updateExisting, sendEmail, batchSize);
        }
    }

    protected final DocumentModelFactory documentModelFactory;

    protected final String dateFormat;

    protected final String listSeparatorRegex;

    protected final boolean updateExisting;

    protected final boolean sendEmail;

    protected final int batchSize;

    protected CSVImporterOptions(DocumentModelFactory documentModelFactory,
            String dateFormat, String listSeparatorRegex, boolean updateExisting,
            boolean sendEmail, int batchSize) {
        this.documentModelFactory = documentModelFactory;
        this.dateFormat = dateFormat;
        this.listSeparatorRegex = listSeparatorRegex;
        this.updateExisting = updateExisting;
        this.sendEmail = sendEmail;
        this.batchSize = batchSize;
    }

    public DocumentModelFactory getDocumentModelFactory() {
        return documentModelFactory;
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

    public boolean sendEmail() {
        return sendEmail;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
