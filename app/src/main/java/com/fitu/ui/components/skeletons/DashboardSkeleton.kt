package com.fitu.ui.components.skeletons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.fitu.ui.components.SkeletonProgressRing
import com.fitu.ui.components.SkeletonText
import com.fitu.ui.components.SkeletonWeeklyChart

/**
 * Full dashboard skeleton loading screen
 */
@Composable
fun DashboardSkeleton(
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
        // Header skeleton
        DashboardHeaderSkeleton()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Motivational message skeleton
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(),
            height = 44.dp,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Streak card skeleton
        StreakCardSkeleton()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Steps card skeleton
        StepsCardSkeleton()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Distance & Burned row skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallStatCardSkeleton(modifier = Modifier.weight(1f))
            SmallStatCardSkeleton(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Nutrition card skeleton
        NutritionCardSkeleton()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Weekly activity skeleton
        WeeklyActivitySkeleton()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // AI Coach card skeleton
        AICoachCardSkeleton()
    }
}

@Composable
private fun DashboardHeaderSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SkeletonText(width = 200.dp, height = 24.dp)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonText(width = 140.dp, height = 14.dp)
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Streak badge skeleton
            SkeletonBox(
                width = 60.dp,
                height = 32.dp,
                shape = RoundedCornerShape(16.dp)
            )
            // Avatar skeleton
            SkeletonCircle(size = 48.dp)
        }
    }
}

@Composable
private fun StreakCardSkeleton() {
    SkeletonCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fire icon placeholder
            SkeletonCircle(size = 48.dp)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                SkeletonText(width = 80.dp, height = 32.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonText(width = 100.dp, height = 12.dp)
            }
            
            // Best streak badge
            SkeletonBox(
                width = 50.dp,
                height = 60.dp,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun StepsCardSkeleton() {
    SkeletonCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonCircle(size = 18.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    SkeletonText(width = 50.dp, height = 16.dp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonText(width = 120.dp, height = 36.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonText(width = 100.dp, height = 14.dp)
            }
            
            SkeletonProgressRing(size = 64.dp)
        }
    }
}

@Composable
private fun SmallStatCardSkeleton(modifier: Modifier = Modifier) {
    SkeletonCard(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonCircle(size = 16.dp)
                Spacer(modifier = Modifier.width(6.dp))
                SkeletonText(width = 60.dp, height = 12.dp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                SkeletonText(width = 50.dp, height = 24.dp)
                Spacer(modifier = Modifier.width(4.dp))
                SkeletonText(width = 30.dp, height = 12.dp)
            }
        }
    }
}

@Composable
private fun NutritionCardSkeleton() {
    SkeletonCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonCircle(size = 18.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    SkeletonText(width = 70.dp, height = 16.dp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonText(width = 160.dp, height = 13.dp)
            }
            
            SkeletonBox(
                width = 60.dp,
                height = 32.dp,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun WeeklyActivitySkeleton() {
    SkeletonCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonText(width = 120.dp, height = 16.dp)
                SkeletonText(width = 50.dp, height = 12.dp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SkeletonWeeklyChart()
        }
    }
}

@Composable
private fun AICoachCardSkeleton() {
    SkeletonBox(
        modifier = Modifier.fillMaxWidth(),
        height = 80.dp,
        shape = RoundedCornerShape(24.dp)
    )
}
