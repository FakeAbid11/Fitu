package com.fitu.ui.components.skeletons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fitu.ui.components.SkeletonBox
import com.fitu.ui.components.SkeletonCard
import com.fitu.ui.components.SkeletonCircle
import com.fitu.ui.components.SkeletonMealItem
import com.fitu.ui.components.SkeletonText

/**
 * Full nutrition screen skeleton
 */
@Composable
fun NutritionSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 120.dp)
    ) {
        // Header
        SkeletonText(width = 120.dp, height = 28.dp)
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonText(width = 140.dp, height = 14.dp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Calories card
        CaloriesCardSkeleton()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Today's meals header
        SkeletonText(width = 120.dp, height = 18.dp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Meal items
        repeat(4) {
            SkeletonMealItem()
            if (it < 3) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CaloriesCardSkeleton() {
    SkeletonCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SkeletonText(width = 120.dp, height = 11.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    SkeletonText(width = 80.dp, height = 32.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    SkeletonText(width = 60.dp, height = 16.dp)
                }
            }
            
            SkeletonCircle(size = 48.dp)
        }
    }
}
