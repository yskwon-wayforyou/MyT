package com.myt.domain

import com.myt.domain.model.WindowHeightSizeClass
import com.myt.domain.model.WindowWidthSizeClass
import com.myt.domain.model.LayoutConfig
import com.myt.domain.usecase.AdaptiveLayoutUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveLayoutUseCaseTest {
    private val useCase = AdaptiveLayoutUseCase()

    @Test
    fun phoneLandscape_usesLandscapeLayout() {
        val config = useCase.computeLayout(
            widthClass = WindowWidthSizeClass.Medium,
            heightClass = WindowHeightSizeClass.Compact,
        )
        assertEquals(LayoutConfig.Landscape, config)
    }

    @Test
    fun phonePortrait_usesSinglePane() {
        val config = useCase.computeLayout(
            widthClass = WindowWidthSizeClass.Compact,
            heightClass = WindowHeightSizeClass.Medium,
        )
        assertEquals(LayoutConfig.SinglePane, config)
    }

    @Test
    fun splitMode_forcesTwoPaneEvenOnPhone() {
        val config = useCase.computeLayout(
            widthClass = WindowWidthSizeClass.Compact,
            heightClass = WindowHeightSizeClass.Medium,
            mode = com.myt.domain.model.GaugeLayoutMode.Split,
        )
        assertEquals(LayoutConfig.TwoPane, config)
    }

    @Test
    fun portraitMode_forcesSinglePaneInLandscapeHeight() {
        val config = useCase.computeLayout(
            widthClass = WindowWidthSizeClass.Medium,
            heightClass = WindowHeightSizeClass.Compact,
            mode = com.myt.domain.model.GaugeLayoutMode.Portrait,
        )
        assertEquals(LayoutConfig.SinglePane, config)
    }
}
