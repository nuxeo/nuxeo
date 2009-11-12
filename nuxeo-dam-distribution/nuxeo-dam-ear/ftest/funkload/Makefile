#CREDCTL := fl-credential-ctl credential.conf
CREDCTL := /bin/true
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


all: start test-import stop

bench: start bench-import stop


start:
	-mkdir -p $(REPORT) $(LOG_HOME)
	-$(MONCTL) restart
	$(CREDCTL) restart
	fl-run-test test_Dam.py -v --simple-fetch -e test_available $(FLOPS)

stop:
	-$(MONCTL) stop
	-$(CREDCTL) stop

test-import:
	fl-run-test -dv --simple-fetch test_Dam.py -e test_import $(FLOPS)


bench-import-load:
	-fl-run-bench --label=load --simple-fetch -c 1:5:10:15:20:25:30 -D 60 -m 0.1 -M 1 -s 1 test_Dam.py Dam.test_import $(FLOPS)
	-fl-build-report $(LOG_HOME)/dam-bench.xml --html -o $(REPORT)

bench-import-long:
	-fl-run-bench --label=long --simple-fetch -c 1:5:5:5:5:5:1 -D 600 -m 0.1 -M 1 -s 1 test_Dam.py Dam.test_import $(FLOPS)
	-fl-build-report $(LOG_HOME)/dam-bench.xml --html -o $(REPORT)

clean:
	-find . "(" -name "*~" -or  -name ".#*" -or  -name "*.pyc" ")" -print0 | xargs -0 rm -f
