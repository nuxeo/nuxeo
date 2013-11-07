package org.nuxeo.ecm.platform.importer.flat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunner;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter.NamedThreadFactory;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class FlatMultiThreadedImporter implements ImporterRunner {

    protected final ThreadPoolExecutor importTP;

    protected final Path startingPoint;

    protected ConcurrentLinkedQueue<Path> pendingFiles = new ConcurrentLinkedQueue<Path>();

    public FlatMultiThreadedImporter(SourceNode sourceNode,
            String importWritePath, Boolean skipRootContainerCreation,
            Integer batchSize, Integer nbThreads, ImporterLogger log)
            throws Exception {

        importTP = new ThreadPoolExecutor(nbThreads, nbThreads, 500L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100),
                new NamedThreadFactory("Nuxeo-Importer-"));
        startingPoint = ((FileSourceNode) sourceNode).getFile().toPath();

    }

    @Override
    public void run() {

        try {
            Files.walkFileTree(startingPoint, new FileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                        BasicFileAttributes attrs) throws IOException {
                    // TODO Auto-generated method stub

                    return null;

                }

                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    pendingFiles.offer(file);
                    return null;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc)
                        throws IOException {
                    // TODO Auto-generated method stub
                    return null;
                }

            });

            Thread.sleep(2000);

            while (!pendingFiles.isEmpty()) {
                Thread.sleep(1000);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    @Override
    public void stopImportProcrocess() {
        // TODO Auto-generated method stub

    }

}
