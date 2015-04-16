package org.nuxeo.webengine.gwt.codeserver;


import com.google.gwt.dev.codeserver.CodeServer;
import com.google.gwt.dev.codeserver.Options;
import com.google.gwt.dev.codeserver.WebServer;

public class CodeServerWrapper implements CodeServerLauncher {

    WebServer server;

    @Override
    public void startup(String[] args) throws Exception {
        Options options = new Options();

        if (!options.parseArgs(args)) {
            throw new RuntimeException("Cannot parse gwt code server options");
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(CodeServerWrapper.class.getClassLoader());
        try {
            server = CodeServer.start(options);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (server == null) {
            return;
        }
        try {
            server.stop();
        } finally {
            server = null;
        }
    }

}
