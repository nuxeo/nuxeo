CREDCTL := fl-credential-ctl credential.conf
MONCTL := fl-monitor-ctl monitor.conf

ifdef URL
	FLOPS = -u $(URL) $(EXT)
else
	FLOPS = $(EXT)
endif

all: start rest page stop

start:
	-$(MONCTL) restart
	$(CREDCTL) restart
	fl-run-test test_pages.py -v --simple-fetch -e testAvailable $(FLOPS)

stop:
	-$(MONCTL) stop
	-$(CREDCTL) stop

rest:
	fl-run-test -v --simple-fetch test_rest.py -e testWriter $(FLOPS)
	fl-run-test -v --simple-fetch test_rest.py -e testReader $(FLOPS)

page:
	fl-run-test -v --simple-fetch test_pages.py $(FLOPS)

nux:
	fl-run-test -dv --simple-fetch test_nuxeo.py -e testInit $(FLOPS)
	fl-run-test -dv --simple-fetch test_nuxeo.py -e testWriter $(FLOPS)
	fl-run-test -dv --simple-fetch test_nuxeo.py -e testReader $(FLOPS)


bench: nuxeo bench-writer warmum bench-reader


bench-writer:
	-fl-run-bench -c 1:5:10:15:20 -D 60 -m 0.7 -M 1 -s 1 test_nuxeo.py Nuxeo.testWriter $(FLOPS)
	-fl-build-report log/nuxeo-bench.xml --html -o target/report
	-./finalize-report.sh test_writer

warmup:
	fl-run-test -v -m 0 -M 0 test_nuxeo.py -e testReader $(FLOPS)
	-fl-run-bench -c 5 -D 1 -m 0 -M 0 -s 0 test_nuxeo.py Nuxeo.testReader $(FLOPS)

bench-reader:
	-fl-run-bench --simple-fetch -c 1:5:10:15:20:25:30 -D 60 -m 0.1 -M 1 -s 1 test_nuxeo.py Nuxeo.testReader $(FLOPS)
	-fl-build-report log/nx5-bench.xml --html -o target/report
	-./finalize-report.sh test_reader


clean:
	-find . "(" -name "*~" -or  -name ".#*" -or  -name "*.pyc" ")" -print0 | xargs -0 rm -f
