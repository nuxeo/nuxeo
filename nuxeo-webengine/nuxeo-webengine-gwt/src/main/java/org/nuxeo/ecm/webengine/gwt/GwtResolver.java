package org.nuxeo.ecm.webengine.gwt;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.Environment;

public class GwtResolver {

    public static final File GWT_ROOT = new File(Environment.getDefault().getWeb(), "root.war/gwt");

    protected final Map<String, GwtAppResolver> resolvers = new HashMap<String, GwtAppResolver>();

    protected static final GwtAppResolver ROOT_RESOLVER = new GwtAppResolver() {

        @Override
        public File resolve(String path) {
            return new File(GWT_ROOT, path);
        }
    };

    public void install(String name, URI location) throws IOException {
        File root = install(location);
        resolvers.put(name, new GwtAppResolver() {

            @Override
            public File resolve(String pathname) {
                return new File(root, pathname);
            }

        });
    }

    protected File install(URI location) throws IOException {
        Path path = Paths.get(location);
        try {
            return path.toFile();
        } catch (UnsupportedOperationException cause) {
            ;
        }
        Path rootPath = GWT_ROOT.toPath();
        Files.walkFileTree(path.relativize(path), new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectory(rootPath.resolve(dir));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, rootPath.resolve(file));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw new IOException("Cannot copy " + file, exc);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return GWT_ROOT;
    }

    public void install(String name, GwtAppResolver resolver) {
        resolvers.put(name, resolver);
    }

    public void uninstall(String name) {
        resolvers.remove(name);
    }

    public File resolve(String path) {
        int indexOf = path.indexOf('/');
        if (indexOf == -1) {
            if (resolvers.containsKey(path)) {
                return resolvers.get(path).resolve("/");
            }
            return new File(GWT_ROOT, path);
        }
        String name = path.substring(0, indexOf);
        if (resolvers.containsKey(name)) {
            return resolvers.get(name).resolve(path);
        }
        return ROOT_RESOLVER.resolve(path);
    }
}
