response = req.getResponse()
msg="Your document has been created successfully."
response.sendRedirect("${req.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
