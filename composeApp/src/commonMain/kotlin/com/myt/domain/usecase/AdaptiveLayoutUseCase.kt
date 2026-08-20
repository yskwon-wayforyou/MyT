package com.myt.domain.usecase

import com.myt.domain.model.LayoutConfig
import com.myt.domain.model.WindowHeightSizeClass
import com.myt.domain.model.WindowWidthSizeClass

class AdaptiveLayoutUseCase {
    fun computeLayout(
        widthClass: WindowWidthSizeClass,
        heightClass: WindowHeightSizeClass,
    ): LayoutConfig = when (widthClass) {
        WindowWidthSizeClass.Compact -> {
            if (heightClass == WindowHeightSizeClass.Compact) {
                LayoutConfig.TwoPane
            } else {
                LayoutConfig.SinglePane
            }
        }
        WindowWidthSizeClass.Medium -> LayoutConfig.TwoPane
        WindowWidthSizeClass.Expanded -> LayoutConfig.ThreePane
    }
}
