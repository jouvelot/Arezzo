compile: 	
	javac -cp ../../mail.jar:../../jsdt.jar *.java 

bundle:
	(cd ../..; jar cfv bundle.jar ./Bundle/*.properties)

applet:
	(cd ../..; jar cfv applet.jar cnpmusic/arezzo/*.class)

server:
	(cd ../..; jar cfv server.jar cnpmusic/arezzo/*.class)

tar:
	(cd ../../../..; tar cfv arezzo.tar --exclude '*~' Arezzo)
