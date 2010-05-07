package org.nuxeo.ecm.core.api.repository.cache;

import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Date;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;

public class DirtyUpdateChecker {

    protected static ThreadLocal<Context> contextHolder = new ThreadLocal<Context>();

    protected static class Context {
        final Long modified;

        final Long invoked;
        Context(Long modified) {
            this.modified = modified;
            this.invoked = Calendar.getInstance().getTimeInMillis();
        }
    }

    public static void piggyBack(Object tag) {
        contextHolder.set(new Context((Long) tag));
    }

    public static void piggyBack() {
        contextHolder.remove();
    }

    public static void check(DocumentModel doc) {
        Context ctx = contextHolder.get();
        if (ctx == null) {
            return; // invoked on server, no cache
        }
        long modified;
        try {
            modified = doc.getProperty("dc:modified").getValue(Date.class).getTime();
        } catch (Exception e) {
            throw new ClientRuntimeException("cannot fetch dc modified for doc " + doc, e);
        }
        if (ctx.modified >= modified) {
            return; // client cache is freshest than doc
        }
        if (ctx.invoked <= modified) {
            return;
        }
        String message = String.format("%s is outdated : cache %s - op start %s - doc %s", doc.getId(), new Date(ctx.modified), new Date(ctx.invoked), new Date(modified));
        throw new ConcurrentModificationException(message);
    }


}
