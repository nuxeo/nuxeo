package org.nuxeo.ecm.core.api.repository.cache;

import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Date;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.operation.ModificationSet;

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

    public static void putInThreadContext(Object tag) {
        contextHolder.set(new Context((Long) tag));
    }

    public static void clearThreadContext() {
        contextHolder.remove();
    }

    public static void check(DocumentModel doc) {
        Context ctx = contextHolder.get();
        if (ctx == null) {
            return; // invoked on server, no cache
        }
        long modified;
        try {
            Property modifiedProp = doc.getProperty("dc:modified");
            if (modifiedProp == null) {
                return;
            }
            Date modifiedDate = modifiedProp.getValue(Date.class);
            if (modifiedDate == null) {
                return;
            }
            modified = modifiedDate.getTime();
        } catch (Exception e) {
            throw new ClientRuntimeException("cannot fetch dc modified for doc " + doc, e);
        }
        if (ctx.modified >= modified) {
            return; // client cache is freshest than doc
        }
        if (ctx.invoked <= modified) {
            return; // modified by self user
        }
        String message = String.format("%s is outdated : cache %s - op start %s - doc %s", doc.getId(), new Date(ctx.modified), new Date(ctx.invoked), new Date(modified));
        throw new ConcurrentModificationException(message);
    }

    public static Object earliestTag(Object tag1, Object tag2) {
        return Long.class.cast(tag1) > Long.class.cast(tag2) ? tag1 : tag2;
    }

    public static Object computeTag(String sessionId, ModificationSet modifs) {
        // TODO compute a more precise time stamp than current date
        return Calendar.getInstance().getTimeInMillis();
    }

}
