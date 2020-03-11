nuxeo-elasticsearch
===================

## About

This project aims at providing Nuxeo bundles to integrate
Elasticsearch with the Nuxeo Platform.

The idea is to index/query Nuxeo Documents to/from Elasticsearch.

## Building

To build and run the tests, simply start the Maven build:

    mvn clean install

## Running tests

### Using the embedded Elasticsearch

With the default `RestClient`:
```bash
mvn -nsu test
# -Dnuxeo.test.elasticsearch.client=RestClient is set by default
```

Or with the `TransportClient`

```bash
mvn -nsu test -Dnuxeo.test.elasticsearch.client=TransportClient
```

### Using an external Elasticsearch

1. Start an Elasticsearch using docker:
```bash
docker run -p 9400:9200 -p 9600:9300 docker.elastic.co/elasticsearch/elasticsearch-oss:6.5.3
```

2.a Run the test with `RestClient`:
```bash
mvn -nsu test -Dnuxeo.test.elasticsearch.addressList=http://localhost:9400
```

2.b Or run the test with `TransportClient`:
```bash
mvn -nsu test -Dnuxeo.test.elasticsearch.client=TransportClient -Dnuxeo.test.elasticsearch.clusterName=docker-cluster -Dnuxeo.test.elasticsearch.addressList=localhost:9600
```

## Links

- Administration: http://doc.nuxeo.com/x/UBY5AQ
- Configuring mapping: http://doc.nuxeo.com/x/WxI5AQ
- Overview: http://doc.nuxeo.com/x/iYElAQ
- [Open jira tikets](https://jira.nuxeo.com/issues/?jql=project%20%3D%20NXP%20AND%20component%20%3D%20Elasticsearch%20AND%20Status%20!%3D%20%22Resolved%22%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC%2C%20created%20ASC)
