<html>
  <head>
    <title>Nuxeo Operations</title>
  </head>
  <body>
    <h3>Execute an operation chain</h3>
    <form method="POST" action="">
      <p>
        Enter the path or the UID of the target document (this will become the chain input).
        <br />
        <input type="text" size="80" name="input"
          value="/default-domain/workspaces" />
      </p>
      <p>
        Fill the textarea using the XML contribution defining the operation chain.
        <br />
        <textarea name="chain" rows="20" cols="80"></textarea>
        <input type="submit" value="Run" />
      </p>
    </form>
    <hr />
    <h3>Export operation (JSON format)</h3>
    <ul>
      <li>
        <a href="${This.path}/doc">Export all operations</a>
      </li>
      <li>
        <a href="${This.path}/studioDoc">Export operations exposed in Studio</a>
      </li>
    </ul>
  </body>
</html>
