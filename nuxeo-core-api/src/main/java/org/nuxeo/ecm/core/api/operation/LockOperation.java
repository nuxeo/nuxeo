package org.nuxeo.ecm.core.api.operation;

import org.nuxeo.ecm.core.api.DocumentRef;

public class LockOperation extends  Operation<String> {

    public LockOperation(DocumentRef ref, String key) {
        super("__LOCK__");
        this.ref = ref;
        this.key = key;
    }

    private static final long serialVersionUID = 1L;
    protected final DocumentRef ref;
    protected final String key;
    
    @Override
    public String doRun(ProgressMonitor montior) throws Exception {
       session.setLock(ref, key);
       return key;
    }

}
