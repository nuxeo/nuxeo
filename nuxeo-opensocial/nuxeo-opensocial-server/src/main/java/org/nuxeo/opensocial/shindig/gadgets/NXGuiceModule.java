package org.nuxeo.opensocial.shindig.gadgets;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.preload.HttpPreloader;
import org.apache.shindig.gadgets.preload.Preloader;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.nuxeo.opensocial.shindig.oauth.NXOAuthDataStore;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class NXGuiceModule extends AbstractModule {

    /** {@inheritDoc} */
    @Override
    protected void configure() {

        ExecutorService service = Executors.newCachedThreadPool();
        bind(Executor.class).toInstance(service);
        bind(ExecutorService.class).toInstance(service);

        this.install(new ParseModule());

        // bind(new TypeLiteral<List<ContentRewriter>>() {
        // }).toProvider(ContentRewritersProvider.class);
        bind(new TypeLiteral<List<Preloader>>() {
        }).toProvider(PreloaderProvider.class);

        bind(OAuthDataStore.class).to(NXOAuthDataStore.class);
        // We perform static injection on HttpResponse for cache TTLs.
        requestStaticInjection(HttpResponse.class);
    }

    // private static class ContentRewritersProvider implements
    // Provider<List<ContentRewriter>> {
    // private final List<ContentRewriter> rewriters;
    //
    // @Inject
    // public ContentRewritersProvider(NXContentRewriter optimizingRewriter,
    // CajaContentRewriter cajaRewriter,
    // RenderingContentRewriter renderingRewriter,
    // NXCSSContentRewriter cssRewriter) {
    // rewriters = Lists.newArrayList();
    // rewriters.add(optimizingRewriter);
    // rewriters.add(cajaRewriter);
    // rewriters.add(renderingRewriter);
    // rewriters.add(cssRewriter);
    // }
    //
    // public List<ContentRewriter> get() {
    // return rewriters;
    // }
    // }

    private static class PreloaderProvider implements Provider<List<Preloader>> {
        private final List<Preloader> preloaders;

        @Inject
        public PreloaderProvider(HttpPreloader httpPreloader) {
            preloaders = Lists.<Preloader> newArrayList(httpPreloader);
        }

        public List<Preloader> get() {
            return preloaders;
        }
    }

}
