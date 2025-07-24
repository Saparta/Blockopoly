package com.gameservice

import com.gameservice.models.Card
import io.ktor.server.application.Application
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.h4
import kotlinx.html.h5
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.title
import kotlinx.html.ul

fun Application.configureGameGui() {
    routing {
        get("/game/{roomId}") {
            val roomId = call.parameters["roomId"] ?: return@get
            val game = ServerManager.getRoom(roomId)

            if (game == null) {
                call.respondHtml {
                    head {
                        title { +"Game Not Found" }
                        link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/tailwindcss/dist/tailwind.min.css")
                    }
                    body(classes = "bg-gray-100 p-4") {
                        h1(classes = "text-2xl font-bold mb-4") { +"Game Not Found" }
                        p { +"The game with ID '$roomId' does not exist or has not started yet." }
                    }
                }
                return@get
            }

            if (!game.state.isCompleted) {
                call.respondHtml {
                    head {
                        title { +"Game Not Ready" }
                        link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/tailwindcss/dist/tailwind.min.css")
                    }
                    body(classes = "bg-gray-100 p-4") {
                        h1(classes = "text-2xl font-bold mb-4") { +"Game Not Ready" }
                        p { +"The game with ID '$roomId' is not yet ready. Please ensure players have connected." }
                    }
                }
                return@get
            }

            val gameState = game.state.await().value

            call.respondHtml {
                head {
                    title { +"Deal Game State - $roomId" }
                    link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/tailwindcss/dist/tailwind.min.css")
                }
                body(classes = "bg-gray-100 p-4") {
                    h1(classes = "text-3xl font-bold mb-6 text-center") { +"Deal Game State: $roomId" }

                    div(classes = "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6") {
                        // General Game Info
                        div(classes = "bg-white p-6 rounded-lg shadow-md") {
                            h2(classes = "text-xl font-semibold mb-4") { +"Game Overview" }
                            p { +"Player at Turn: " ; span(classes = "font-medium") { +(gameState.playerAtTurn ?: "N/A") } }
                            p { +"Winning Player: " ; span(classes = "font-medium") { +(gameState.winningPlayer ?: "None") } }
                            p { +"Cards Left to Play: " ; span(classes = "font-medium") { +"${gameState.cardsLeftToPlay}" } }
                            p { +"Draw Pile Size: " ; span(classes = "font-medium") { +"${gameState.drawPile.size}" } }
                            p { +"Discard Pile Size: " ; span(classes = "font-medium") { +"${gameState.discardPile.size}" } }
                            p { +"Turn Started: " ; span(classes = "font-medium") { +"${gameState.turnStarted}" } }
                            div(classes = "mt-4") {
                                h3(classes = "text-lg font-medium mb-2") { +"Player Order:" }
                                ul(classes = "list-disc list-inside") {
                                    gameState.playerOrder.forEach { player ->
                                        li { +player }
                                    }
                                }
                            }
                        }

                        // Pending Interactions
                        div(classes = "bg-white p-6 rounded-lg shadow-md") {
                            h2(classes = "text-xl font-semibold mb-4") { +"Pending Interactions" }
                            if (gameState.pendingInteractions.pendingInteractions.isEmpty()) {
                                p { +"No pending interactions." }
                            } else {
                                ul(classes = "list-disc list-inside") {
                                    gameState.pendingInteractions.pendingInteractions.forEach { interaction ->
                                        li {
                                            +"From: ${interaction.fromPlayer}, To: ${interaction.toPlayer}, Awaiting Response From: ${interaction.awaitingResponseFrom}"
                                            p(classes = "ml-4 text-sm text-gray-600") { +"Action: ${interaction.action.javaClass.simpleName}, Initial Cards: ${interaction.initial.size}, Offense: ${interaction.offense.size}, Defense: ${interaction.defense.size}, Resolved: ${interaction.resolved}" }
                                        }
                                    }
                                }
                            }
                        }

                        // Player States
                        div(classes = "bg-white p-6 rounded-lg shadow-md col-span-full") {
                            h2(classes = "text-xl font-semibold mb-4") { +"Player States" }
                            div(classes = "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4") {
                                gameState.playerState.forEach { (playerId, playerState) ->
                                    div(classes = "border p-4 rounded-lg bg-gray-50") {
                                        h3(classes = "text-lg font-medium mb-2") { +"Player: $playerId" }
                                        div(classes = "mt-2") {
                                            h4(classes = "font-medium") { +"Hand:" }
                                            if (playerState.hand.isEmpty()) {
                                                p(classes = "text-sm text-gray-600") { +"Empty" }
                                            } else {
                                                ul(classes = "list-disc list-inside text-sm text-gray-700") {
                                                    playerState.hand.forEach { card ->
                                                        when (card) {
                                                            is Card.Rent -> li { +"${card.actionType} (ID: ${card.id}, Value: ${card.value}, Colors: ${card.colors})"}
                                                            is Card.Action -> li { +"${card.actionType} (ID: ${card.id}, Value: ${card.value})"}
                                                            is Card.Money -> li { +"${card.cardType} (ID: ${card.id}, Value: ${card.value})" }
                                                            is Card.Property -> li { +"${card.cardType} (ID: ${card.id}, Value: ${card.value}, Colors: ${card.colors})"}
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        div(classes = "mt-2") {
                                            h4(classes = "font-medium") { +"Bank:" }
                                            if (playerState.bank.isEmpty()) {
                                                p(classes = "text-sm text-gray-600") { +"Empty" }
                                            } else {
                                                ul(classes = "list-disc list-inside text-sm text-gray-700") {
                                                    playerState.bank.forEach { card ->
                                                        li { +"${card.cardType} (ID: ${card.id}, Value: ${card.value ?: "N/A"})" }
                                                    }
                                                }
                                            }
                                        }
                                        div(classes = "mt-2") {
                                            h4(classes = "font-medium") { +"Properties:" }
                                            val propertyCollection = playerState.propertyCollection

                                            if (propertyCollection.collection.isEmpty()) {
                                                p(classes = "text-sm text-gray-600") { +"No properties." }
                                            } else {
                                                propertyCollection.collection.forEach { (setId, propertySet) ->
                                                    div(classes = "border-l-2 border-blue-400 pl-2 mt-2") {
                                                        p { +"Set ID: ${setId}" }
                                                        p { +"Color: ${propertySet.color ?: "Wild"}" }
                                                        p { +"Complete: ${propertySet.isComplete}" }
                                                        p { +"Rent: ${propertySet.calculateRent()}" }
                                                        div(classes = "ml-4") {
                                                            h5(classes = "font-normal") { +"Cards:" }
                                                            ul(classes = "list-disc list-inside text-xs text-gray-600") {
                                                                propertySet.properties.forEach { prop ->
                                                                    li { +"Property: ${prop.id} (Colors: ${prop.colors.joinToString()}, Value: ${prop.value ?: "N/A"})" }
                                                                }
                                                            }
                                                            propertySet.house?.let { house ->
                                                                p(classes = "text-xs text-gray-600") { +"House: ${house.id} (Value: ${house.value})" }
                                                            }
                                                            propertySet.hotel?.let { hotel ->
                                                                p(classes = "text-xs text-gray-600") { +"Hotel: ${hotel.id} (Value: ${hotel.value})" }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}