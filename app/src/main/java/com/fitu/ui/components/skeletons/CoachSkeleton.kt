package com.fitu.ui.components.skeletons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fitu.ui.components.SkeletonBox
import com.fitu.ui.components.SkeletonCard
import com.fitu.ui.components.SkeletonCircle
import com.fitu.ui.components.SkeletonText
import com.fitu.ui.components.shimmerBrush

/**
 * Full coach screen skeleton
 */
@Composable
fun CoachSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 120.dp)
    ) {
        // Header
        SkeletonText(width = 100.dp, height = 28.dp)
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonText(width = 180.dp, height = 14.dp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Exercise selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                SkeletonBox(
                    modifier = Modifier.weight(1f),
                    height = 80.dp,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Camera preview placeholder
        CameraPreviewSkeleton()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CoachStatSkeleton(modifier = Modifier.weight(1f))
            CoachStatSkeleton(modifier = Modifier.weight(1f))
            CoachStatSkeleton(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Feedback card
        SkeletonCard {
            Column {
                SkeletonText(width = 80.dp, height = 14.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonText(height = 16.dp)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Start button
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
private fun CameraPreviewSkeleton() {
    val brush = shimmerBrush()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(24.dp))
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkeletonCircle(size = 64.dp)
            Spacer(modifier = Modifier.height(16.dp))
            SkeletonText(width = 120.dp, height = 16.dp)
        }
    }
}

@Composable
private fun CoachStatSkeleton(modifier: Modifier = Modifier) {
    SkeletonCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkeletonText(width = 50.dp, height = 28.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonText(width = 40.dp, height = 12.dp)
        }
    }
}
