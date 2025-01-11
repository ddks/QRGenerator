package com.example.qrgenerator

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRGeneratorScreen(
                onClear = { /* Optional: Clear additional data */ },
                onClose = { finish() }
            )
        }
    }
}

@Composable
fun QRGeneratorScreen(
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    val inputText = remember { mutableStateOf("") }
    val qrCodeBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val qrForegroundColor = remember { mutableStateOf(Color.Black) }
    val qrBackgroundColor = remember { mutableStateOf(Color.White) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section for QR Code display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(qrBackgroundColor.value),
            contentAlignment = Alignment.Center
        ) {
            qrCodeBitmap.value?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated QR Code",
                    modifier = Modifier.size(200.dp)
                )
            }
        }

        // Bottom section for input and controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Input field
            OutlinedTextField(
                value = inputText.value,
                onValueChange = { inputText.value = it },
                label = { Text("Enter text here") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code details
            Text(
                text = "Current QR Version: ${calculateVersion(inputText.value)}",
                fontSize = 16.sp
            )
            Text(
                text = "Graphical Size: ${calculateSize(calculateVersion(inputText.value))} px",
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Color selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("QR Foreground")
                    ColorPicker(currentColor = qrForegroundColor.value) { selectedColor ->
                        qrForegroundColor.value = selectedColor
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("QR Background")
                    ColorPicker(currentColor = qrBackgroundColor.value) { selectedColor ->
                        qrBackgroundColor.value = selectedColor
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons: Clear, Share, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = {
                    inputText.value = ""
                    qrCodeBitmap.value = null
                    onClear()
                }) {
                    Text("Clear")
                }

                Button(onClick = {
                    qrCodeBitmap.value?.let { bitmap ->
                        shareQRCode(context, bitmap)
                    }
                }) {
                    Text("Share")
                }

                Button(onClick = onClose) {
                    Text("Close")
                }
            }

            Spacer(modifier = Modifier.height(33.dp))

            // Text caption
            Text(
                text = "Special for my bro Max",
                fontSize = 32.sp,
                color = Color.Blue,
                modifier = Modifier.padding(top = 8.dp)
            )

        }
    }

    // Generate QR code whenever input or colors change
    LaunchedEffect(inputText.value, qrForegroundColor.value, qrBackgroundColor.value) {
        if (inputText.value.isNotEmpty()) {
            qrCodeBitmap.value = generateQRCode(
                content = inputText.value,
                version = calculateVersion(inputText.value),
                qrColor = qrForegroundColor.value,
                bgColor = qrBackgroundColor.value
            )
        } else {
            qrCodeBitmap.value = null
        }
    }
}

fun calculateVersion(content: String): Int {
    return when (content.length) {
        in 1..25 -> 1
        in 26..47 -> 2
        in 48..77 -> 3
        else -> 4
    }
}

fun calculateSize(version: Int): Int {
    val moduleSize = 10 // Scaling factor
    return when (version) {
        1 -> 21 * moduleSize
        2 -> 25 * moduleSize
        3 -> 29 * moduleSize
        4 -> 33 * moduleSize
        else -> 21 * moduleSize
    }
}

fun generateQRCode(content: String, version: Int, qrColor: Color, bgColor: Color): Bitmap {
    val qrCodeWriter = QRCodeWriter()
    val size = calculateSize(version)
    val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size / 10, size / 10)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width * 10, height * 10, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            for (dx in 0 until 10) {
                for (dy in 0 until 10) {
                    bitmap.setPixel(
                        x * 10 + dx,
                        y * 10 + dy,
                        if (bitMatrix[x, y]) qrColor.toArgb() else bgColor.toArgb()
                    )
                }
            }
        }
    }
    return bitmap
}

fun shareQRCode(context: android.content.Context, bitmap: Bitmap) {
    val uri: Uri? = try {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            "QR Code",
            null
        )
        Uri.parse(path)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    uri?.let {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, it)
            type = "image/png"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
    }
}

@Composable
fun ColorPicker(currentColor: Color, onColorSelected: (Color) -> Unit) {
    val colors = listOf(Color.Black, Color.White, Color.Red, Color.Green, Color.Blue)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color)
                    .clickable { onColorSelected(color) }
            )
        }
    }
}
