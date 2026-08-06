plugins {
    java
}

group = "io.github.ezraio.indexsmith.examples"
version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.ezraio:indexsmith-bom:0.1.0"))
    implementation("io.github.ezraio:indexsmith-api")
    implementation("io.github.ezraio:indexsmith-client-es8")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
