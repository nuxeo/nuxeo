package org.nuxeo.runtime.tomcat.adapter.tests.metaindex;

enum JarFileKind {

    CLASSONLY('!'), RESOURCEONLY('@'), MIXED('#');

    private char markerChar;

    JarFileKind(char markerChar) {
        this.markerChar = markerChar;
    }

    public char getMarkerChar() {
        return markerChar;
    }
}