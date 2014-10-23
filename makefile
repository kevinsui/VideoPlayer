JCC = javac

default: VideoPlayer.class

VideoPlayer.class: VideoPlayer.java
	$(JCC) $(JFLAGS) VideoPlayer.java

run: VideoPlayer.class
	java VideoPlayer video1.rgb 1.0 1.0 30 0 0

clean:
	$(RM) *.class