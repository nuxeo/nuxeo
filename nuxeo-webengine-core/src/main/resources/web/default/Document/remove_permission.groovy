import org.nuxeo.ecm.webengine.util.ACLUtils;


permission = Request.getParameter("permission")
username = Request.getParameter("user")


ACLUtils.removePermission(Context.coreSession, Document.ref, username, permission);
Context.coreSession.save()

Context.redirect(Context.targetObjectUrlPath)
