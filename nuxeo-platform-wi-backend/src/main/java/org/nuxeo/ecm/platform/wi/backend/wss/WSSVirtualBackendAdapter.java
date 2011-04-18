package org.nuxeo.ecm.platform.wi.backend.wss;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.wi.backend.Backend;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.DWSMetaDataImpl;
import org.nuxeo.wss.spi.dws.Site;
import org.nuxeo.wss.spi.dws.SiteImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class WSSVirtualBackendAdapter extends WSSBackendAdapter {

    private String urlRoot;

    public WSSVirtualBackendAdapter(Backend backend, String virtualRoot) {
        super(backend, virtualRoot);
        this.urlRoot = virtualRoot + backend.getRootUrl();
    }

    @Override
    public WSSListItem getItem(String location) throws WSSException {
        throw new WSSException("Operation not supported");
    }

    @Override
    public List<WSSListItem> listItems(String location) throws WSSException {
        try {
            List<WSSListItem> result = new ArrayList<WSSListItem>();
            List<String> folders = backend.getVirtualFolderNames();
            for(String folder : folders){
                WSSListItem item = new VirtualListItem(folder, corePathPrefix, urlRoot);
                result.add(item);
            }
            return result;
        } catch (ClientException e) {
            throw new WSSException("Error while getting children" + location, e);
        }
    }

    @Override
    public void begin() throws WSSException {
        //nothing
    }

    @Override
    public void saveChanges() throws WSSException {
        super.saveChanges();
    }

    @Override
    public void discardChanges() throws WSSException {
        super.discardChanges();
    }

    @Override
    public WSSListItem moveItem(String location, String destination) throws WSSException {
        throw new WSSException("Operation not supported");
    }

    @Override
    public void removeItem(String location) throws WSSException {
        throw new WSSException("Operation not supported");
    }

    @Override
    public WSSListItem createFolder(String parentPath, String name) throws WSSException {
        throw new WSSException("Operation not supported");
    }

    @Override
    public WSSListItem createFileItem(String parentPath, String name) throws WSSException {
        throw new WSSException("Operation not supported");
    }

    @Override
    public DWSMetaData getMetaData(String location, WSSRequest wssRequest) throws WSSException {
        return new DWSMetaDataImpl();
    }

    @Override
    public Site getSite(String location) throws WSSException {
        return new SiteImpl(urlRoot);
    }

}
