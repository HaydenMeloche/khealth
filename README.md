# KHealth
[![Release](https://jitpack.io/v/haydenmeloche/khealth.svg)](https://jitpack.io/#dev.hayden/khealth)

Khealth is a simple & customizable health plugin for Ktor.

> As of KHealth version `2.0.0`, Ktor 2 is only supported
## Usage

```kotlin
import dev.hayden.KHealth

fun main(args: Array<String>) {
  embeddedServer(Netty, 80) {
    install(KHealth)
  }.start(wait = true)
}
```

This will configure a `/ready` and a `/health` endpoint both returning a `200` status code.

KHealth also supports adding custom checks to both the ready and health endpoints.

```kotlin
import dev.hayden.KHealth

fun main(args: Array<String>) {
  embeddedServer(Netty, 80) {
    install(KHealth) {
      readyChecks {
        check("check my database is up") {
          myDatabase.ping()
        }
      }
      healthChecks {
        check("another check") { true }
      }
    }
  }.start(wait = true)
}
```

A `GET /ready` call would return

```json
{
  "check my database is up": true
}
```

and a `200` status code.

If any provided checks return `false` then a 500 would be returned.

If you'd like to override the default status codes, that can be done by overriding the default values.
```kotlin
import dev.hayden.KHealth

fun main(args: Array<String>) {
  embeddedServer(Netty, 80) {
    install(KHealth) { 
      successfulCheckStatusCode = HttpStatusCode.Accepted
      unsuccessfulCheckStatusCode = HttpStatusCode.ExpectationFailed
    }
  }.start(wait = true)
}
```
The health endpoint and ready endpoint can both be disabled using the `healthCheckEnabled` and
`readyCheckEnabled` properties.

```kotlin
import dev.hayden.KHealth

fun main(args: Array<String>) {
  embeddedServer(Netty, 80) {
    install(KHealth) {
      readyCheckEnabled = false
      healthCheckEnabled = false
    }
  }.start(wait = true)
}
```

If you need to override the default URI paths, that can be done too.

```kotlin
import dev.hayden.KHealth

fun main(args: Array<String>) {
  embeddedServer(Netty, 80) {
    install(KHealth) {
      readyCheckPath = "newready"
      healthCheckPath = "/newhealth"
    }
  }.start(wait = true)
}
```

For more advanced configurations, KHealth accepts a Ktor `Route` allowing you to wrap the routes
created by KHealth. An example of this is shown below adding basic authentication to the KHealth
endpoints.

```kotlin
authentication {
  basic(name = "basic auth") {
    validate { credentials ->
      // your logic here
    }
  }
}
install(KHealth) {
  wrap {
    // wrap our KHealth endpoints with an authentication block
    authenticate("basic auth", optional = false, build = it)
  }
}
```

## Installation
KHealth uses Jitpack as its repository. This means you will need to add a custom repository for
Jitpack if you don't already have it.

For Maven:
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```
```xml
<dependency>
  <groupId>dev.hayden</groupId>
  <artifactId>khealth</artifactId>
  <version>2.1.1</version>
</dependency>
```

For gradle:
```groovy
allprojects {
  repositories {
    maven { url 'https://jitpack.io' }
  }
}
```
```groovy
dependencies {
  implementation 'dev.hayden:khealth:2.1.1'
}
```

For additional build systems check out: https://jitpack.io/#dev.hayden/khealth
