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
                Notifications, Reviews,
            )
            seedData()
        }
    }

    private fun seedData() {
        val now = System.currentTimeMillis()

        // Users — (id, name, city, rating, lat, lng)
        data class SeedUser(val id: String, val name: String, val city: String, val rating: Double, val lat: Double, val lng: Double)
        listOf(
            SeedUser("u1", "Mihai", "Bălți", 4.6, 47.7617, 27.9293),
            SeedUser("u2", "Alina", "Chișinău", 4.9, 47.0105, 28.8638),
            SeedUser("u3", "Sergiu", "Orhei", 4.4, 47.3816, 28.8253),
            SeedUser("u4", "Ana", "Chișinău", 4.7, 47.0245, 28.8325),
            SeedUser("u5", "Vlad", "Tiraspol", 4.3, 46.8403, 29.6433),
            SeedUser("u6", "Elena", "Chișinău", 4.8, 47.0056, 28.8575),
            SeedUser("u7", "Andrei", "Bălți", 4.5, 47.7556, 27.9167),
            SeedUser("u8", "Maria", "Comrat", 4.9, 46.2955, 28.6578),
        ).forEach { u ->
            Users.insert {
                it[id] = u.id
                it[displayName] = u.name
                it[location] = u.city
                it[this.rating] = u.rating
                it[latitude] = u.lat
                it[longitude] = u.lng
                it[createdAt] = now
            }
        }

        // Test user
        Users.insert {
            it[id] = "me"
            it[displayName] = "Ion"
            it[location] = "Chișinău"
            it[rating] = 4.8
            it[latitude] = 47.0105
            it[longitude] = 28.8638
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
