package com.anwrt.ooserver.daemon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.convert.oooserver.OOoDaemonService;

public class Log4JLogger extends Logger {

    private static final Log log = LogFactory.getLog(OOoDaemonService.class);

    public static boolean logInfoAsDebug = true;

    @Override
    protected void debugImpl(String msg) {
        log.debug(msg);
    }

    @Override
    protected void debugImpl(String msg, Exception ex) {
        log.debug(msg,ex);

    }

    @Override
    protected void debugImpl(Exception ex) {
        log.debug(ex);
    }

    @Override
    protected void detailedDebugImpl(Exception ex) {
        log.debug(ex);
    }

    @Override
    protected void errorImpl(String msg) {
        log.error(msg);
    }

    @Override
    protected void fatalErrorImpl(String msg) {
        log.error(msg);
    }

    @Override
    protected void fatalErrorImpl(String msg, Exception ex) {
        log.error(msg,ex);
    }

    @Override
    protected void infoImpl(String msg) {
        if (logInfoAsDebug) {
            log.debug(msg);
        }
        else {
            log.info(msg);
        }
    }

    @Override
    protected void warningImpl(String msg) {
        log.warn(msg);
    }

}
