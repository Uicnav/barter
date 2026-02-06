package com.barter.server.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init() {
        Database.connect(
            url = "jdbc:h2:mem:barter;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )

        transaction {
            SchemaUtils.create(
                Users, Listings, ListingTags, Swipes,
                Matches, Messages, Deals, DealItems,
            )
            seedData()
        }
    }

    private fun seedData() {
        val now = System.currentTimeMillis()

        // Users
        listOf(
            Triple("u1", "Mihai", "Bălți") to 4.6,
            Triple("u2", "Alina", "Chișinău") to 4.9,
            Triple("u3", "Sergiu", "Orhei") to 4.4,
            Triple("u4", "Ana", "Chișinău") to 4.7,
            Triple("u5", "Vlad", "Tiraspol") to 4.3,
            Triple("u6", "Elena", "Chișinău") to 4.8,
            Triple("u7", "Andrei", "Bălți") to 4.5,
            Triple("u8", "Maria", "Comrat") to 4.9,
        ).forEach { (triple, rating) ->
            Users.insert {
                it[id] = triple.first
                it[displayName] = triple.second
                it[location] = triple.third
                it[this.rating] = rating
                it[createdAt] = now
            }
        }

        // Test user
        Users.insert {
            it[id] = "me"
            it[displayName] = "Ion"
            it[location] = "Chișinău"
            it[rating] = 4.8
            it[createdAt] = now
        }

        // Listings
        data class Seed(val id: String, val ownerId: String, val kind: String, val title: String, val desc: String, val tags: List<String>)

        listOf(
            Seed("l1", "u1", "GOODS", "Schimb bicicletă", "Ofer bicicletă mountain bike, caut Apple Watch sau servicii IT.", listOf("Sport", "Tech")),
            Seed("l2", "u2", "SERVICES", "Machiaj și manichiură", "Ofer servicii de beauty la domiciliu. Caut AirPods sau alte gadget-uri.", listOf("Beauty", "Home")),
            Seed("l3", "u3", "BOTH", "Laptop + reparații", "Ofer laptop vechi + ajutor tehnic. Caut telefon sau servicii auto.", listOf("Tech", "Auto")),
            Seed("l4", "u4", "SERVICES", "Lecții de chitară", "Predau chitară acustică și electrică. Caut echipament foto sau design grafic.", listOf("Music", "Art")),
            Seed("l5", "u5", "GOODS", "Geacă de iarnă", "Geacă North Face, mărimea L, stare excelentă. Caut cărți sau board games.", listOf("Fashion", "Winter")),
            Seed("l6", "u6", "SERVICES", "Sesiune foto", "Ofer sesiune foto profesională. Caut cursuri de programare sau design web.", listOf("Photo", "Creative")),
            Seed("l7", "u7", "GOODS", "Colecție board games", "Ofer Catan, Ticket to Ride, Codenames. Caut echipament sport.", listOf("Games", "Fun")),
            Seed("l8", "u8", "SERVICES", "Web design & branding", "Creez site-uri și identități vizuale. Caut servicii de traducere sau catering.", listOf("Design", "Web")),
        ).forEach { s ->
            Listings.insert {
                it[id] = s.id
                it[ownerId] = s.ownerId
                it[kind] = s.kind
                it[title] = s.title
                it[description] = s.desc
                it[createdAt] = now
            }
            s.tags.forEach { tag ->
                ListingTags.insert {
                    it[listingId] = s.id
                    it[this.tag] = tag
                }
            }
        }
    }
}
