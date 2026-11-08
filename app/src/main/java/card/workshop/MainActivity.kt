package card.workshop

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CardWorkshopApp()
            }
        }
    }
}

private data class EditableCard(
    val title: String = "卡牌名称",
    val description: String = "在这里输入卡牌介绍。你可以修改文字、颜色、字号，并在卡牌上调整位置。",
    val tagOne: String = "技能卡",
    val tagTwo: String = "策略",
    val titleColor: Color = Color(0xFF242424),
    val bodyColor: Color = Color(0xFF444444),
    val tagColor: Color = Color(0xFF386A9B),
    val titleSize: Float = 26f,
    val bodySize: Float = 15f,
    val iconX: Float = 0.5f,
    val iconY: Float = 0.22f,
    val titleX: Float = 0.5f,
    val titleY: Float = 0.49f,
    val bodyX: Float = 0.5f,
    val bodyY: Float = 0.70f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardWorkshopApp() {
    val context = LocalContext.current
    var card by remember { mutableStateOf(EditableCard()) }
    var selected by remember { mutableStateOf("介绍") }
    var showEditor by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf("介绍") }
    var backgroundColor by remember { mutableStateOf(Color(0xFFF4E8CF)) }
    var importedImageName by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        importedImageName = uri?.lastPathSegment ?: if (uri != null) "已选择图片" else null
        if (uri != null) {
            Toast.makeText(context, "已选择图片。当前版本暂未将图片绘制到卡面。", Toast.LENGTH_LONG).show()
        }
    }

    fun openTextEditor(target: String) {
        editTarget = target
        editValue = when (target) {
            "名称" -> card.title
            "标签一" -> card.tagOne
            "标签二" -> card.tagTwo
            else -> card.description
        }
        showEditor = true
    }

    fun exportPng() {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(backgroundColor.toArgb())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.White.toArgb()
        canvas.drawRoundRect(45f, 45f, 1155f, 1555f, 55f, 55f, paint)

        paint.color = Color(0xFFDDCBAA).toArgb()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawRoundRect(45f, 45f, 1155f, 1555f, 55f, 55f, paint)
        paint.style = Paint.Style.FILL

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color(0xFF8C7658).toArgb()
        canvas.drawCircle(width * card.iconX, height * card.iconY, 125f, paint)

        paint.color = Color.White.toArgb()
        paint.textSize = 100f
        canvas.drawText("✦", width * card.iconX, height * card.iconY + 35f, paint)

        paint.color = card.titleColor.toArgb()
        paint.textSize = card.titleSize * 3f
        canvas.drawText(card.title, width * card.titleX, height * card.titleY, paint)

        paint.color = card.tagColor.toArgb()
        canvas.drawRoundRect(90f, 820f, 360f, 900f, 24f, 24f, paint)
        canvas.drawRoundRect(390f, 820f, 660f, 900f, 24f, 24f, paint)

        paint.color = Color.White.toArgb()
        paint.textSize = 38f
        canvas.drawText(card.tagOne, 225f, 875f, paint)
        canvas.drawText(card.tagTwo, 525f, 875f, paint)

        paint.color = card.bodyColor.toArgb()
        paint.textSize = card.bodySize * 2.3f
        paint.textAlign = Paint.Align.CENTER
        val lines = wrapText(card.description, 22)
        var y = height * card.bodyY
        val lineHeight = card.bodySize * 3.2f
        lines.take(10).forEach { line ->
            canvas.drawText(line, width * card.bodyX, y, paint)
            y += lineHeight
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "CardWorkshop_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CardWorkshop")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        try {
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            if (uri == null) {
                Toast.makeText(context, "创建图片文件失败", Toast.LENGTH_SHORT).show()
            } else {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, done, null, null)
                }
                Toast.makeText(context, "PNG 已保存到 Pictures/CardWorkshop", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
        } finally {
            bitmap.recycle()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自制卡牌工坊", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { exportPng() }) {
                        Text("导出 PNG")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("卡牌预览", style = MaterialTheme.typography.titleMedium)
            CardPreview(
                card = card,
                backgroundColor = backgroundColor,
                onSelect = { selected = it },
                onDrag = { target, dx, dy ->
                    card = when (target) {
                        "图标" -> card.copy(
                            iconX = (card.iconX + dx).coerceIn(0.12f, 0.88f),
                            iconY = (card.iconY + dy).coerceIn(0.08f, 0.42f)
                        )
                        "名称" -> card.copy(
                            titleX = (card.titleX + dx).coerceIn(0.15f, 0.85f)
                        )
                        else -> card.copy(
                            bodyX = (card.bodyX + dx).coerceIn(0.15f, 0.85f),
                            bodyY = (card.bodyY + dy).coerceIn(0.58f, 0.88f)
                        )
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) { Text("导入图标/素材") }

                OutlinedButton(
                    onClick = { backgroundColor = Color(0xFFF4E8CF) },
                    modifier = Modifier.weight(1f)
                ) { Text("重置底色") }
            }

            importedImageName?.let {
                Text("已选择素材：$it", style = MaterialTheme.typography.bodySmall)
            }

            Text("编辑内容", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("名称", "介绍", "标签一", "标签二").forEach { item ->
                    FilterChip(
                        selected = selected == item,
                        onClick = { selected = item },
                        label = { Text(item) }
                    )
                }
            }

            when (selected) {
                "名称" -> {
                    Button(onClick = { openTextEditor("名称") }) { Text("修改卡牌名称") }
                    Text("名称仅支持横向移动，可在预览中拖动。")
                }
                "标签一", "标签二" -> {
                    Button(onClick = { openTextEditor(selected) }) {
                        Text("修改$selected文字")
                    }
                    Text("标签颜色")
                    ColorChoices(card.tagColor) {
                        card = card.copy(tagColor = it)
                    }
                }
                else -> {
                    Button(onClick = { openTextEditor("介绍") }) { Text("编辑卡牌介绍") }
                    Text("介绍字号：${card.bodySize.toInt()}sp")
                    Slider(
                        value = card.bodySize,
                        onValueChange = { card = card.copy(bodySize = it) },
                        valueRange = 10f..24f
                    )
                    Text("介绍颜色")
                    ColorChoices(card.bodyColor) {
                        card = card.copy(bodyColor = it)
                    }
                }
            }

            Text("名称字号：${card.titleSize.toInt()}sp")
            Slider(
                value = card.titleSize,
                onValueChange = { card = card.copy(titleSize = it) },
                valueRange = 18f..36f
            )
            Text("名称颜色")
            ColorChoices(card.titleColor) {
                card = card.copy(titleColor = it)
            }

            Text(
                "当前为基础修正版：支持文字编辑、拖动、颜色字号调整和 PNG 导出。图片素材绘制、字体切换及斜体功能仍需后续接入。",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("编辑$editTarget") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (editTarget == "介绍") 4 else 1,
                    label = { Text("输入文字") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    card = when (editTarget) {
                        "名称" -> card.copy(title = editValue)
                        "标签一" -> card.copy(tagOne = editValue)
                        "标签二" -> card.copy(tagTwo = editValue)
                        else -> card.copy(description = editValue)
                    }
                    showEditor = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("取消") }
            }
        )
    }
}

private fun wrapText(text: String, maxChars: Int): List<String> {
    if (text.isEmpty()) return listOf("")
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var count = 0
    text.forEach { ch ->
        if (ch == '\n') {
            result.add(current.toString())
            current = StringBuilder()
            count = 0
        } else {
            current.append(ch)
            count++
            if (count >= maxChars) {
                result.add(current.toString())
                current = StringBuilder()
                count = 0
            }
        }
    }
    if (current.isNotEmpty() || result.isEmpty()) result.add(current.toString())
    return result
}

@Composable
private fun CardPreview(
    card: EditableCard,
    backgroundColor: Color,
    onSelect: (String) -> Unit,
    onDrag: (String, Float, Float) -> Unit
) {
    var dragTarget by remember { mutableStateOf("介绍") }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .border(2.dp, Color(0xFFBBA77F), RoundedCornerShape(22.dp))
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthPx, heightPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val candidates = listOf(
                                "图标" to Offset(card.iconX * widthPx, card.iconY * heightPx),
                                "名称" to Offset(card.titleX * widthPx, card.titleY * heightPx),
                                "介绍" to Offset(card.bodyX * widthPx, card.bodyY * heightPx)
                            )
                            dragTarget = candidates.minBy { (_, point) ->
                                (offset - point).getDistance()
                            }.first
                            onSelect(dragTarget)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(
                                dragTarget,
                                dragAmount.x / widthPx,
                                dragAmount.y / heightPx
                            )
                        }
                    )
                }
        ) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * 0.06f, size.height * 0.04f),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.88f,
                    size.height * 0.92f
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
            )

            drawCircle(
                color = Color(0xFF8C7658),
                radius = size.width * 0.105f,
                center = Offset(size.width * card.iconX, size.height * card.iconY)
            )

            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    color = android.graphics.Color.WHITE
                    textSize = size.width * 0.11f
                }
                drawText(
                    "✦",
                    size.width * card.iconX,
                    size.height * card.iconY + paint.textSize * 0.35f,
                    paint
                )

                paint.color = card.titleColor.toArgb()
                paint.textSize = card.titleSize * density * 0.85f
                drawText(
                    card.title,
                    size.width * card.titleX,
                    size.height * card.titleY,
                    paint
                )

                paint.color = card.tagColor.toArgb()
                paint.textSize = 13f * density
                drawRoundRect(
                    size.width * 0.13f, size.height * 0.57f,
                    size.width * 0.40f, size.height * 0.63f,
                    10f, 10f, paint
                )
                drawRoundRect(
                    size.width * 0.45f, size.height * 0.57f,
                    size.width * 0.72f, size.height * 0.63f,
                    10f, 10f, paint
                )

                paint.color = android.graphics.Color.WHITE
                paint.textSize = 12f * density
                drawText(card.tagOne, size.width * 0.265f, size.height * 0.61f, paint)
                drawText(card.tagTwo, size.width * 0.585f, size.height * 0.61f, paint)

                paint.color = card.bodyColor.toArgb()
                paint.textSize = card.bodySize * density * 0.75f
                wrapText(card.description, 18).take(7).forEachIndexed { index, line ->
                    drawText(
                        line,
                        size.width * card.bodyX,
                        size.height * card.bodyY + index * paint.textSize * 1.55f,
                        paint
                    )
                }
            }
        }

        Text(
            text = "拖动图标、名称或介绍可调整位置",
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColorChoices(
    current: Color,
    onChoose: (Color) -> Unit
) {
    val colors = listOf(
        Color(0xFF242424),
        Color(0xFF386A9B),
        Color(0xFFB84B43),
        Color(0xFF54835D),
        Color(0xFF8A5CA8),
        Color(0xFFCC8B32)
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (color == current) 3.dp else 1.dp,
                        color = if (color == current) Color.Black else Color.LightGray,
                        shape = CircleShape
                    )
                    .clickable { onChoose(color) }
            )
        }
    }
}
