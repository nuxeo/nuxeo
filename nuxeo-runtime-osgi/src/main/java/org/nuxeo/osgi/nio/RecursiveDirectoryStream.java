package org.nuxeo.osgi.nio;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.concurrent.NamedThreadFactory;

public class RecursiveDirectoryStream<T extends Path> implements
        DirectoryStream<T> {

    protected class Walker implements Callable<Void> {

        protected final T root;

        protected final Filter<T> filter;

        protected Walker(T root, Filter<T> filter) {
            this.root = root;
            this.filter = filter;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Void call() throws Exception {
            try {
                Files.walkFileTree(root,
                        (FileVisitor<? super Path>) new FileVisitor<T>() {

                            @Override
                            public FileVisitResult preVisitDirectory(T dir,
                                    BasicFileAttributes attrs)
                                    throws IOException {
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(T file,
                                    BasicFileAttributes attrs)
                                    throws IOException {
                                if (filter.accept(file)) {
                                    pathsBlockingQueue.offer(file);
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(T file,
                                    IOException exc) throws IOException {
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(T dir,
                                    IOException exc) throws IOException {
                                return FileVisitResult.CONTINUE;
                            }

                        });
                return null;
            } catch (Exception e) {
                LogFactory.getLog(RecursiveDirectoryStream.class).error(
                        "Cannot walk tree " + root, e);
                throw e;
            }
        }
    }

    protected final LinkedBlockingQueue<T> pathsBlockingQueue = new LinkedBlockingQueue<>();

    protected final T startPath;

    protected final Filter<T> filter;

    protected FutureTask<Void> walkTask;

    protected static final ExecutorService walkExecutor = newExecutor();

    public RecursiveDirectoryStream(T startPath,
            DirectoryStream.Filter<T> filter) {
        this.startPath = Objects.requireNonNull(startPath);
        this.filter = filter;
    }

    @Override
    public Iterator<T> iterator() {
        findFiles(startPath, filter);
        return new Iterator<T>() {
            T path;

            @Override
            public boolean hasNext() {
                try {
                    path = pathsBlockingQueue.poll();
                    while (!walkTask.isDone() && path == null) {
                        path = pathsBlockingQueue.poll(5, TimeUnit.MILLISECONDS);
                    }
                    return (path != null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return false;
            }

            @Override
            public T next() {
                return path;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Removal not supported");
            }
        };
    }

    protected void findFiles(final T root, final Filter<T> filter) {
        walkTask = new FutureTask<>(new Walker(root, filter));
        launchWalkTask();
    }

    protected static ExecutorService newExecutor() {
        return Executors.newFixedThreadPool(6, new NamedThreadFactory(
                "directory-walker"));
    }

    protected void launchWalkTask() {
        walkExecutor.submit(walkTask);
    }

    @Override
    public void close() throws IOException {
        if (walkTask != null) {
            walkTask.cancel(true);
        }
        pathsBlockingQueue.clear();
        walkTask = null;
    }

}
