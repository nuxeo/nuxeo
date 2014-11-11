package org.nuxeo.runtime.osgi.util.jar.index;

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