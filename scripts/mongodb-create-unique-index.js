// (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Contributors:
//     Florent Guillaume
// v1.1

// This script attempts to create a unique index on ecm:id.
// It will fail if the database has duplicates.

var DBNAME = "nuxeo"
var COLLNAME = "default"

db = db.getSiblingDB(DBNAME)
coll = db.getCollection(COLLNAME)
print()
print("Using " + DBNAME + "." + COLLNAME)

// check index presence
var dropIndex = false
coll.getIndexes().forEach(function(idx) {
  if (idx.key["ecm:id"]) {
    if (idx.unique) {
      print("Unique index on ecm:id already present")
      quit(0)
    } else {
      dropIndex = true
    }
  }
})

// check if there are duplicates
// we don't want to remove the existing index if there are duplicates,
// because it will be useful to de-duplicate
print("Starting scan for duplicate ids...")
var agg = coll.aggregate([
  {"$group": {"_id": "$ecm:id", "count": {"$sum": 1}}},
  {"$match": {"count": {"$gt": 1}}}],
  {allowDiskUse: true})
if (agg.hasNext()) {
  print("Collection has duplicates, the first one is ecm:id = " + agg.next()["_id"])
  print("Unique index not created")
  quit(1)
}

// delete old index
var res
if (dropIndex) {
  print("Dropping previous index on ecm:id...")
  res = coll.dropIndex({"ecm:id": 1})
  if (res.ok) {
    print("Done")
  } else {
    print("Failed to drop previous index on ecm:id")
    printjson(res)
    quit(2)
  }
}

// attempt creation of unique index
print("Creating unique index on ecm:id...")
res = coll.createIndex({"ecm:id": 1}, {unique: true})
if (res.ok) {
  print("Done")
} else {
  print("Failed to create unique index on ecm:id")
  printjson(res)
  quit(3)
}
