// This is also used as the Maven artifact ID.
// <https://maven.apache.org/pom.html#maven-coordinates>
rootProject.name = "idea-ansible-plugin"


// Add source dependencies.
// https://docs.gradle.org/current/javadoc/org/gradle/vcs/SourceControl.html
sourceControl {
    gitRepository(uri("https://github.com/mdklatt/idea-common.git")) {
        producesModule("dev.mdklatt:idea-common")  // <groupId>:<artifactId>
    }
}
