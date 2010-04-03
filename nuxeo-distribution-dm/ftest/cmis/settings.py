#
# Override these settings with values to match your environment.
#
# CMIS repository's service URL
#REPOSITORY_URL = 'http://cmis.alfresco.com/s/cmis'
#REPOSITORY_URL = 'http://localhost:8080/cmis/repository' # Apache Chemistry
REPOSITORY_URL = 'http://cmis.dnsdojo.com:8080/p8cmis/resources/DaphneA/Service'
#REPOSITORY_URL = 'http://localhost:8080/alfresco/s/cmis'
#REPOSITORY_URL = 'http://cmis.demo.nuxeo.org/nuxeo/site/cmis/repository'
#REPOSITORY_URL = 'http://localhost:8080/opencmis/atom' # OpenCMIS from the OpenText guys
#REPOSITORY_URL = 'http://ec2-174-129-218-67.compute-1.amazonaws.com/cmis/atom' #OpenText on Amazon
# CMIS repository credentials
USERNAME = 'admin'
PASSWORD = 'admin'
#USERNAME = 'Administrator' # Nuxeo
#PASSWORD = 'Administrator' # Nuxeo
#USERNAME = 'cmisuser'
#PASSWORD = 'otcmis'
# Absolute path to a directory where test folders can be created, including
# the trailing slash.
#TEST_ROOT_PATH = '/default-domain/jeff test/' # REMEMBER TRAILING SLASH
TEST_ROOT_PATH = '/cmislib test/' # REMEMBER TRAILING SLASH
#TEST_ROOT_PATH = '/'
# Binary test files. Assumed to exist in the same dir as this python script
TEST_BINARY_1 = '250px-Cmis_logo.png'
TEST_BINARY_2 = 'sample-a.pdf'
# For repositories that may index test content asynchronously, the number of
# times a query is retried before giving up.
MAX_FULL_TEXT_TRIES = 10
# The number of seconds the test should sleep between tries.
FULL_TEXT_WAIT = 10

# Nuxeo
#REPOSITORY_URL = 'http://cmis.demo.nuxeo.org/nuxeo/site/cmis/repository'
REPOSITORY_URL = 'http://localhost:8080/nuxeo/site/cmis/repository'
USERNAME = 'Administrator'
PASSWORD = 'Administrator'
TEST_ROOT_PATH = '/default-domain/'

# Chemistry
#REPOSITORY_URL = 'http://localhost:8082/cmis/repository'
#USERNAME = ''
#PASSWORD = ''
#TEST_ROOT_PATH = ''

