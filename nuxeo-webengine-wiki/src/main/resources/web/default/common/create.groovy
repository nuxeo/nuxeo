
msg="Your document has been created successfully."
Response.sendRedirect("${req.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
