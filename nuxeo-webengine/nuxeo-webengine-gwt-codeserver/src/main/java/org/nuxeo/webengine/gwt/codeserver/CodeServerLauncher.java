package org.nuxeo.webengine.gwt.codeserver;

public interface CodeServerLauncher {

    void startup(String[] args) throws Exception;

    void shutdown() throws Exception;

}