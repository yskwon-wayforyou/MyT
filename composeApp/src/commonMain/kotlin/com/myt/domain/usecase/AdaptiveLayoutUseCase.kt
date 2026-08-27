package com.myt.domain.usecase

import com.myt.domain.model.GaugeLayoutMode
import com.myt.domain.model.LayoutConfig
import com.myt.domain.model.WindowHeightSizeClass
import com.myt.domain.model.WindowWidthSizeClass

class AdaptiveLayoutUseCase {
    /**
     * @param isLandscapeAspect true when maxWidth > maxHeight (physical landscape mount).
     */
    fun computeLayout(
        widthClass: WindowWidthSizeClass,
        heightClass: WindowHeightSizeClass,
        mode: GaugeLayoutMode = GaugeLayoutMode.Auto,
        isLandscapeAspect: Boolean = false,
    ): LayoutConfig = when (mode) {
        GaugeLayoutMode.Portrait -> {
            // Still prefer cluster when physically landscape so dash mount never sticks in vertical UI.
            if (isLandscapeAspect) LayoutConfig.Landscape else LayoutConfig.SinglePane
        }
        GaugeLayoutMode.Landscape -> LayoutConfig.Landscape
        GaugeLayoutMode.Split -> if (isLandscapeAspect) LayoutConfig.Landscape else LayoutConfig.TwoPane
        GaugeLayoutMode.Auto -> when {
            isLandscapeAspect || heightClass == WindowHeightSizeClass.Compact ->
                LayoutConfig.Landscape
            widthClass == WindowWidthSizeClass.Compact -> LayoutConfig.SinglePane
            widthClass == WindowWidthSizeClass.Medium -> LayoutConfig.TwoPane
            widthClass == WindowWidthSizeClass.Expanded -> LayoutConfig.ThreePane
            else -> LayoutConfig.SinglePane
        }
    }
}
