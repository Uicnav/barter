package com.barter.server

import com.barter.server.db.DatabaseFactory
import com.barter.server.routes.barterRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Patch)
            allowMethod(HttpMethod.Delete)
        }

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respondText(
                    text = "Internal error: ${cause.localizedMessage}",
                    status = HttpStatusCode.InternalServerError,
                )
            }
        }

        DatabaseFactory.init()

        routing {
            barterRoutes()

            get("/") {
                call.respondText("Barter API is running")
            }
        }
    }.start(wait = true)
}
