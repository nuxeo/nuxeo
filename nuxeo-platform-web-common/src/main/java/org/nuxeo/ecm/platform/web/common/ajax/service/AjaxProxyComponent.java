package org.nuxeo.ecm.platform.web.common.ajax.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.LRUCachingMap;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 * Simple Runtime component to manage proxyable url configuration via Extension Points
 *
 * @author tiry
 *
 */
public class AjaxProxyComponent extends DefaultComponent implements AjaxProxyService {

    public static final String PROXY_URL_EP = "proxyableURL";

    private static final Log log = LogFactory.getLog(AjaxProxyComponent.class);

    protected static final Map<String, ProxyableURLDescriptor> urlDescriptors = new HashMap<String, ProxyableURLDescriptor>();

    protected static final Map<String, ProxyURLConfigEntry> urlCache = new LRUCachingMap<String, ProxyURLConfigEntry>(
            250);

    protected static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PROXY_URL_EP.equals(extensionPoint)) {
            ProxyableURLDescriptor desc = (ProxyableURLDescriptor) contribution;
            registerProxyURL(desc);
        } else {
            log.error("Unknow ExtensionPoint " + extensionPoint);
        }
    }

    protected void registerProxyURL(ProxyableURLDescriptor desc) {

        if (urlDescriptors.containsKey(desc.getName())) {
            urlDescriptors.get(desc.getName()).merge(desc);
        } else {
            urlDescriptors.put(desc.getName(), desc);
        }
    }

    public ProxyURLConfigEntry getConfigForURL(String targetUrl) {

        ProxyURLConfigEntry entry = null;

        try {
            cacheLock.readLock().lock();
            entry = urlCache.get(targetUrl);
        } finally {
            cacheLock.readLock().unlock();
        }
        if (entry == null) {
            entry = computeConfigForURL(targetUrl);
            try {
                cacheLock.writeLock().lock();
                urlCache.put(targetUrl, entry);
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        return entry;
    }

    public ProxyURLConfigEntry computeConfigForURL(String targetUrl) {

        for (ProxyableURLDescriptor desc : urlDescriptors.values()) {

            if (desc.isEnabled()) {
                Pattern pat = desc.getCompiledPattern();
                Matcher m = pat.matcher(targetUrl);
                if (m.matches()) {
                    return new ProxyURLConfigEntry(true, desc);
                }
            }
        }
        // return deny by default
        return new ProxyURLConfigEntry();
    }

}
