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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.nuxeo.common.utils.FileUtils;

/**
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class PackageDownloader {

    protected final static Log log = LogFactory.getLog(PackageDownloader.class);

    protected static final int NB_DOWNLOAD_THREADS = 3;

    protected static final int NB_CHECK_THREADS = 1;

    protected static final int QUEUESIZE = 20;

    protected CopyOnWriteArrayList<PendingDownload> pendingDownloads = new CopyOnWriteArrayList<PendingDownload>();

    protected static PackageDownloader instance;

    protected DefaultHttpClient httpClient;

    protected Boolean canReachServer = null;

    protected DownloadablePackageOptions downloadOptions;

    protected static final String DIGEST = "MD5";

    protected static final int DIGEST_CHUNK = 1024 * 100;

    boolean downloadStarted = false;

    protected String lastSelectionDigest;

    public static final String BASE_URL = "http://community.nuxeo.com/static/staging/mp/"; //"http://127.0.0.1/pkgs/";

    protected final AtomicInteger dwThreadCount = new AtomicInteger(0);
    protected final AtomicInteger checkThreadCount = new AtomicInteger(0);

    protected ThreadPoolExecutor download_tpe = new ThreadPoolExecutor(
            NB_DOWNLOAD_THREADS, NB_DOWNLOAD_THREADS, 10L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(QUEUESIZE), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(false);
                    t.setName("DownloaderThread-" + dwThreadCount.incrementAndGet());
                    return t;
                }
            });

    protected ThreadPoolExecutor check_tpe = new ThreadPoolExecutor(
            NB_CHECK_THREADS, NB_CHECK_THREADS, 10L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(QUEUESIZE), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(false);
                    t.setName("MD5CheckThread-" + checkThreadCount.incrementAndGet());
                    return t;
                }
            });

    protected PackageDownloader() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http",
                PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https",
                SSLSocketFactory.getSocketFactory(), 443));
        BasicHttpParams httpParams = new BasicHttpParams();
        HttpProtocolParams.setUseExpectContinue(httpParams, false);
        ConnManagerParams.setMaxTotalConnections(httpParams, NB_DOWNLOAD_THREADS);
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, registry);
        httpClient = new DefaultHttpClient(cm, httpParams);
    }

    public synchronized static PackageDownloader instance() {
        if (instance == null) {
            instance = new PackageDownloader();
            instance.download_tpe.prestartAllCoreThreads();
            instance.check_tpe.prestartAllCoreThreads();
        }
        return instance;
    }

    public void setProxy (String proxy, int port, String login, String password) {
        if (proxy!=null) {
            HttpHost proxyHost = new HttpHost(proxy, port);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
            if (login!=null) {
                httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(proxy, port),
                    new UsernamePasswordCredentials(login, password));
            }
        }
    }

    protected String getSelectionDigest(List<String> ids) {
        ArrayList<String> lst = new ArrayList<String>(ids);
        Collections.sort(lst);
        StringBuffer sb = new StringBuffer();
        for (String item : lst) {
            sb.append(item);
            sb.append(":");
        }
        return sb.toString();
    }

    public void selectOptions(List<String> ids) {
        String newSelectionDigest = getSelectionDigest(ids);
        if (lastSelectionDigest != null) {
            if (lastSelectionDigest.equals(newSelectionDigest)) {
                return;
            }
        }
        getPackageOptions().select(ids);
        downloadStarted = false;
        lastSelectionDigest = newSelectionDigest;
    }

    protected File getDownloadDirectory() {
        // XXX do better !
        File tmp = new File(System.getProperty("java.io.tmpdir"));
        File dir = new File(tmp, "testDownload");
        if (!dir.exists()) {
            dir.mkdirs();
            dir = new File(tmp, "testDownload");
        }
        return dir;
    }

    public boolean canReachServer() {
        if (canReachServer == null) {
            HttpGet ping = new HttpGet(BASE_URL + "packages.xml");
            try {
                HttpResponse response = httpClient.execute(ping);
                if (response.getStatusLine().getStatusCode() == 200) {
                    canReachServer = true;
                } else {
                    log.error("Unable to ping server -> status code :"
                            + response.getStatusLine().getStatusCode() + " ("
                            + response.getStatusLine().getReasonPhrase() + ")");
                    canReachServer = false;
                }
            } catch (Exception e) {
                log.error("Unable to ping server", e);
                canReachServer = false;
            }
        }
        return canReachServer;
    }

    public DownloadablePackageOptions getPackageOptions() {
        if (downloadOptions == null) {
            File packageFile = null;
            if (canReachServer()) {
                packageFile = getRemotePackagesDescriptor();
            }
            if (packageFile == null) {
                packageFile = getLocalPackagesDescriptor();
            }
            if (packageFile != null) {
                try {
                    downloadOptions = DownloadDescriptorParser.parsePackages(new FileInputStream(
                            packageFile));
                } catch (FileNotFoundException e) {
                    log.error("Unable to read packages.xml", e);
                }
            }
        }
        return downloadOptions;
    }

    protected File getRemotePackagesDescriptor() {
        File desc = null;
        HttpGet ping = new HttpGet(BASE_URL + "packages.xml");
        try {
            HttpResponse response = httpClient.execute(ping);
            if (response.getStatusLine().getStatusCode() == 200) {
                desc = new File(getDownloadDirectory(), "packages.xml");
                FileUtils.copyToFile(response.getEntity().getContent(), desc);
            } else {
                log.error("Unable to download remote packages.xml, status code :"
                        + response.getStatusLine().getStatusCode()
                        + " ("
                        + response.getStatusLine().getReasonPhrase() + ")");
                return null;
            }
        } catch (Exception e) {
            log.error("Unable to reach remote packages.xml", e);
            return null;
        }
        return desc;
    }

    protected File getLocalPackagesDescriptor() {
        File desc = new File(getDownloadDirectory(), "packages.xml");
        if (desc.exists()) {
            return desc;
        }
        return null;
    }

    public List<DownloadPackage> getSelectedPackages() {
        List<DownloadPackage> pkgs = downloadOptions.getPkg4Download();
        for (DownloadPackage pkg : pkgs) {
            if (needToDownload(pkg)) {
                pkg.setAlreadyInLocal(false);
            } else {
                pkg.setAlreadyInLocal(true);
            }
        }
        return pkgs;
    }

    public List<PendingDownload> getPendingDownloads() {
        return pendingDownloads;
    }

    public void reStartDownload(String id) {
        for (PendingDownload pending : pendingDownloads) {
            if (pending.getPkg().getId().equals(id)) {
                if (Arrays.asList(PendingDownload.CORRUPTED,
                        PendingDownload.ABORTED).contains(pending.getStatus())) {
                    pendingDownloads.remove(pending);
                    startDownloadPackage(pending.getPkg());
                }
                break;
            }
        }
    }

    public void startDownload() {
        startDownload(downloadOptions.getPkg4Download());
    }

    public void startDownload(List<DownloadPackage> pkgs) {
        downloadStarted = true;
        for (DownloadPackage pkg : pkgs) {
            if (needToDownload(pkg)) {
                startDownloadPackage(pkg);
            }
        }
    }

    protected boolean needToDownload(DownloadPackage pkg) {
        for (File file : getDownloadDirectory().listFiles()) {
            if (file.getName().equals(pkg.getMd5())) {
                // recheck md5 ???
                return false;
            }
        }
        return true;
    }

    protected void startDownloadPackage(final DownloadPackage pkg) {
        final PendingDownload download = new PendingDownload(pkg);
        if (pendingDownloads.addIfAbsent(download)) {
            Runnable downloadRunner = new Runnable() {

                @Override
                public void run() {
                    log.info("Starting download on Thread " + Thread.currentThread().getName());
                    download.setStatus(PendingDownload.INPROGRESS);
                    String url = pkg.getDownloadUrl();
                    if (!url.startsWith("http")) {
                        if (!BASE_URL.endsWith("/")) {
                            url = BASE_URL + "/" + url;
                        } else {
                            url = BASE_URL + url;
                        }
                    }
                    File filePkg = null;
                    HttpGet dw = new HttpGet(url);
                    try {
                        HttpResponse response = httpClient.execute(dw);
                        if (response.getStatusLine().getStatusCode() == 200) {
                            filePkg = new File(getDownloadDirectory(),
                                    pkg.filename);
                            Header clh = response.getFirstHeader("Content-Length");
                            if (clh != null) {
                                long filesize = Long.parseLong(clh.getValue());
                                download.setFile(filesize, filePkg);
                            }
                            FileUtils.copyToFile(
                                    response.getEntity().getContent(), filePkg);
                            download.setStatus(PendingDownload.COMPLETED);
                        } else if (response.getStatusLine().getStatusCode() == 404) {
                            log.error("Package " + pkg.filename
                                    + " not found :" + url);
                            download.setStatus(PendingDownload.MISSING);
                            return;
                        } else {
                            log.error("Received StatusCode "
                                    + response.getStatusLine().getStatusCode());
                            download.setStatus(PendingDownload.ABORTED);
                            return;
                        }
                    } catch (Exception e) {
                        download.setStatus(PendingDownload.ABORTED);
                        log.error("Error during download", e);
                        return;
                    }
                    checkPackage(download);
                }
            };
            download_tpe.execute(downloadRunner);
        }
    }

    protected void checkPackage(final PendingDownload download) {
        final File filePkg = download.getDowloadingFile();
        Runnable checkRunner = new Runnable() {
            @Override
            public void run() {
                download.setStatus(PendingDownload.VERIFICATION);
                String expectedDigest = download.getPkg().getMd5();
                String digest = getDigest(filePkg);
                if (digest == null
                        || (expectedDigest != null && !expectedDigest.equals(digest))) {
                    download.setStatus(PendingDownload.CORRUPTED);
                    log.error("Digest check failed : expected :"
                            + expectedDigest + " computed :" + digest);
                    return;
                }
                filePkg.renameTo(new File(getDownloadDirectory(), digest));
                download.setStatus(PendingDownload.VERIFIED);
            }
        };
        check_tpe.execute(checkRunner);
    }

    protected String getDigest(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance(DIGEST);
            byte[] buffer = new byte[DIGEST_CHUNK];
            InputStream stream = new FileInputStream(file);
            int bytesRead = -1;
            while ((bytesRead = stream.read(buffer)) >= 0) {
                md.update(buffer, 0, bytesRead);
            }
            stream.close();
            byte[] b = md.digest();
            return md5ToHex(b);
        } catch (Exception e) {
            log.error("Error while computing Digest ", e);
            return null;
        }
    }

    protected static String md5ToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public boolean isDownloadStarted() {
        return downloadStarted;
    }

    public boolean isDownloadCompleted() {
        if (!isDownloadStarted()) {
            return false;
        }
        for (PendingDownload download : pendingDownloads) {
            if (download.getStatus() < PendingDownload.VERIFIED) {
                return false;
            }
        }
        return true;
    }

    public boolean isDownloadInProgress() {
        if (!isDownloadStarted()) {
            return false;
        }
        if (isDownloadCompleted()) {
            return false;
        }
        int nbInProgress = 0;
        for (PendingDownload download : pendingDownloads) {
            if (download.getStatus() < PendingDownload.VERIFIED
                    && download.getStatus() >= PendingDownload.PENDING) {
                nbInProgress++;
            }
        }
        return nbInProgress > 0;
    }

}
