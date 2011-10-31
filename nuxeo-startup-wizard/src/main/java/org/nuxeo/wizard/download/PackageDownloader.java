/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tdelprat
 *
 */

package org.nuxeo.wizard.download;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class PackageDownloader {

    protected static final int NB_THREADS = 3;

    protected static final int QUEUESIZE = 20;

    protected CopyOnWriteArrayList<String> pendingDownload = new CopyOnWriteArrayList<String>();

    protected ThreadPoolExecutor tpe = new ThreadPoolExecutor(0, NB_THREADS,
            NB_THREADS, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(
                    QUEUESIZE), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(false);
                    t.setName("DownloaderThread");
                    return t;
                }
            });

    protected static PackageDownloader instance;

    public synchronized static PackageDownloader instance() {
        if (instance == null) {
            instance = new PackageDownloader();
            instance.tpe.prestartAllCoreThreads();
        }
        return instance;
    }

    protected File getDownloadDirectory() {
        // XXX do better !
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public void startDownload(DownloadablePackageOptions options) {
        startDownload(options.getPkg4Download());
    }

    public void startDownload(List<DownloadPackage> pkgs) {

        for (DownloadPackage pkg : pkgs) {
            if (needToDownload(pkg)) {
                startDownloadPackage(pkg);
            }
        }

    }

    protected boolean needToDownload(DownloadPackage pkg) {
        for (File file : getDownloadDirectory().listFiles()) {
            if (file.getName().equals(pkg.getMd5())) {
                return false;
            }
        }
        return true;
    }

    protected void startDownloadPackage(final DownloadPackage pkg) {

        Runnable download = new Runnable() {

            @Override
            public void run() {
                String url = pkg.getDownloadUrl();



            }
        };
        tpe.execute(download);
    }
}
