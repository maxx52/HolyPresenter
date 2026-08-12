package holypresenter.org.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal const val HolyPresenterVersion = "1.0.3"

@Composable
internal fun SplashScreen(
    message: String,
    isError: Boolean = false
) {
    val accent = Color(0xFFE7D5FF)
    val background = useResource("holypresenter-splash.png", ::loadImageBitmap)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = background,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x33080A24)).padding(36.dp)
        ) {
            Text(
                text = "HolyPresenter",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isError) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFFFDE84),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.size(10.dp))
                    }
                    Text(
                        text = message,
                        color = if (isError) Color(0xFFFFB4AB) else accent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Версия $HolyPresenterVersion",
                    color = Color(0xFFBFAFCE),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
