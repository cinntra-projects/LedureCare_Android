package com.cinntra.ledurecare.feature.dealers.data

import com.cinntra.ledurecare.feature.dealers.domain.Dealer

/**
 * Sample dealers used while `BuildConfig.USE_STATIC_DEALERS` is on: 24 in the North UP zone,
 * 8 in Noida, 6 in Meerut and 4 in Bijnor, as in the design. Ids 1-9 are used by the sample plan.
 */
object StaticDealers {
    private const val ZONE = "North UP"
    private val SalesPeople = listOf("Rohit Sharma", "Neha Gupta", "Aman Verma")

    val all: List<Dealer> = listOf(
        Dealer("1", "Amit Electric", "BP-23125", "Noida"),
        Dealer("2", "Sumit Electric", "BP-23128", "Meerut"),
        Dealer("3", "ABM Electrical", "BP-21222", "Bijnor"),
        Dealer("4", "Verma Motors", "BP-22310", "Ghaziabad"),
        Dealer("5", "Gupta Spares", "BP-23140", "Firozabad"),
        Dealer("6", "Delta Auto", "BP-22871", "Noida"),
        Dealer("7", "Star Electric", "BP-22655", "Noida"),
        Dealer("8", "Sharma Auto Agency", "BP-23090", "Agra"),
        Dealer("9", "Kapoor Traders", "BP-22987", "Aligarh"),
        Dealer("10", "Singh Electricals", "BP-23201", "Mathura"),
        Dealer("11", "Jain Hardware", "BP-22764", "Bijnor"),
        Dealer("12", "Mehta Lighting", "BP-23318", "Bareilly"),
        Dealer("13", "Rathi Enterprises", "BP-22411", "Noida"),
        Dealer("14", "Bansal Electricals", "BP-22502", "Noida"),
        Dealer("15", "Tyagi Power House", "BP-22619", "Noida"),
        Dealer("16", "Khanna Agencies", "BP-22733", "Noida"),
        Dealer("17", "Saxena Lights", "BP-22848", "Noida"),
        Dealer("18", "Goel Brothers", "BP-22956", "Meerut"),
        Dealer("19", "Chauhan Motors", "BP-23067", "Meerut"),
        Dealer("20", "Pandey Traders", "BP-23172", "Meerut"),
        Dealer("21", "Rastogi Electric", "BP-23284", "Meerut"),
        Dealer("22", "Malik Spares", "BP-23391", "Meerut"),
        Dealer("23", "Siddiqui Hardware", "BP-22188", "Bijnor"),
        Dealer("24", "Yadav Auto Parts", "BP-22294", "Bijnor"),
    ).map {
        it.copy(
            zone = ZONE,
            address = "Main Market, ${it.city}, UP",
            salesPerson = SalesPeople[it.id.toInt() % SalesPeople.size],
        )
    }.map {
        if (it.id == "2") it.copy(address = "Delhi Road, Meerut, UP", salesPerson = "Rohit Sharma") else it
    }

    /** Sample dealers are numbered "1" to "24"; the sample plan refers to them by these numbers. */
    fun byId(id: Int): Dealer = all.first { it.id == id.toString() }
}
