MAIN_SOURCES := $(shell find src/main/java -name '*.java')
TEST_SOURCES := $(shell find src/test/java -name '*.java')

.PHONY: build run test load clean

build:
	@mkdir -p build/main
	javac --release 21 -d build/main $(MAIN_SOURCES)

run: build
	java -cp build/main io.quorumforge.flashpoint.Flashpoint

test: build
	@mkdir -p build/test
	javac --release 21 -cp build/main -d build/test $(TEST_SOURCES)
	java -ea -cp build/main:build/test io.quorumforge.flashpoint.FlashpointTest

load: test
	java -cp build/main:build/test io.quorumforge.flashpoint.LoadGenerator 10000 100

clean:
	rm -rf build
