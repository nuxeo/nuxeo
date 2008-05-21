import org.nuxeo.ecm.platform.comment.api.CommentableDocument as CommentableDocument
cDoc = Document.getAdapter(CommentableDocument);

cAuthor = Request.getParameter('author')
cText = Request.getParameter('text')
//TODO: filter the comment text to remove harmful html


if (cText != null) {
    cDoc.addComment(cText)
    msg="Comment submitted."
    Response.sendRedirect("${Context.targetObject.urlPath}?msg=${msg}")
} else {
    msg="No comment posted."
    Response.sendRedirect("${Context.targetObject.urlPath}?msg=${msg}")
}
