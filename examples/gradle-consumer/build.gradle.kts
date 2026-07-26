plugins {
    java
}

group = "io.github.indexsmith.examples"
version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.indexsmith:indexsmith-bom:0.1.0"))
    implementation("io.github.indexsmith:indexsmith-api")
    implementation("io.github.indexsmith:indexsmith-client-es8")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
