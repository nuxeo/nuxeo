import org.nuxeo.ecm.platform.comment.api.CommentableDocument as CommentableDocument
cDoc = Document.getAdapter(CommentableDocument, true);

cText = Request.getParameter('text')
//TODO: filter the comment text to remove harmful html

if (Session.principal.isAnonymous()) {
    cAuthor = Request.getParameter('author')
} else {
    cAuthor = Session.principal
}

Context.print("doc ${Document.sessionId}\n")
Context.print("session ${Session.sessionId}\n")
Context.print("principal ${Session.principal.name}\n")

if (cText != null) {
    cDoc.addComment(cText)
    msg="Comment added by ${cAuthor}."
    Response.sendRedirect("${Context.targetObject.urlPath}?msg=${msg}")
} else {
    msg="No comment posted."
    Response.sendRedirect("${Context.targetObject.urlPath}?msg=${msg}")
}
