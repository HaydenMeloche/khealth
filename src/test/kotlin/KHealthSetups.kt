import KHealthTest.Companion.helloRoute
import dev.hayden.KHealth
import io.ktor.application.Application
import io.ktor.application.install

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

// THE CONFIG BELOW CAUSE PATH ERRORS

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
