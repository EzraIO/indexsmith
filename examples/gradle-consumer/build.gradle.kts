plugins {
    java
}

group = "io.github.outakeshi.examples"
version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.outakeshi:indexsmith-bom:0.1.0"))
    implementation("io.github.outakeshi:indexsmith-api")
    implementation("io.github.outakeshi:indexsmith-client-es8")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
