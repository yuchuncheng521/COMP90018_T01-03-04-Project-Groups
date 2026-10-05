package com.knot.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.knot.app.R


// Text sizes use sp (per project plan Guidelines) so they scale with user font settings.
val JudsonFontFamily = FontFamily(
    Font(R.font.judson_regular, FontWeight.Normal),
    Font(R.font.judson_bold, FontWeight.Bold)
)

val KnotTypography = Typography(
    headlineMedium = TextStyle(fontFamily = JudsonFontFamily,fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = JudsonFontFamily,fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = JudsonFontFamily,fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = JudsonFontFamily,fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = JudsonFontFamily,fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = JudsonFontFamily,fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = JudsonFontFamily,fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

