msg = "The document has been updated successfully."
Response.sendRedirect("${Context.targetObject.urlPath}?msg=${msg}")
