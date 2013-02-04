libkhordr: khordr_KeyGrabber.c khordr_KeyGrabber.h
	gcc khordr_KeyGrabber.c \
	-std=c99 \
	-I/Developer/SDKs/MacOSX10.5.sdk/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Headers/ \
	-D_JNI_IMPLEMENTATION_ \
	-dynamiclib \
	-gstabs+ \
	-framework ApplicationServices \
	-o libkhordr.jnilib

khordr_KeyGrabber.h: khordr/KeyGrabber.java
	javac khordr/KeyGrabber.java
	javah khordr.KeyGrabber

deploy: libkhordr
	cp libkhordr.jnilib ../../ext
