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



