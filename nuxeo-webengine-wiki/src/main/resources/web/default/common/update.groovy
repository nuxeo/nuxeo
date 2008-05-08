response = req.getResponse()
msg="The document has been updated successfully."
response.sendRedirect("${req.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
