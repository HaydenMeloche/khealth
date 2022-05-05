package dev.hayden

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class Check(val checkName: String, val check: CheckFunction)
typealias CheckFunction = suspend () -> Boolean

val KHealth = createApplicationPlugin(
    name = "KHealth",
    createConfiguration = ::KHealthConfiguration,
    body = {
        onCall { call ->
            KHealthPlugin(this.pluginConfig).apply { interceptor(call) }
        }
    }
)

class KHealthPlugin internal constructor(private val config: KHealthConfiguration) {

    /**
     * Interceptor that handles all http requests. If either the health check or ready endpoint are
     * called it will return a custom response with the result of each custom check if any are defined.
     */
    fun interceptor(call: ApplicationCall) {
        val routing: Routing.() -> Unit = {
            val routing: Route.() -> Unit = {
                if (config.readyCheckEnabled) route(config.readyCheckPath) {
                    get {
                        val (status, responseBody) = processChecks(
                            checkLinkedList = config.readyChecks,
                            passingStatusCode = config.successfulCheckStatusCode,
                            failingStatusCode = config.unsuccessfulCheckStatusCode
                        )
                        call.respondText(responseBody, ContentType.Application.Json, status)
                    }
                }
                if (config.healthCheckEnabled) route(config.healthCheckPath) {
                    get {
                        val (status, responseBody) = processChecks(
                            checkLinkedList = config.healthChecks,
                            passingStatusCode = config.successfulCheckStatusCode,
                            failingStatusCode = config.unsuccessfulCheckStatusCode
                        )
                        call.respondText(responseBody, ContentType.Application.Json, status)
                    }
                }
            }
            config.wrapWith?.invoke(this, routing) ?: routing(this)
        }
        call.application.pluginOrNull(Routing)?.apply(routing) ?: call.application.install(Routing, routing)
    }

    /**
     * Process the checks for a specific endpoint returning the evaluation of each check as a JSON string
     * with a status code. If any of the checks are false, then a [HttpStatusCode.InternalServerError] will be returned,
     * otherwise a [HttpStatusCode.OK] will be returned.
     * @param checkLinkedList A linkedlist of [Check] to be run.
     * @return A pair including a [HttpStatusCode] and a JSON encoded string of each check with their result.
     */
    private suspend fun processChecks(
        checkLinkedList: LinkedHashSet<Check>,
        passingStatusCode: HttpStatusCode,
        failingStatusCode: HttpStatusCode
    ): Pair<HttpStatusCode, String> {
        val checksWithResults = checkLinkedList.associate { Pair(it.checkName, it.check.invoke()) }
        val status = if (checksWithResults.containsValue(false)) {
            failingStatusCode
        } else passingStatusCode
        return Pair(status, Json.encodeToString(checksWithResults))
    }
}

/**
 * Configuration class used to configure [KHealthPlugin]. No values are required to be passed in as defaults
 * are provided.
 */
class KHealthConfiguration internal constructor() {
    internal var healthChecks = linkedSetOf<Check>()
    internal var readyChecks = linkedSetOf<Check>()
    internal var wrapWith: (Route.(next: Route.() -> Unit) -> Unit)? = null

    /**
     * The status code returned when a check fails. Defaults to 500 - Internal Server Error.
     */
    var unsuccessfulCheckStatusCode = HttpStatusCode.InternalServerError

    /**
     * The status code returned when all checks pass. Defaults to 200 - OK
     */
    var successfulCheckStatusCode = HttpStatusCode.OK

    /**
     * The path of the health check endpoint. Defaults to "/health".
     */
    var healthCheckPath = "/health"
        set(value) {
            field = normalizePath(value)
        }

    /**
     * The path of the ready check endpoint. Defaults to "/ready".
     */
    var readyCheckPath = "/ready"
        set(value) {
            field = normalizePath(value)
        }

    /**
     * Controls whether the health check endpoint is enabled or disabled. Defaults to true.
     */
    var healthCheckEnabled = true

    /**
     * Controls whether the ready check endpoint is enabled or disabled. Defaults to true.
     */
    var readyCheckEnabled = true

    /**
     * Builder function to add checks to the health endpoint.
     */
    fun healthChecks(init: CheckBuilder.() -> Unit) {
        healthChecks = CheckBuilder().apply(init).checks
    }

    /**
     * Builder function to add checks to the ready endpoint.
     */
    fun readyChecks(init: CheckBuilder.() -> Unit) {
        readyChecks = CheckBuilder().apply(init).checks
    }

    fun wrap(block: Route.(next: Route.() -> Unit) -> Unit) {
        wrapWith = block
    }
}

/**
 * A builder class used to create descriptive DSL for adding checks to an endpoint.
 * @see healthChecks
 * @see readyChecks
 */
class CheckBuilder {
    val checks = linkedSetOf<Check>()

    /**
     * A custom check that will be run on every call the customized endpoint.
     * If any check [CheckFunction] returns false, a 500 will be returned.
     * @param name The name of the check in the GET response
     * @param check A boolean returning function that supplies the result of the check
     */
    fun check(name: String, check: CheckFunction) {
        checks.add(Check(name, check))
    }
}

/**
 * Normalizes the provided URI and asserts that it's not blank.
 * The provided URI can have a slash, if it does not then one will be added to normalize it.
 * @param uri The provided URI
 */
private fun normalizePath(uri: String): String {
    return if (uri[0] == '/') {
        assertNotBlank(uri.removePrefix("/"))
        uri
    } else {
        assertNotBlank(uri)
        "/$uri"
    }
}

/**
 * Asserts the provided path value is not blank.
 */
private fun assertNotBlank(value: String) {
    require(value.isNotBlank()) {
        "The provided path must not be empty"
    }
}
