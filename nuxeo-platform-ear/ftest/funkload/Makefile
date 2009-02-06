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

clean:
	-find . "(" -name "*~" -or  -name ".#*" -or  -name "*.pyc" ")" -print0 | xargs -0 rm -f
