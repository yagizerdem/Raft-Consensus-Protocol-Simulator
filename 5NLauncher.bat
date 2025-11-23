javac -sourcepath ./src -d ./out/production/raft ./src/Main.java
javac -sourcepath ./src -d ./out/production/raft ./src/ClientShell.java

start cmd /k java -cp ./out/production/raft Main 9000 9001 9002 9003 9004 9000 8000
start cmd /k java -cp ./out/production/raft Main 9001 9000 9002 9003 9004 9000 8000
start cmd /k java -cp ./out/production/raft Main 9002 9001 9000 9003 9004 9000 8000
start cmd /k java -cp ./out/production/raft Main 9003 9001 9002 9000 9004 9000 8000
start cmd /k java -cp ./out/production/raft Main 9004 9001 9002 9003 9000 9000 8000

start cmd /k java -cp ./out/production/raft ClientShell 8000 -peers=9000,9001,9002,9003,9004
