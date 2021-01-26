PLANTUML=plantuml

UMLS := $(shell ls *.txt)
PNGS := $(UMLS:.txt=.png)

all: $(PNGS)

%.png: %.txt
	$(PLANTUML) $<
