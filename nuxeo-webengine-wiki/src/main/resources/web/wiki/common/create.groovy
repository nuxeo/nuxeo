msg = "Your document has been created successfully."
Response.sendRedirect("${Context.lastResolvedObject.urlPath}?msg=${msg}")
