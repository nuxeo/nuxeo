package org.nuxeo.apidoc.documentation;

public enum DefaultDocumentationType {

    DESCRIPTION ("description"),
    CODE_SAMPLE ("codeSample"),
    HOW_TO ("howTo");

    private final String value;

    DefaultDocumentationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
