# Form Validator

A Toolkit to maintain and validates form fields in android application.

# Installation

The Form Validator can be installed directly into your application by importing sdk artifacts via Gradle.

Add the following code to your project's **build.gradle** file:

```groovy
allprojects {
    repositories {
        maven {
            url "https://maven.pkg.github.com/sapkotamadhusudan/form-validator"
        }
    }
}
```

And the following code to your **module's** `build.gradle` file:

```groovy
dependencies {
    implementation "com.maddy:form-validator:0.0.1"
}
```
