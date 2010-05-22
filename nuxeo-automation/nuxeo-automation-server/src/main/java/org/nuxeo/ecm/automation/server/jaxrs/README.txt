
{
  input   : null | /path/to/doc[?path_to_blob] | UID[?path_to_blob]
  context : { key: value, ... }
  params  :  { key: value, ... }
}

params types are resolved at runtime (they are specified as strings)
see type adapters: date, resource, document, properties

context -> string -> string pairs.

input can have one of the types:
- document, documents, blob, blobs (add query too)?


result:
{
  uid:
  path:
  title:

  properties: {
    "dc:title": "test"

    "dc:title": { "test", "string" }
  }
}

exception:
{

}


