Package to deploy relation service related core event listeners:

  - PublishRelationsListener synchronous core event listener for
    documentPublished events to forward relations to the proxy, and keep the
    relations or comments from the previous proxy to the new version

TODO:

  - include an additional asynchronous core event listener to clean the
    relations on deleted documents
