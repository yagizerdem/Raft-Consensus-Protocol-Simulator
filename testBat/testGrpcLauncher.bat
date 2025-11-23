javac -sourcepath ./src -d ./out/production/raft ./src/Main.java

start cmd /k java -cp ./out/production/raft Main 9000 9001 9002
start cmd /k java -cp ./out/production/raft Main 9001 9000 9002
start cmd /k java -cp ./out/production/raft Main 9002 9000 9001

pause