package org.nuxeo.ecm.platform.wi.backend.wss;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.wi.backend.Backend;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.Site;

import java.util.List;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class WSSRootBackendAdapter extends WSSBackendAdapter {

    public WSSRootBackendAdapter(Backend backend, String virtualRoot) {
        super(backend, virtualRoot);
        this.urlRoot = virtualRoot + backend.getRootUrl();
    }

    @Override
    public boolean exists(String location) {
        return getBackend(location).exists(location);
    }

    @Override
    public WSSListItem getItem(String location) throws WSSException {
        return getBackend(location).getItem(location);
    }

    @Override
    public List<WSSListItem> listItems(String location) throws WSSException {
        WSSBackend backend = getBackend(location);
        return backend.listItems(location);
    }

    @Override
    public void begin() throws WSSException {
        //backend.begin();
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
        WSSBackend sourceBackend = getBackend(location);
        DocumentModel source = null;
        if(sourceBackend instanceof WSSBackendAdapter){
            source = ((WSSBackendAdapter)sourceBackend).getDocument(location);
        }
        if(source == null){
            throw new WSSException("Can't move document. Source did not found.");
        }
        WSSBackend destinationBackend = getBackend(destination);
        if(destinationBackend instanceof WSSBackendAdapter){
            return ((WSSBackendAdapter)destinationBackend).moveItem(source, destination);
        } else {
            return sourceBackend.moveItem(location, destination);
        }
    }

    @Override
    public void removeItem(String location) throws WSSException {
        getBackend(location).removeItem(location);
    }

    @Override
    public WSSListItem createFolder(String parentPath, String name) throws WSSException {
        return getBackend(parentPath).createFolder(parentPath, name);
    }

    @Override
    public WSSListItem createFileItem(String parentPath, String name) throws WSSException {
        return getBackend(parentPath).createFileItem(parentPath, name);
    }

    @Override
    public DWSMetaData getMetaData(String location, WSSRequest wssRequest) throws WSSException {
        return getBackend(location).getMetaData(location, wssRequest);
    }

    @Override
    public Site getSite(String location) throws WSSException {
        return getBackend(location).getSite(location);
    }

    protected WSSBackend getBackend(String location){
        if(StringUtils.isEmpty(location)){
            return new WSSFakeBackend();
        }

        Backend backend = this.backend.getBackend(cleanLocation(location));
        if(backend == null){
            return new WSSFakeBackend();
        }
        if(backend.isVirtual()){
            return new WSSVirtualBackendAdapter(backend, virtualRoot);
        }
        return new WSSBackendAdapter(backend, virtualRoot);
    }
}
