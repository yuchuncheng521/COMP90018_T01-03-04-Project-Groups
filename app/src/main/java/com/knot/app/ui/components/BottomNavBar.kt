package com.knot.app.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.knot.app.navigation.MainScreen
import com.knot.app.ui.theme.KnotBlue
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotInk
import com.knot.app.ui.theme.KnotTheme

@Composable
fun KnotBottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isDefaultTheme = MaterialTheme.colorScheme.background == KnotCream

    val navBarContainerColor = if (isDefaultTheme) KnotBlue else MaterialTheme.colorScheme.primary
    val selectedIconColor = if (isDefaultTheme) KnotInk else MaterialTheme.colorScheme.primary
    val unselectedIconColor = if (isDefaultTheme) KnotCream.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
    val indicatorColor = if (isDefaultTheme) KnotCream else MaterialTheme.colorScheme.onPrimary

    // nav bar padding, shape
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp)),
        containerColor = navBarContainerColor
    ) {
        // nav bar point to action
        MainScreen.bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
                // nav bar icons
                icon = { 
                    Icon(
                        imageVector = ImageVector.vectorResource(id = screen.iconRes),
                        contentDescription = screen.label,
                        modifier = Modifier.size(40.dp)
                    ) 
                },
                // nav bar icon colour
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedIconColor,
                    unselectedIconColor = unselectedIconColor,
                    indicatorColor = indicatorColor
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KnotBottomNavBarPreview() {
    KnotTheme {
        KnotBottomNavBar(navController = rememberNavController())
    }
}
