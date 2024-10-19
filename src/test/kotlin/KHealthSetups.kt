import KHealthTest.Companion.helloRoute
import dev.hayden.KHealth
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic

/**
 * A basic configuration of [KHealth] with nothing but defaults
 */
fun Application.defaultKHealth() {
    install(KHealth)
    helloRoute()
}

/**
 * A configuration of [KHealth] where both check endpoints are overridden
 */
fun Application.overridePathUris() {
    install(KHealth) {
        readyCheckPath = "newready"
        healthCheckPath = "/newhealth"
    }
    helloRoute()
}

/**
 * A configuration of [KHealth] where both check endpoints are disabled
 */
fun Application.disabledChecks() {
    install(KHealth) {
        readyCheckEnabled = false
        healthCheckEnabled = false
    }
    helloRoute()
}

/**
 * A configuration of [KHealth] with custom checks
 */
fun Application.customChecks() {
    install(KHealth) {
        readyChecks {
            check("a sample check") { true }
        }
        healthChecks {
            check("another check") { true }
        }
    }
    helloRoute()
}

/**
 * A configuration of [KHealth] with custom checks that contain a failure
 */
fun Application.customChecksWithFailure() {
    install(KHealth) {
        readyChecks {
            check("a sample check") { true }
            check("a sample failing check") { false }
        }
        healthChecks {
            check("another check") { true }
            check("another failing check") { false }
        }
    }
    helloRoute()
}

/**
 * A configuration of [KHealth] utilizing 'wrap']
 */
fun Application.customRouteWrapper() {
    // build an example authentication setup for basic auth
    authentication {
        basic(name = "basic auth") {
            realm = "Ktor Server"
            validate { credentials ->
                if (credentials.name == credentials.password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
    install(KHealth) {
        wrap {
            // wrap our KHealth endpoints with an authentication block
            authenticate("basic auth", optional = false, build = it)
        }
    }
    helloRoute()
}

/**
 * A configuration of [KHealth] with custom status codes set.
 */
fun Application.customStatusCodes() {
    install(KHealth) {
        successfulCheckStatusCode = HttpStatusCode.Accepted
        unsuccessfulCheckStatusCode = HttpStatusCode.ExpectationFailed
        readyChecks {
            check("failing check") { false }
        }
    }
    helloRoute()
}

// THE CONFIG BELOW CAUSES PATH ERRORS

/**
 * A [KHealth] configuration with an invalid healthcheck uri path
 */
fun Application.incorrectHealthUri() {
    install(KHealth) {
        healthCheckPath = "/"
    }
    helloRoute()
}

/**
 * A [KHealth] configuration with an invalid ready uri path
 */
fun Application.incorrectReadyCheckUri() {
    install(KHealth) {
        readyCheckPath = "/"
    }
    helloRoute()
}
