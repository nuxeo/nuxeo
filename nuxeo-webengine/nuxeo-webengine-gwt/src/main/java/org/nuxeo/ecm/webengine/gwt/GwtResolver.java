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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.nuxeo.common.Environment;

public class GwtResolver {

    public static final File GWT_ROOT = new File(Environment.getDefault().getWeb(), "root.war/gwt");

    protected final Map<String, CompositeStrategy> strategies = new HashMap<String, CompositeStrategy>();

    protected static final GwtAppResolver.Strategy ROOT_RESOLVER_STRATEGY = new GwtAppResolver.Strategy() {

        @Override
        public URI source() {
            return GWT_ROOT.toURI();
        }

        @Override
        public File resolve(String path) {
            return new File(GWT_ROOT, path);
        }
    };

    class CompositeStrategy {
        final Map<URI, GwtAppResolver.Strategy> strategiesByKey = new HashMap<URI, GwtAppResolver.Strategy>();

        final List<GwtAppResolver.Strategy> strategies = new ArrayList<GwtAppResolver.Strategy>();

        void install(GwtAppResolver.Strategy strategy) {
            strategiesByKey.put(strategy.source(), strategy);
            strategies.add(strategy);
        }

        void uninstall(URI source) {
            GwtAppResolver.Strategy strategy = strategiesByKey.remove(source);
            if (strategy == null) {
                return;
            }
            strategies.remove(strategy);
        }

        public File resolve(String path) throws FileNotFoundException {
            ListIterator<GwtAppResolver.Strategy> it = strategies.listIterator(strategies.size());
            while (it.hasPrevious()) {
                File file = it.previous().resolve(path);
                if (file.exists()) {
                    return file;
                }
            }
            return null;
        }
    }

    public GwtAppResolver.Strategy newStrategy(URI location) throws IOException {
        File root = install(location);
        return new GwtAppResolver.Strategy() {

            @Override
            public URI source() {
                return location;
            }

            @Override
            public File resolve(String path) throws FileNotFoundException {
                return new File(root, path);
            }
        };
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

    public void install(String name, GwtAppResolver.Strategy strategy) {
        if (!strategies.containsKey(name)) {
            strategies.put(name, new CompositeStrategy());
        }
        strategies.get(name).install(strategy);
    }

    public void install(String name, URI location) throws IOException {
        install(name, newStrategy(location));
    }

    public void uninstall(String name) {
        strategies.remove(name);
    }

    public File resolve(String path) throws FileNotFoundException {
        int indexOf = path.indexOf('/');
        if (indexOf == -1) {
            if (strategies.containsKey(path)) {
                return strategies.get(path).resolve("/");
            }
            return ROOT_RESOLVER_STRATEGY.resolve(path);
        }
        String name = path.substring(0, indexOf);
        if (strategies.containsKey(name)) {
            return strategies.get(name).resolve(path);
        }
        return ROOT_RESOLVER_STRATEGY.resolve(path);
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
