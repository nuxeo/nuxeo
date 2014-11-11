msg = "Your document has been created successfully."
Response.sendRedirect("${Context.targetObject.urlPath}?msg=${msg}")
