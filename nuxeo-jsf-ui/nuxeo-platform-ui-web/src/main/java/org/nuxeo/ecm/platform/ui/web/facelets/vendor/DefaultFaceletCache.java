/*
 * (C) Copyright 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 */

package org.nuxeo.ecm.platform.ui.web.facelets.vendor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletCache;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.platform.ui.web.application.NuxeoUnknownResource;

import com.sun.faces.util.ConcurrentCache;
import com.sun.faces.util.ExpiringConcurrentCache;
import com.sun.faces.util.FacesLogger;
import com.sun.faces.util.Util;

/**
 * Default FaceletCache implementation.
 * <p>
 * Overrides the default facelet cache for the following reasons:
 * <ul>
 * <li>fix a bug in the refresh cache, incrementing next refresh time too much (and then breaking the refresh mechanism)
 * </li>
 * </ul>
 *
 * @since 6.0: copy/paste of default implementation because constructor is package private, and changing private
 *        DefaultFacelet implementation references to {@link Facelet}, and for bufix/adapt.
 */
final class DefaultFaceletCache extends FaceletCache<Facelet> {

    @SuppressWarnings("unused")
    private final static Logger LOGGER = FacesLogger.FACELETS_FACTORY.getLogger();

    /**
     * Constructor
     *
     * @param refreshPeriod cache refresh period (in seconds). 0 means 'always refresh', negative value means 'never
     *            refresh'
     */
    DefaultFaceletCache(final long refreshPeriod) {

        // We will be delegating object storage to the ExpiringCocurrentCache
        // Create Factory objects here for the cache. The objects will be
        // delegating to our
        // own instance factories

        final boolean checkExpiry = (refreshPeriod > 0);

        ConcurrentCache.Factory<URL, Record> faceletFactory = new ConcurrentCache.Factory<URL, Record>() {
            @Override
            public Record newInstance(final URL key) throws IOException {
                // Make sure that the expensive timestamp retrieval is not done
                // if no expiry check is going to be performed
                long lastModified = checkExpiry ? Util.getLastModified(key) : 0;
                return new Record(System.currentTimeMillis(), lastModified, getMemberFactory().newInstance(key),
                        refreshPeriod);
            }
        };

        ConcurrentCache.Factory<URL, Record> metadataFaceletFactory = new ConcurrentCache.Factory<URL, Record>() {
            @Override
            public Record newInstance(final URL key) throws IOException {
                // Make sure that the expensive timestamp retrieval is not done
                // if no expiry check is going to be performed
                long lastModified = checkExpiry ? Util.getLastModified(key) : 0;
                return new Record(System.currentTimeMillis(), lastModified,
                        getMetadataMemberFactory().newInstance(key), refreshPeriod);
            }
        };

        // No caching if refreshPeriod is 0
        if (refreshPeriod == 0) {
            _faceletCache = new NoCache(faceletFactory);
            _metadataFaceletCache = new NoCache(metadataFaceletFactory);
        } else {
            ExpiringConcurrentCache.ExpiryChecker<URL, Record> checker = (refreshPeriod > 0) ? new ExpiryChecker()
                    : new NeverExpired();
            _faceletCache = new ExpiringConcurrentCache<>(faceletFactory, checker);
            _metadataFaceletCache = new ExpiringConcurrentCache<>(metadataFaceletFactory, checker);
        }
    }

    @Override
    public Facelet getFacelet(URL url) throws IOException {
        com.sun.faces.util.Util.notNull("url", url);

        Facelet f = null;

        try {
            // Nuxeo patches to avoid crash on unknown facelets
            String path = url.getPath();
            if (path.contains(NuxeoUnknownResource.MARKER)) {
                f = getErrorFacelet(url, null);
            } else {
                f = _faceletCache.get(url).getFacelet();
            }
            if (f == null) {
                f = getErrorFacelet(url, null);
            }
        } catch (ExecutionException e) {
            f = getErrorFacelet(url, e);
        }

        return f;
    }

    /**
     * Build error facelet, taking into account the fact that depending on resource inclusion (absolute or relative), an
     * error might happen when checking the last modified information.
     *
     * @since 6.0
     */
    protected Facelet getErrorFacelet(URL url, ExecutionException origError) throws IOException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            if (origError != null) {
                _unwrapIOException(origError);
            }
            return null;
        }

        boolean errorShouldBeCaught = origError != null;
        if (errorShouldBeCaught) {
            String errorMessage = origError.getMessage();
            if (errorMessage != null && errorMessage.contains("Error Checking Last Modified for ")) {
                errorShouldBeCaught = false;
            }
            Throwable t = origError.getCause();
            if (t instanceof FileNotFoundException) {
                errorShouldBeCaught = false;
            }
        }
        if (errorShouldBeCaught) {
            _unwrapIOException(origError);
            return null;
        }

        try {
            // wrap with marker
            String path = url.getPath();
            String lookupPath;
            if (path.startsWith(NuxeoUnknownResource.MARKER)) {
                lookupPath = path;
            } else {
                lookupPath = NuxeoUnknownResource.MARKER + path;
            }
            ViewResource res = ctx.getApplication().getResourceHandler().createViewResource(ctx, lookupPath);
            if (res != null) {
                return _faceletCache.get(res.getURL()).getFacelet();
            }
            return null;
        } catch (ExecutionException e) {
            _unwrapIOException(e);
            return null;
        }
    }

    @Override
    public boolean isFaceletCached(URL url) {
        com.sun.faces.util.Util.notNull("url", url);

        return _faceletCache.containsKey(url);
    }

    @Override
    public Facelet getViewMetadataFacelet(URL url) throws IOException {
        com.sun.faces.util.Util.notNull("url", url);

        Facelet f = null;

        try {
            f = _metadataFaceletCache.get(url).getFacelet();
        } catch (ExecutionException e) {
            _unwrapIOException(e);
        }
        return f;
    }

    @Override
    public boolean isViewMetadataFaceletCached(URL url) {
        com.sun.faces.util.Util.notNull("url", url);

        return _metadataFaceletCache.containsKey(url);
    }

    private void _unwrapIOException(ExecutionException e) throws IOException {
        Throwable t = e.getCause();
        if (t instanceof IOException) {
            throw (IOException) t;
        }
        if (t.getCause() instanceof IOException) {
            throw (IOException) t.getCause();
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new FacesException(t);
    }

    private final ConcurrentCache<URL, Record> _faceletCache;

    private final ConcurrentCache<URL, Record> _metadataFaceletCache;

    /**
     * This class holds the Facelet instance and its original URL's last modified time. It also produces the time when
     * the next expiry check should be performed
     */
    private static class Record {
        Record(long creationTime, long lastModified, Facelet facelet, long refreshInterval) {
            _facelet = facelet;
            _lastModified = lastModified;
            _refreshInterval = refreshInterval;

            // There is no point in calculaing the next refresh time if we are
            // refreshing always/never
            _nextRefreshTime = (_refreshInterval > 0) ? new AtomicLong(creationTime + refreshInterval) : null;
        }

        Facelet getFacelet() {
            return _facelet;
        }

        long getLastModified() {
            return _lastModified;
        }

        long getNextRefreshTime() {
            // There is no point in calculating the next refresh time if we are
            // refreshing always/never
            // Nuxeo patch: do not increment everytime on get
            return (_refreshInterval > 0) ? _nextRefreshTime.longValue() : 0;
            // return (_refreshInterval > 0) ?
            // _nextRefreshTime.getAndAdd(_refreshInterval) : 0;
        }

        private final long _lastModified;

        private final long _refreshInterval;

        private final AtomicLong _nextRefreshTime;

        private final Facelet _facelet;
    }

    private static class ExpiryChecker implements ExpiringConcurrentCache.ExpiryChecker<URL, Record> {

        @Override
        public boolean isExpired(URL url, Record record) {
            // getNextRefreshTime() increments the next refresh time
            // atomically
            // Nuxeo patch: do not increment it every time a check is done.
            long ttl = record.getNextRefreshTime();
            if (System.currentTimeMillis() > ttl) {
                long lastModified = Util.getLastModified(url);
                // The record is considered expired if its original last
                // modified time
                // is older than the URL's current last modified time
                return (lastModified > record.getLastModified());
            }
            return false;
        }
    }

    private static class NeverExpired implements ExpiringConcurrentCache.ExpiryChecker<URL, Record> {
        @Override
        public boolean isExpired(URL key, Record value) {
            return false;
        }
    }

    /**
     * ConcurrentCache implementation that does no caching (always creates new instances)
     */
    private static class NoCache extends ConcurrentCache<URL, Record> {
        public NoCache(ConcurrentCache.Factory<URL, Record> f) {
            super(f);
        }

        @Override
        public Record get(final URL key) throws ExecutionException {
            try {
                return this.getFactory().newInstance(key);
            } catch (Exception e) { // stupid Faces API throws Exception
                ExceptionUtils.checkInterrupt(e);
                throw new ExecutionException(e);
            }
        }

        @Override
        public boolean containsKey(final URL key) {
            return false;
        }
    }
}
