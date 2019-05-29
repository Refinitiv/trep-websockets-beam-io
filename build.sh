mvn clean
mvn install -DskipTests

cd Dataflow-Pipeline
mvn assembly:assembly -DskipTests
