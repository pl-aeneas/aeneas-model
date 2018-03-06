JRI=/home/acanino/Src/rJava/jri/src/

all: model

model: src/model/Simulator.java
	javac -d build/ -cp lib/commons-cli-1.4/commons-cli-1.4.jar:$(JRI)/JRI.jar:./lib/stoke.jar src/model/Simulator.java src/model/Roll.java

clean:
	rm -rf build/*
	
