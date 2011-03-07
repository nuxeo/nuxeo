package org.nuxeo.ecm.platform.wi.backend.wss;

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
public class WSSFakeBackend implements WSSBackend {

    @Override
    public WSSListItem getItem(String s) throws WSSException {
        return null;
    }

    @Override
    public List<WSSListItem> listItems(String s) throws WSSException {
        return null;
    }

    @Override
    public List<WSSListItem> listFolderishItems(String s) throws WSSException {
        return null;
    }

    @Override
    public List<WSSListItem> listLeafItems(String s) throws WSSException {
        return null;
    }

    @Override
    public void begin() throws WSSException {

    }

    @Override
    public void saveChanges() throws WSSException {

    }

    @Override
    public void discardChanges() throws WSSException {

    }

    @Override
    public WSSListItem moveItem(String s, String s1) throws WSSException {
        return null;
    }

    @Override
    public void removeItem(String s) throws WSSException {

    }

    @Override
    public boolean exists(String s) {
        return false;
    }

    @Override
    public WSSListItem createFolder(String s, String s1) throws WSSException {
        return null;
    }

    @Override
    public WSSListItem createFileItem(String s, String s1) throws WSSException {
        return null;
    }

    @Override
    public DWSMetaData getMetaData(String s, WSSRequest wssRequest) throws WSSException {
        return null;
    }

    @Override
    public Site getSite(String s) throws WSSException {
        return null;
    }
}
