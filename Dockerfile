FROM amazoncorretto:21-alpine

WORKDIR /usr/app

COPY build/libs/Raft-Consensus-Protocol-Simulator.jar Raft-Consensus-Protocol-Simulator.jar

CMD ["java", "-jar", "Raft-Consensus-Protocol-Simulator.jar"]