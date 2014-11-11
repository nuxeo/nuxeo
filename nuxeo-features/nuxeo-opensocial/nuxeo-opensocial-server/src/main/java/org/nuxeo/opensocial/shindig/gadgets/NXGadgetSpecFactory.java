package org.nuxeo.opensocial.shindig.gadgets;

import java.net.URI;
import java.util.logging.Logger;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.SoftExpiringCache;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Check;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.DefaultGadgetSpecFactory;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth.OAuthFetcher;
import org.apache.shindig.gadgets.oauth.OAuthFetcherConfig;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.nuxeo.opensocial.shindig.oauth.SignedRequestAuthenticationInfo;
import org.nuxeo.opensocial.shindig.oauth.SimpleSignedOAuthFetcherConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Custom GadgetSpecFactory that signs requests when fetching gadgets from a
 * Nuxeo host.
 * 
 * @author dmetzler
 */
@Singleton
public class NXGadgetSpecFactory implements GadgetSpecFactory {
    private static final int ONE_SECOND = 1000;

    private static final String WEBENGINE_GET_DEF_ACTION = "@@getDef";

    public static final String CACHE_NAME = "gadgetSpecs";

    static final String RAW_GADGETSPEC_XML_PARAM_NAME = "rawxml";

    static final Uri RAW_GADGET_URI = Uri.parse("http://localhost/raw.xml");

    static final String ERROR_SPEC = "<Module><ModulePrefs title='Error'/><Content/></Module>";

    static final String ERROR_KEY = "parse.exception";

    static final Logger LOG = Logger.getLogger(DefaultGadgetSpecFactory.class.getName());

    private final HttpFetcher fetcher;

    private final OAuthFetcherConfig oAuthFetcherConfig;

    private final SoftExpiringCache<Uri, GadgetSpec> cache;

    private final long refresh;

    @Inject
    public NXGadgetSpecFactory(HttpFetcher fetcher,
            CacheProvider cacheProvider,
            @Named("shindig.cache.xml.refreshInterval") long refresh,
            SimpleSignedOAuthFetcherConfig oauthFetcherConfig) {
        this.fetcher = fetcher;
        this.oAuthFetcherConfig = oauthFetcherConfig;
        Cache<Uri, GadgetSpec> baseCache = cacheProvider.createCache(CACHE_NAME);
        this.cache = new SoftExpiringCache<Uri, GadgetSpec>(baseCache);
        this.refresh = refresh;
    }

    public GadgetSpec getGadgetSpec(GadgetContext context)
            throws GadgetException {
        String rawxml = context.getParameter(RAW_GADGETSPEC_XML_PARAM_NAME);
        if (rawxml != null) {
            // Set URI to a fixed, safe value (localhost), preventing a gadget
            // rendered
            // via raw XML (eg. via POST) to be rendered on a locked domain of
            // any
            // other
            // gadget whose spec is hosted non-locally.
            return new GadgetSpec(RAW_GADGET_URI, rawxml);
        }
        return getGadgetSpec(context, context.getIgnoreCache());
    }

    public GadgetSpec getGadgetSpec(GadgetContext context, boolean ignoreCache)
            throws GadgetException {

        Uri uri = Uri.fromJavaUri(context.getUrl());
        if (ignoreCache) {
            return fetchObjectAndCache(uri, context, ignoreCache);
        }

        SoftExpiringCache.CachedObject<GadgetSpec> cached = cache.getElement(uri);

        GadgetSpec spec = null;
        if (cached == null || cached.isExpired) {
            try {
                spec = fetchObjectAndCache(uri, context, ignoreCache);
            } catch (GadgetException e) {
                // Enforce negative caching.
                if (cached != null) {
                    spec = cached.obj;
                    Check.notNull(spec);
                } else {
                    // We create this dummy spec to avoid the cost of re-parsing
                    // when a
                    // remote site is out.
                    spec = new GadgetSpec(uri, ERROR_SPEC);
                    spec.setAttribute(ERROR_KEY, e);
                }
                LOG.info("GadgetSpec fetch failed for " + uri
                        + " - using cached.");
                cache.addElement(uri, spec, refresh);
            }
        } else {
            spec = cached.obj;
        }

        GadgetException exception = (GadgetException) spec.getAttribute(ERROR_KEY);
        if (exception != null) {
            throw exception;
        }
        return spec;
    }

    /**
     * Retrieves a gadget specification from the Internet, processes its views
     * and adds it to the cache.
     */
    private GadgetSpec fetchObjectAndCache(Uri uri, GadgetContext context,
            boolean ignoreCache) throws GadgetException {

        HttpRequest request = new HttpRequest(uri).setIgnoreCache(ignoreCache);
        HttpResponse response = null;

        // Since we don't allow any variance in cache time, we should just force
        // the
        // cache time
        // globally. This ensures propagation to shared caches when this is set.
        request.setCacheTtl((int) (refresh / ONE_SECOND));

        // For now, if there is a token, we use it to send a signed message
        // if (context.getToken() != null
        // && uri.getPath().contains(WEBENGINE_GET_DEF_ACTION)) {
        if (context.getToken() != null) {
            AuthType auth = AuthType.SIGNED;
            request.setAuthType(auth);
            SecurityToken token = context.getToken();
            request.setSecurityToken(token);

            request.setOAuthArguments(new OAuthArguments(
                    new SignedRequestAuthenticationInfo(token.getOwnerId(),
                            token.getViewerId(), uri)));

            OAuthFetcher oauthFetcher = new OAuthFetcher(
                    this.oAuthFetcherConfig, fetcher, request);

            response = oauthFetcher.fetch(request);

        } else {
            /*
             * if (request.getGadget() == null) {
             * LOG.warning("patching up get gadget spec request for " +
             * "non-authed version :" + uri); request.setGadget(uri); }
             */
            response = fetcher.fetch(request);
        }

        if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
            throw new GadgetException(
                    GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                    "Unable to retrieve gadget xml. HTTP error "
                            + response.getHttpStatusCode());
        }

        GadgetSpec spec = new GadgetSpec(uri, response.getResponseAsString());
        cache.addElement(uri, spec, refresh);
        return spec;
    }

    public GadgetSpec getGadgetSpec(URI uri, boolean ignoreCache)
            throws GadgetException {
        // TODO Auto-generated method stub

        GadgetContext context = new UriGadgetContext(uri);
        return getGadgetSpec(context, ignoreCache);
    }
}
