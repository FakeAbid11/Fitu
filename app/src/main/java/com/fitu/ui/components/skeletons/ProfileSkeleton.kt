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
import com.fitu.ui.components.SkeletonText

/**
 * Full profile screen skeleton
 */
@Composable
fun ProfileSkeleton(
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonText(width = 80.dp, height = 28.dp)
            SkeletonBox(width = 70.dp, height = 36.dp, shape = RoundedCornerShape(18.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Avatar and name
        ProfileHeaderSkeleton()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // BMI Card
        BMICardSkeleton()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatSkeleton(modifier = Modifier.weight(1f))
            ProfileStatSkeleton(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatSkeleton(modifier = Modifier.weight(1f))
            ProfileStatSkeleton(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Settings section
        SkeletonText(width = 80.dp, height = 16.dp)
        Spacer(modifier = Modifier.height(12.dp))
        
        repeat(3) {
            SettingsItemSkeleton()
            if (it < 2) Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProfileHeaderSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SkeletonCircle(size = 100.dp)
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonText(width = 150.dp, height = 24.dp)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonText(width = 100.dp, height = 14.dp)
    }
}

@Composable
private fun BMICardSkeleton() {
    SkeletonCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SkeletonText(width = 30.dp, height = 12.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonText(width = 60.dp, height = 32.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonText(width = 80.dp, height = 14.dp)
            }
            
            // BMI indicator
            SkeletonBox(
                modifier = Modifier.weight(2f),
                height = 8.dp,
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}

@Composable
private fun ProfileStatSkeleton(modifier: Modifier = Modifier) {
    SkeletonCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkeletonCircle(size = 24.dp)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonText(width = 60.dp, height = 20.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonText(width = 50.dp, height = 12.dp)
        }
    }
}

@Composable
private fun SettingsItemSkeleton() {
    SkeletonCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonText(width = 100.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonText(width = 140.dp, height = 12.dp)
            }
            SkeletonCircle(size = 24.dp)
        }
    }
}
