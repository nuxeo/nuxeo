package org.nuxeo.ecm.core.api.operation;

import org.nuxeo.ecm.core.api.DocumentRef;

public class UnlockOperation extends  Operation<String> {

    public UnlockOperation(DocumentRef ref) {
        super("__UNLOCK__");
        this.ref = ref;
    }

    private static final long serialVersionUID = 1L;
    protected final DocumentRef ref;
    
    @Override
    public String doRun(ProgressMonitor montior) throws Exception {
       session.unlock(ref);
       addModification(new Modification(ref,Modification.STATE));
       return ref + " is unlocked";
    }

}
