/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.services;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.InvalidationHandler;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.preload.PreloadModule;
import org.apache.shindig.gadgets.render.RenderModule;
import org.apache.shindig.gadgets.servlet.HttpRequestHandler;
import org.apache.shindig.gadgets.templates.TemplateModule;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * This replaces the DefaultGuiceModule by using our own NXRewriteModule instead
 * of the default one
 *
 * @author dmetzler
 *
 */
public class NXGuiceModule extends AbstractModule {

    /** {@inheritDoc} */
    @Override
    protected void configure() {

        final ExecutorService service = Executors.newCachedThreadPool(DAEMON_THREAD_FACTORY);
        bind(Executor.class).toInstance(service);
        bind(ExecutorService.class).toInstance(service);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                service.shutdownNow();
            }
        });

        install(new ParseModule());
        install(new PreloadModule());
        install(new RenderModule());
        install(new NXRewriteModule());
        install(new TemplateModule());

        // Handlers for /gadgets/rpc
        bind(new TypeLiteral<Set<Object>>() {
        }).annotatedWith(Names.named("org.apache.shindig.gadgets.handlers")).toInstance(
                ImmutableSet.<Object> of(InvalidationHandler.class,
                        HttpRequestHandler.class));
        // bind(Long.class).annotatedWith(Names.named("org.apache.shindig.serviceExpirationDurationMinutes")).toInstance(60l);

        // We perform static injection on HttpResponse for cache TTLs.
        requestStaticInjection(HttpResponse.class);
    }

    public static final ThreadFactory DAEMON_THREAD_FACTORY = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    };

}
