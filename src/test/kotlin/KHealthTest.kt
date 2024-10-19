import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class KHealthTest {

    @Test
    fun `assert the basic configuration exposes the ready and health endpoints`() = testApplication {
        application {
            defaultKHealth()
        }
        client.get(readyEndpoint).apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        client.get(healthEndpoint).apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        baseAssertion()
    }

    @Test
    fun `assert error is thrown when the health check path is invalid`() {
        assertThrows<IllegalArgumentException>("The provided path must not be empty") {
            testApplication {
                application {
                    incorrectHealthUri()
                }
            }
        }
    }

    @Test
    fun `assert error is thrown when the ready check path is invalid`() {
        assertThrows<IllegalArgumentException>("The provided path must not be empty") {
            testApplication {
                application {
                    incorrectReadyCheckUri()
                }
            }
        }
    }

    @Test
    fun `assert overriding both default uris correctly sets`() = testApplication {
        application {
            overridePathUris()
        }
        client.get("/newready").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        client.get("/newhealth").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        baseAssertion()
    }

    @Test
    fun `assert no check endpoints are available if both are disabled`() = testApplication {
        application {
            disabledChecks()
        }
        client.get(readyEndpoint).apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
        client.get(healthEndpoint).apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
        baseAssertion()
    }

    @Test
    fun `assert custom checks are returned as a json response`() = testApplication {
        application {
            customChecks()
        }
        client.get(readyEndpoint).apply {
            assertEquals(HttpStatusCode.OK, status)
            val responseAsMap: Map<String, Boolean> = Json.decodeFromString(bodyAsText())
            assertEquals(1, responseAsMap.size)
            assertEquals(true, responseAsMap["a sample check"])
        }
        client.get(healthEndpoint).apply {
            assertEquals(HttpStatusCode.OK, status)
            val responseAsMap: Map<String, Boolean> = Json.decodeFromString(bodyAsText())
            assertEquals(1, responseAsMap.size)
            assertEquals(true, responseAsMap["another check"])
        }
        baseAssertion()
    }

    @Test
    fun `assert custom checks are returned with 500 if any fail`() = testApplication {
        application {
            customChecksWithFailure()
        }
        client.get(readyEndpoint).apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            val responseAsMap: Map<String, Boolean> = Json.decodeFromString(bodyAsText())
            assertEquals(2, responseAsMap.size)
            assertEquals(true, responseAsMap["a sample check"])
            assertEquals(false, responseAsMap["a sample failing check"])
        }
        client.get(healthEndpoint).apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            val responseAsMap: Map<String, Boolean> = Json.decodeFromString(bodyAsText())
            assertEquals(2, responseAsMap.size)
            assertEquals(true, responseAsMap["another check"])
            assertEquals(false, responseAsMap["another failing check"])
        }
        baseAssertion()
    }

    @Test
    fun `assert provided route wrapper is used`() = testApplication {
        application {
            customRouteWrapper()
        }
        client.get(readyEndpoint) {
            header("Authorization", "Basic dXNlcm5hbWU6dXNlcm5hbWU=")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            baseAssertion()
        }
    }

    @Test
    fun `assert custom status codes are used when provided`() = testApplication {
        application {
            customStatusCodes()
        }
        client.get(healthEndpoint).apply {
            assertEquals(HttpStatusCode.Accepted, status)
        }
        client.get(readyEndpoint).apply {
            assertEquals(HttpStatusCode.ExpectationFailed, status)
        }
        baseAssertion()
    }

    companion object {
        private const val readyEndpoint = "/ready"
        private const val healthEndpoint = "/health"
        private const val expectedResponse = "Hello"
        private const val defaultPath = "/hi"

        fun Application.helloRoute() {
            routing {
                get(defaultPath) {
                    call.respondText(expectedResponse)
                }
            }
        }

        /**
         * Base assertion to assert no configuration breaks a simple GET endpoint
         */
        private suspend fun ApplicationTestBuilder.baseAssertion() {
            client.get(defaultPath).apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(expectedResponse, bodyAsText())
            }
        }
    }
}
