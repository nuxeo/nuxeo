CREDCTL := fl-credential-ctl credential.conf
MONCTL := fl-monitor-ctl monitor.conf
LOG_HOME := ../../target/ftest/funkload/log

ifdef URL
	FLOPS = -u $(URL) $(EXT)
else
	FLOPS = $(EXT)
endif

ifdef REPORT_HOME
	REPORT = $(REPORT_HOME)
else
	REPORT = ../../target/ftest/funkload/report
endif


# all test except bench
all: start rest page test-nuxeo webengine stop

start:
	-mkdir -p $(REPORT) $(LOG_HOME)
	-$(MONCTL) restart
	$(CREDCTL) restart
	fl-run-test test_pages.py --simple-fetch -v -e testAvailable $(FLOPS)


stop:
	-$(MONCTL) stop
	-$(CREDCTL) stop

# test rest and pages libs
rest:
	fl-run-test -v --simple-fetch test_rest.py -e testWriter $(FLOPS)
	fl-run-test -v --simple-fetch test_rest.py -e testReader $(FLOPS)

page:
	fl-run-test -v --simple-fetch test_pages.py $(FLOPS)

webengine:
	fl-run-test -v --simple-fetch test_webengine.py $(FLOPS)


# test nuxeo ep
init:
	fl-run-test -v --simple-fetch test_nuxeo.py -e testInit $(FLOPS)

writer:
	fl-run-test -v --simple-fetch test_nuxeo.py -e testWriter $(FLOPS)

reader:
	fl-run-test -v --simple-fetch test_nuxeo.py -e testReader $(FLOPS)

test-nuxeo: init writer reader

test-dashboard:
	fl-run-test -dv --simple-fetch test_nuxeo.py -e testDashboard $(FLOPS)	

navigation-init:
	fl-run-test -v --simple-fetch test_navigation.py -e testBrowse $(FLOPS)

# require init for users access
navigation:
	fl-run-test -v --simple-fetch test_navigation.py -e testNavigation $(FLOPS)

# bench nuxeo ep
bench: start test-nuxeo bench-writer bench-reader stop

bench-long: start test-nuxeo bench-writer bench-writer-long bench-reader stop

bench-writer:
	-fl-run-bench --simple-fetch -c 1:4:8:10:12:14 -D 60 -m 0.2 -M 1 -s 1 test_nuxeo.py Nuxeo.testWriter $(FLOPS)
	-fl-build-report $(LOG_HOME)/nuxeo-bench.xml --html -o $(REPORT)

bench-rest-writer:
	-fl-run-bench --simple-fetch -c 1:10 -D 30 -f test_rest.py Rest.testWriter $(FLOPS)
	-fl-build-report $(LOG_HOME)/rest-bench.xml --html -o $(REPORT)

warmup:
	fl-run-test --simple-fetch -v -m 0 -M 0 test_nuxeo.py -e testReader $(FLOPS)
	-fl-run-bench --simple-fetch -c 5 -D 1 -m 0 -M 0 -s 0 test_nuxeo.py Nuxeo.testReader $(FLOPS)

bench-reader: warmup
	-fl-run-bench --simple-fetch -c 1:5:10:15:20:25:30:35:40:45:50 -D 60 -m 0.1 -M 1 -s 1 test_nuxeo.py Nuxeo.testReader $(FLOPS)
	-fl-build-report $(LOG_HOME)/nuxeo-bench.xml --html -o $(REPORT)


# bench nuxeo ep
bench-longevity: start test-nuxeo bench-writer-long bench-reader-long stop


bench-writer-long:
	-fl-run-bench --simple-fetch --label=long -c 10:10:10:10:10 -D 600 -m 0.1 -M 1 -s 0.5 test_nuxeo.py Nuxeo.testWriter $(FLOPS)
	-fl-build-report $(LOG_HOME)/nuxeo-bench.xml --html -o $(REPORT)

bench-reader-long: warmup
	-fl-run-bench --simple-fetch -c 1:20:20:20:20:1 -D 600 -m 0.1 -M 1 -s 1 test_nuxeo.py Nuxeo.testReader $(FLOPS)
	-fl-build-report $(LOG_HOME)/nuxeo-bench.xml --html -o $(REPORT)


bench-navigation: navigation-init
	-fl-run-bench --simple-fetch -c 1:5:10:15:20:25:30:35:40:45:50 -D 60 -m 0.1 -M 1 -s 1 test_navigation.py Navigation.testNavigation $(FLOPS)
	-fl-build-report $(LOG_HOME)/navigation-bench.xml --html -o $(REPORT)


clean:
	-find . "(" -name "*~" -or  -name ".#*" -or  -name "*.pyc" ")" -print0 | xargs -0 rm -f
