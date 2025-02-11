# Gradle Test Synchronization Plugin

A gradle plugin that provides a way to synchronize tests between different gradle modules.

## Motivation

Consider following project structure:

```
root
  build.gradle
  settings.gradle
  +-- module1
    build.gradle
  +-- module2
    build.gradle
```

Both modules have API tests that call some external service.
You want to build / tests these modules in parallel (with gradle property `org.gradle.parallel=true`),
but external API has a rate limit that causes tests to fail when run in parallel.
You can't simply synchronize tests by using some static state or JUnit built-in synchronization tools (@ResourceLock, @Isolated, etc), because tests are run in different JVMs.
One possible solution is to move all tests to new single module, but this is not always desirable or possible.

This plugin aims to solve exactly this problem.

## Usage

### Synchronizing based on single tag

In `build.gradle`:
```groovy
plugins {
    id("com.varlanv.testsync-gradle-plugin").version("0.0.1")
}

testSync {
    tag("postgres")
}
```

In junit test:
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

class PostgresTest {

    @Test
    @Tag("postgres")
    void test() {
        // ...
    }
}

```
In this example, all tests tagged **"postgres"** will be synchronized.

### Synchronizing based on multiple tags

In `build.gradle`:
```groovy
plugins {
    id("com.varlanv.testsync-gradle-plugin").version("0.0.1")    
}

testSync {
    tags("postgres", "mongo")
}
```

In junit tests:
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;

class PostgresTest {

    @Test
    @Tag("postgres")
    void test() {
        // ...
    }
}

class MongoTest {

    @Test
    @Tag("mongo")
    void test() {
        // ...
    }
}

class MongoWithPostgresTest {

    @Test
    @Tags({@Tag("postgres"), @Tag("mongo")})
    void test() {
        // ...
    }
}
```
In this example:
- All tests tagged **"postgres"** or **"mongo"** will have independent locks, allowing them to run independently and in parallel.
- Tests that are tagged with both **"postgres"** AND **"mongo"** will (wait and) acquire both locks. When the locks are acquired, no other
**"postgres"** or **"mongo"** will be allowed to start until locks are released.

## Known limitations

- Currently, only JUnit 5 is supported
- Synchronization is possible only by using Junit tags (`org.junit.jupiter.api.Tag`).
- The plugin was tested with latest Gradle 8x, 7x, 6x versions (8.10.2, 7.6.1, 6.9.4). Any other version is not
  guaranteed to work.

If you have any issues or feature requests, please don't hesitate to create an issue.

## License

This project is distributed under the [MIT License](LICENSE).
