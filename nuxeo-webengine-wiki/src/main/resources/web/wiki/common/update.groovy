msg = "The document has been updated successfully."
Response.sendRedirect("${Context.lastResolvedObject.urlPath}?msg=${msg}")
