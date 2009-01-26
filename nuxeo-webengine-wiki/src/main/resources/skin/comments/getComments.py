import org.nuxeo.ecm.platform.comment.api.CommentableDocument as CommentableDocument
cDoc = Document.getAdapter(CommentableDocument);

comments = cDoc.getComments()
Context.render("WikiPage/show_comments.ftl", {'comments': comments})


#cDoc.addComment("a nice comment for the doc %s" % Document.getPropertyValue('dc:title'))
