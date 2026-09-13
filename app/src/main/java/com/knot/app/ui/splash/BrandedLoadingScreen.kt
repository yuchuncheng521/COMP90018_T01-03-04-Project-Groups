package com.knot.app.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knot.app.R
import com.knot.app.ui.theme.KnotCream
import androidx.compose.ui.tooling.preview.Preview
import com.knot.app.ui.theme.JudsonFontFamily
import com.knot.app.ui.theme.KnotBlue
import com.knot.app.ui.theme.KnotTheme

@Preview(showBackground = true)
@Composable
fun BrandedLoadingScreenPreview() {
    KnotTheme {
        BrandedLoadingScreen()
    }
}
@Composable
fun BrandedLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(KnotBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-80).dp)
        ) {

            Image(
                painter = painterResource(R.drawable.knot_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .offset(x = 12.dp)

            )
            Text(
                text = "Knot",
                fontFamily = JudsonFontFamily,
                fontSize =52.sp,
                color = KnotCream
            )
        }
    }



}