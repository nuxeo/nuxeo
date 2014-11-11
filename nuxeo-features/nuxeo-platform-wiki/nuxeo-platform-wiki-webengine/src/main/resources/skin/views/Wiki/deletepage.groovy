/*Remove a document given a UUID*/

import org.nuxeo.ecm.core.api.IdRef

uuid = Request.getParameter('uuid')

if (uuid) {
    ref = new IdRef(uuid)
} else {
    Response.sendError(403, 'You need to provide a valid docId')
}

if (ref && Session.canRemoveDocument(ref)) {
    try {
        Session.removeDocument(ref)
        Session.save()
        Response.writer.write("${uuid} removed")
        Response.status = 200
    }
    catch(Exception e) {
        Response.sendError(403, 'Cannot remove document')
    }
} else {
    Response.sendError(403, 'Cannot remove document')
}
