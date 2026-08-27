package com.myt.domain.usecase

import com.myt.domain.model.GaugeLayoutMode
import com.myt.domain.model.LayoutConfig
import com.myt.domain.model.WindowHeightSizeClass
import com.myt.domain.model.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveLayoutUseCaseTest {
    private val useCase = AdaptiveLayoutUseCase()

    @Test
    fun landscapeAspect_forcesClusterEvenWhenHeightNotCompact() {
        val config = useCase.computeLayout(
            widthClass = WindowWidthSizeClass.Medium,
            heightClass = WindowHeightSizeClass.Medium,
            mode = GaugeLayoutMode.Auto,
            isLandscapeAspect = true,
        )
        assertEquals(LayoutConfig.Landscape, config)
    }

    @Test
    fun portraitAspect_compactWidth_isSinglePane() {
        val config = useCase.computeLayout(
            widthClass = WindowWidthSizeClass.Compact,
            heightClass = WindowHeightSizeClass.Medium,
            mode = GaugeLayoutMode.Auto,
            isLandscapeAspect = false,
        )
        assertEquals(LayoutConfig.SinglePane, config)
    }
}
