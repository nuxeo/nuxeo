package org.nuxeo.ecm.platform.convert.ooolauncher;

import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;

public interface OOoConnectionManager {

    boolean canGetConnection();

    SocketOpenOfficeConnection getConnection();

    void releaseConnection(SocketOpenOfficeConnection connection);

}
