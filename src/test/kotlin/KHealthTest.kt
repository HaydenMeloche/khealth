import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class KHealthTest {

    @Test
    fun `assert the basic configuration exposes the ready and health endpoints`() {
        withTestApplication(Application::defaultKHealth) {
            handleRequest(HttpMethod.Get, readyEndpoint).apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            handleRequest(HttpMethod.Get, healthEndpoint).apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            baseAssertion()
        }
    }

    @Test
    fun `assert error is thrown when the health check path is invalid`() {
        Assert.assertThrows("The provided path must not be empty", IllegalArgumentException::class.java) {
            withTestApplication(Application::incorrectHealthUri) {
            }
        }
    }

    @Test
    fun `assert error is thrown when the ready check path is invalid`() {
        Assert.assertThrows("The provided path must not be empty", IllegalArgumentException::class.java) {
            withTestApplication(Application::incorrectHealthUri) {
            }
        }
    }

    @Test
    internal fun `assert overriding both default uris correctly sets`() {
        withTestApplication(Application::overridePathUris) {
            handleRequest(HttpMethod.Get, "/newready").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            handleRequest(HttpMethod.Get, "/newhealth").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            baseAssertion()
        }
    }

    @Test
    internal fun `assert no check endpoints are available if both are disabled`() {
        withTestApplication(Application::disabledChecks) {
            handleRequest(HttpMethod.Get, readyEndpoint).apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, healthEndpoint).apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            baseAssertion()
        }
    }

    @Test
    internal fun `assert custom checks are returned as a json response`() {
        withTestApplication(Application::customChecks) {
            handleRequest(HttpMethod.Get, readyEndpoint).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val responseAsMap: Map<String, Boolean> = Json.decodeFromString(response.content!!)
                assertEquals(1, responseAsMap.size)
                assertEquals(true, responseAsMap["a sample check"])
            }
            handleRequest(HttpMethod.Get, healthEndpoint).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val responseAsMap: Map<String, Boolean> = Json.decodeFromString(response.content!!)
                assertEquals(1, responseAsMap.size)
                assertEquals(true, responseAsMap["another check"])
            }
            baseAssertion()
        }
    }

    @Test
    internal fun `assert custom checks are returned with 500 if any fail`() {
        withTestApplication(Application::customChecksWithFailure) {
            handleRequest(HttpMethod.Get, readyEndpoint).apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
                val responseAsMap: Map<String, Boolean> = Json.decodeFromString(response.content!!)
                assertEquals(2, responseAsMap.size)
                assertEquals(true, responseAsMap["a sample check"])
                assertEquals(false, responseAsMap["a sample failing check"])
            }
            handleRequest(HttpMethod.Get, healthEndpoint).apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
                val responseAsMap: Map<String, Boolean> = Json.decodeFromString(response.content!!)
                assertEquals(2, responseAsMap.size)
                assertEquals(true, responseAsMap["another check"])
                assertEquals(false, responseAsMap["another failing check"])
            }
            baseAssertion()
        }
    }

    companion object {
        private const val readyEndpoint = "/ready"
        private const val healthEndpoint = "/health"
        private const val expectedResponse = "Hello"
        private const val defaultPath = "/hi"
        fun Application.helloRoute() {
            routing {
                get(defaultPath) { call.respondText(expectedResponse) }
            }
        }

        /**
         * Base assertion to assert no configuration breaks a simple GET endpoint
         */
        private fun TestApplicationEngine.baseAssertion() {
            handleRequest(HttpMethod.Get, defaultPath).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(expectedResponse, response.content)
            }
        }
    }
}
