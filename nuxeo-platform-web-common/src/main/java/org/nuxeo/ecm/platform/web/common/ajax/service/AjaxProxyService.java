package org.nuxeo.ecm.platform.web.common.ajax.service;

/**
 *
 * Service interface to know what urls are proxyable
 *
 * @author tiry
 *
 */
public interface AjaxProxyService {

    ProxyURLConfigEntry getConfigForURL(String targetUrl);
}
