call javac -source 1.3 -target 1.4.2 -classpath ".;..;C:\apache-ant-1.7.1\lib\ant.jar;..\lib\jftp.jar" src\SignTask.java
move src\SignTask.class .
call java -classpath ".;..;C:\apache-ant-1.7.1\lib\ant.jar;..\lib\jftp.jar" SignTask
jar cvf SignTask.jar *.class && move SignTask.jar ..\lib
del/q SignTask.class