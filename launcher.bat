javac -sourcepath ./src -d ./out/production/raft ./src/Main.java

java -cp ./out/production/raft Main 9000
java -cp ./out/production/raft Main 9001

pause