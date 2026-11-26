package com.example

import com.example.data.GlobalCountries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalCountriesTest {

    @Test
    fun `every supported country includes defaultState inside its states list`() {
        GlobalCountries.all.forEach { country ->
            assertTrue(
                "${country.name} should define states",
                country.states.isNotEmpty()
            )
            assertTrue(
                "${country.name} states should include defaultState ${country.defaultState}",
                country.states.contains(country.defaultState)
            )
        }
    }

    @Test
    fun `statesFor returns exact states for selected countries`() {
        val nigeriaStates = GlobalCountries.statesFor("NG")
        assertEquals(37, nigeriaStates.size)
        assertTrue(nigeriaStates.contains("Kano"))
        assertTrue(nigeriaStates.contains("Lagos"))
        assertTrue(nigeriaStates.contains("FCT Abuja"))

        val ghanaStates = GlobalCountries.statesFor("GH")
        assertTrue(ghanaStates.contains("Greater Accra"))
        assertTrue(ghanaStates.contains("Ashanti"))

        val usStates = GlobalCountries.statesFor("US")
        assertEquals(51, usStates.size)
        assertTrue(usStates.contains("California"))
        assertTrue(usStates.contains("Texas"))
    }
}
