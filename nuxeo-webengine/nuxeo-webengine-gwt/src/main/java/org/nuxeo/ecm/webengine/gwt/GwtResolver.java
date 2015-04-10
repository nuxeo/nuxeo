package org.nuxeo.ecm.webengine.gwt;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.common.Environment;

public class GwtResolver {

    public static final File GWT_ROOT = new File(Environment.getDefault().getWeb(), "root.war/gwt");

    protected final Map<String, CompositeAppResolver> resolvers = new HashMap<String, CompositeAppResolver>();

    protected static final GwtAppResolver ROOT_RESOLVER = new GwtAppResolver() {

        @Override
        public URI source() {
            return GWT_ROOT.toURI();
        }

        @Override
        public File resolve(String path) {
            return new File(GWT_ROOT, path);
        }
    };

    class CompositeAppResolver {
        final Map<URI, GwtAppResolver> resolvers = new LinkedHashMap<URI, GwtAppResolver>();

        void install(GwtAppResolver resolver) {
            resolvers.put(resolver.source(), resolver);
        }

        void uninstall(URI source) {
            resolvers.remove(source);
        }

        public File resolve(String path) throws FileNotFoundException {
            for (GwtAppResolver each : resolvers.values()) {
                File file = each.resolve(path);
                if (file.exists()) {
                    return file;
                }
            }
            throw new FileNotFoundException(path);
        }
    }

    public void install(String name, URI location) throws IOException {
        File root = install(location);
        install(name, new GwtAppResolver() {

            @Override
            public URI source() {
                return location;
            }

            @Override
            public File resolve(String path) throws FileNotFoundException {
                return new File(root, path);
            }
        });

    }

    protected File install(URI location) throws IOException {
        if ("jar".equals(location.getScheme())) {
            FileSystems.newFileSystem(location, Collections.emptyMap());
        }
        Path path = Paths.get(location);
        try {
            return path.toFile();
        } catch (UnsupportedOperationException cause) {
            ;
        }
        Files.walkFileTree(path, new TreeImporter(path, GWT_ROOT.toPath()));
        return GWT_ROOT;
    }

    public void install(String name, GwtAppResolver resolver) {
        if (!resolvers.containsKey(name)) {
            resolvers.put(name, new CompositeAppResolver());
        }
        resolvers.get(name).install(resolver);
    }

    public void uninstall(String name) {
        resolvers.remove(name);
    }

    public File resolve(String path) throws FileNotFoundException {
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

    public class TreeImporter implements FileVisitor<Path> {
        final Path source;

        final Path sink;

        public TreeImporter(Path source, Path sink) {
            this.source = source;
            this.sink = sink;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (dir == source) {
                return CONTINUE;
            }
            Files.copy(dir, toSinkPath(dir), COPY_ATTRIBUTES);
            return CONTINUE;
        }

        Path toSinkPath(Path path) {
            if (path == source) {
                return sink;
            }
            path = source.relativize(path);
            path = sink.resolve(path.toString());
            return path;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, toSinkPath(file), COPY_ATTRIBUTES, REPLACE_EXISTING);
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException error) {
            if (error != null) {
                return FileVisitResult.TERMINATE;
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException error) {
            if (error != null) {
                return FileVisitResult.TERMINATE;
            }

            return CONTINUE;
        }

    }
}
