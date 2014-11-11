# Misc Scripts to manage Nuxeo Elasticsearch index


## Dump existing Nuxeo documents into Elasticsearch

### Requirements

- A Nuxeo intance with the nuxeo-rest-api addon (>5.9.3)

- GNU parallel

        sudo apt-get install parallel

- ElasticSearch >= 1.1.1

        wget --no-check-certificate https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.1.1.deb && sudo dpkg -i elasticsearch-1.1.1.deb


### Dump the Nuxeo content


    NXUSER=Administrator NXPASSWORD=secret NXURL=http://localhost:8080/nuxeo ./nx_dump.sh

Here is the output

    ### Dump Nuxeo documents ... Fri Feb 17 17:38:18 CET 2014
    ### Page 0/?
	### Found total pages: 987
	### Page 1/987
	....
	### Page 987/987
    ### Total number of docs
    87039
    ### Creating archive...
    ### Done:  Mon Feb 17 18:19:44 CET 2014
    /tmp/dump-nuxeo-doc.tgz


### Initialize the Elasticsearch index and template

    ESHOST=localhost ESPORT=9200 ./init-index.sh

Output

    ### Going to RESET the elasticsearch index localhost:9200/nuxeo, are you sure? [y/N] y
    ### Delete index...
    ### Creating index nuxeo ...
    ### Creating mapping doc ...
    ### Done


### Import content into elasticsearch

Import the documents dump into elasticsearch:

    ESHOST=localhost ESPORT=9200 ./es_import.sh /tmp/dump-nuxeo-doc.tgz

Output


    ### Number of doc before import
    {"count":0,"_shards":{"total":5,"successful":5,"failed":0}}
    ### Total doc to import:
    87039
    ### Import ...
    real    1m19.391s
    user    0m1.684s
    sys     0m2.480s
    ### Number of doc after import
    + curl -XGET localhost:9200/nuxeo/doc/_count
    {"count":85950,"_shards":{"total":5,"successful":5,"failed":0}}+ set +x


## Change the Elasticsearch settings or mapping of an existing instance


If you want to change the settings or the mapping you need to reindex
all the Elasticsearch documents, here is the fast way to achieve this
without extracting the data from Nuxeo:

### Requirements

- Java 7

- [stream2es](https://github.com/elasticsearch/stream2es/)

        curl -O download.elasticsearch.org/stream2es/stream2es; chmod +x stream2es


### Procedure

You need an http access to the Elasticsearch, for an embedded instance
you need to set the `elasticsearch.httpEnabled=true` option in the
`nuxeo.conf` file.

1. Extract the current configuration of your index (settings and
  mapping)

        ./es_get_conf.sh localhost:9200/nuxeo

  Output:

        ### Dump settings
        ### Dump mapping
        ### Merging conf
        ### Done
        /tmp/es-conf.json

  This small script does nothing more to merge the settings and
  mapping configuration into a single file.

2. Modify the `/tmp/es-conf.json` file.

3. Create a new index with the new configuration:

        curl -XPUT localhost:9200/nuxeo-new -d @/tmp/es-conf.json

  Output

        {"acknowledged":true}

4. Copy the documents to the new index:


        stream2es es --source http://localhost:9200/nuxeo --target http://localhost:9200/nuxeo-new

  Output

        stream es from http://localhost:9200/nuxeo to http://localhost:9200/nuxeo-new
        00:00,993 396,8d/s 1042,0K/s 394 394 1059563 0 5f959f21-5e02-4346-acae-a56614224058
        00:01,276 848,0d/s 1628,8K/s 1082 688 1068667 0 24decc6a-2fb7-4c9d-a64b-4fc1f88c707f
        00:01,497 1181,0d/s 2085,1K/s 1768 686 1068139 0 cb972a9d-cf6a-45e7-be27-c79c7c5630f0
        00:01,708 1436,2d/s 2438,4K/s 2453 685 1068381 0 abbec188-9808-4134-861e-78bab63c4e78
        00:01,931 1626,1d/s 2697,2K/s 3140 687 1068564 0 d3e98084-66cf-442d-a708-4b5e39d86f9b
        flushing index queue
        00:02,180 1755,5d/s 2867,6K/s 3827 687 1068080 0 9fec1fbe-7c4b-4a84-b6d7-a774e54916bf
        00:02,219 1872,9d/s 3042,2K/s 4156 329 511349 0 62c56520-2d26-4778-8292-f91a099f65f8
        streamed 4156 indexed 4156 bytes xfer 6912743 errors 0


5. Update the `elasticsearch.indexName` value in the `nuxeo.conf` and
   restart Nuxeo. You can delete your old index if you are happy with
   the new one.


Note that you can also do this without any downtime using an
[alias](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/indices-aliases.html). For
instance if you have an alias `nuxeo` that point to `nuxeo1`, you do
step 1 to 4 creating a new index `nuxeo2`, then you update the alias
`nuxeo` to point to `nuxeo2`.

## Case insensitive search (ILIKE)

If you want to do case insensitive search or use an ILIKE operation,
you need to change the mapping.

Follow the previous procedure and edit `es-conf.json` file to add an
analyzer in the `analyzers` section:

            "lowercase" : {
              "type" : "custom",
              "filter" : [ "lowercase", "asciifolding" ],
              "tokenizer" : "keyword"
            },

Then add the analyzer to your field in the `mappings` section for
instance:

          "dc:source" : {
            "type" : "string",
            "analyzer" : "lowercase"
          },


Then continue the procedure step 3 and 4.

Now `SELECT * FROM Document WHERE dc:source LIKE 'FoO%'` will match
`foobar` or `FoObAz`.

