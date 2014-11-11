package org.nuxeo.osgi.nio;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

public class PatternFilter<T extends Path> {

    protected String syntaxAndPattern;

    protected PatternFilter(String syntaxAndPattern) {
        this.syntaxAndPattern = syntaxAndPattern;
    }

    public DirectoryStream.Filter<T> newFilter(final FileSystem fs) {
        return new DirectoryStream.Filter<T>() {
            final PathMatcher matcher = fs.getPathMatcher(syntaxAndPattern);

            @Override
            public boolean accept(T entry) throws IOException {
                return matcher.matches(entry);
            }
        };
    }

}