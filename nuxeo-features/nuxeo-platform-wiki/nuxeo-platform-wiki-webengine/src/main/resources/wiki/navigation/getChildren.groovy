package wiki.navigation;

import org.nuxeo.ecm.core.api.IdRef
import org.nuxeo.ecm.core.api.DocumentModel
import net.sf.json.JSONArray

rid = Request.getParameter('root')

def getChildren() {
    
    children = []
    
    if (rid != null && rid != 'source') {
        docid = new IdRef(rid)
    } else {
        docid = new IdRef(Document.id)
    }
    
    for (doc in Session.getChildren(docid)) {
        children.add(contructDocArray(doc))
    }
    
    return children
}

def contructDocArray(DocumentModel doc) {
    child = [:]
    child['text'] = doc.title
    child['expanded'] = false
    child['hasChildren'] = Session.hasChildren(new IdRef(doc.id))
    child['id'] = doc.id
    child['href'] = Context.getUrlPath(doc)
    return child
}

doclist = getChildren()

jsob = JSONArray.fromObject(doclist)

Response.writer.write(jsob.toString())

