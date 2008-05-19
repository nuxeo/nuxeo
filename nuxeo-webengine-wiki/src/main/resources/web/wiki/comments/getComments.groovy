import org.nuxeo.ecm.platform.comment.api.CommentableDocument
cDoc = Document.getAdapter(org.nuxeo.ecm.platform.comment.api.CommentableDocument);

Context.setProperty('comments', cDoc.comments)

Context.render("comments/show_comments.ftl")