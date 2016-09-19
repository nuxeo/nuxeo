package org.nuxeo.ecm.restapi.server.jaxrs.targetplatforms;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformFilterImpl;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;
import org.nuxeo.targetplatforms.io.JSONExporter;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class TargetPlatformsRestlet extends BaseNuxeoRestlet {
    private static final String PUBLIC_TP_CACHE_KEY = "PUBLIC_TP";

    private static final LoadingCache<String, String> PUBLIC_CACHE = CacheBuilder //
            .newBuilder() //
            .expireAfterAccess(5, TimeUnit.MINUTES) //
            .refreshAfterWrite(10, TimeUnit.MINUTES) //
            .recordStats() //
            .maximumSize(5).build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) throws Exception {
                    return key;
                }
            });

    @Override
    public void handle(Request request, Response response) {
        HttpServletResponse res = getHttpResponse(response);
        if (res == null || res.isCommitted()) {
            return;
        }

        try {
            String targetPlatforms = getTargetPlatforms();
            response.setEntity(targetPlatforms, MediaType.APPLICATION_JSON);
        } catch (ExecutionException e) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    private String getTargetPlatforms() throws ExecutionException {
        return PUBLIC_CACHE.get(PUBLIC_TP_CACHE_KEY, new TargetPlatformCallable());
    }

    private static class TargetPlatformCallable implements Callable<String> {
        @Override
        public String call() throws Exception {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                TargetPlatformService tps = Framework.getService(TargetPlatformService.class);
                List<TargetPlatform> res = tps.getAvailableTargetPlatforms(
                        new TargetPlatformFilterImpl(false, true, true, false, null));
                if (res == null) {
                    res = new ArrayList<>();
                }
                JSONExporter.exportToJson(res, baos, false);
                return new String(baos.toByteArray());
            }
        }
    }
}
