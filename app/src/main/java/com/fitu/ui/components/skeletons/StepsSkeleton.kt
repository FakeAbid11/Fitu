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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fitu.ui.components.SkeletonBox
import com.fitu.ui.components.SkeletonCard
import com.fitu.ui.components.SkeletonText
import com.fitu.ui.components.SkeletonWeeklyChart
import com.fitu.ui.components.shimmerBrush

/**
 * Full steps screen skeleton
 */
@Composable
fun StepsSkeleton(
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
        SkeletonText(width = 80.dp, height = 28.dp)
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonText(width = 100.dp, height = 14.dp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Large progress ring
        LargeProgressRingSkeleton()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepStatSkeleton(modifier = Modifier.weight(1f))
            StepStatSkeleton(modifier = Modifier.weight(1f))
            StepStatSkeleton(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Weekly chart
        SkeletonCard {
            Column {
                SkeletonText(width = 120.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(16.dp))
                SkeletonWeeklyChart()
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Service status
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp
        )
    }
}

@Composable
private fun LargeProgressRingSkeleton() {
    val brush = shimmerBrush()
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(brush),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0A0F)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SkeletonText(width = 100.dp, height = 40.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonText(width = 60.dp, height = 14.dp)
                }
            }
        }
    }
}

@Composable
private fun StepStatSkeleton(modifier: Modifier = Modifier) {
    SkeletonCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkeletonText(width = 50.dp, height = 24.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonText(width = 40.dp, height = 12.dp)
        }
    }
}
