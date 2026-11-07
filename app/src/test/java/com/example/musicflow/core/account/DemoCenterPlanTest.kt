package com.example.musicflow.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoCenterPlanTest {
    @Test fun everyPersonaHasACompleteButClearlyMockedJourney() {
        assertEquals(9, DemoPersona.entries.size)
        DemoPersona.entries.forEach { persona ->
            val steps = DemoCenterPlan.steps(persona)
            assertTrue(steps.size in 2..3)
            assertTrue(steps.all { it.title.isNotBlank() && it.explanation.isNotBlank() &&
                it.action.isNotBlank() && it.outcome.isNotBlank() })
        }
    }

    @Test fun progressCannotExceedScenarioStepsOrBecomeNegative() {
        DemoPersona.entries.forEach { persona ->
            assertEquals(0, DemoCenterPlan.boundedProgress(persona, -5))
            assertEquals(1, DemoCenterPlan.advance(persona, 0))
            assertEquals(DemoCenterPlan.steps(persona).size,
                DemoCenterPlan.advance(persona, 100))
        }
    }

    @Test fun ownerCanIllustrateDelegationButPremiumCannot() {
        assertTrue(DemoCenterPlan.steps(DemoPersona.OWNER)
            .any { "Cấp quyền demo" == it.action })
        assertTrue(DemoCenterPlan.steps(DemoPersona.PREMIUM)
            .none { "quyền" in it.action.lowercase() })
    }

    @Test fun storyActionsAreDescriptiveAndDoNotExposeRpcNames() {
        DemoPersona.entries.flatMap { DemoCenterPlan.steps(it) }.forEach { step ->
            assertTrue(!step.action.contains("musicflow_"))
            assertTrue(!step.outcome.contains("Bearer "))
        }
    }
}
