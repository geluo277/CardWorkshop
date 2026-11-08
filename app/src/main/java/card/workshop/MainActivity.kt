package card.workshop

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.content.ContentValues
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CardWorkshopApp() } }
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

@Composable
fun CardWorkshopApp() {
    val context = LocalContext.current
    var card by remember { mutableStateOf(EditableCard()) }
    var selected by remember { mutableStateOf("介绍") }
    var editDialog by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf("介绍") }
    var backgroundColor by remember { mutableStateOf(Color(0xFFF4E8CF)) }
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    fun beginEdit(target: String) {
        editTarget = target
        editValue = when(target) {
            "名称" -> card.title
            "标签一" -> card.tagOne
            "标签二" -> card.tagTwo
            else -> card.description
        }
        editDialog = true
    }

    fun savePng() {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(backgroundColor.toArgb())
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.White.toArgb()
        canvas.drawRoundRect(45f, 45f, 1155f, 1555f, 55f, 55f, p)
        p.color = Color(0xFFDDCBAA).toArgb()
        p.style = Paint.Style.STROKE
        p.strokeWidth = 8f
        canvas.drawRoundRect(45f, 45f, 1155f, 1555f, 55f, 55f, p)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.color = Color(0xFF8C7658).toArgb()
        p.textSize = 100f
        canvas.drawCircle(width * card.iconX, height * card.iconY, 125f, p)
        p.color = Color.White.toArgb()
        p.textSize = 100f
        canvas.drawText("✦", width * card.iconX, height * card.iconY + 35f, p)
        p.color = card.titleColor.toArgb()
        p.textSize = card.titleSize * 3.0f
        canvas.drawText(card.title, width * card.titleX, height * card.titleY, p)
        p.color = card.tagColor.toArgb()
        p.textSize = 42f
        canvas.drawRoundRect(90f, 820f, 360f, 900f, 24f, 24f, p)
        canvas.drawRoundRect(390f, 820f, 660f, 900f, 24f, 24f, p)
        p.color = Color.White.toArgb()
        p.textSize = 38f
        canvas.drawText(card.tagOne, 225f, 875f, p)
        canvas.drawText(card.tagTwo, 525f, 875f, p)
        p.color = card.bodyColor.toArgb()
        p.textSize = card.bodySize * 2.3f
        val words = card.description.chunked(22)
        var y = height * card.bodyY
        words.take(8).forEach {
            canvas.drawText(it, width * card.bodyX, y, p)
            y += card.bodySize * 3.2f
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "CardWorkshop_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CardWorkshop")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(context, "PNG 已保存到相册", Toast.LENGTH_LONG).show()
        } else Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
        bitmap.recycle()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自制卡牌工坊", fontWeight = FontWeight.Bold) },
                actions = { TextButton(onClick = { savePng() }) { Text("导出 PNG") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("卡牌预览", style = MaterialTheme.typography.titleMedium)
            CardPreview(card, backgroundColor, selected, onSelect = { selected = it },
                onDrag = { target, dx, dy ->
                    card = when(target) {
                        "图标" -> card.copy(iconX = (card.iconX + dx).coerceIn(0.12f,0.88f), iconY = (card.iconY + dy).coerceIn(0.08f,0.42f))
                        "名称" -> card.copy(titleX = (card.titleX + dx).coerceIn(0.15f,0.85f))
                        else -> card.copy(bodyX = (card.bodyX + dx).coerceIn(0.15f,0.85f), bodyY = (card.bodyY + dy).coerceIn(0.58f,0.88f))
                    }
                },
                onEdit = { beginEdit(it) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("导入图标/素材") }
                OutlinedButton(onClick = { backgroundColor = Color(0xFFF4E8CF) }, modifier = Modifier.weight(1f)) { Text("重置底色") }
            }
            if (imageUri != null) Text("已选择素材：${imageUri}", style = MaterialTheme.typography.bodySmall)
            Text("编辑内容", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("名称","介绍","标签一","标签二").forEach { item ->
                    FilterChip(selected = selected == item, onClick = { selected = item }, label = { Text(item) })
                }
            }
            when(selected) {
                "名称" -> {
                    Button(onClick = { beginEdit("名称") }) { Text("修改卡牌名称") }
                    Text("名称仅支持横向移动；可在预览中拖动。")
                }
                "标签一", "标签二" -> {
                    Button(onClick = { beginEdit(selected) }) { Text("修改$selected文字") }
                    Text("标签颜色：")
                    ColorChoices(card.tagColor) { card = card.copy(tagColor = it) }
                }
                else -> {
                    Button(onClick = { beginEdit("介绍") }) { Text("编辑卡牌介绍") }
                    Text("介绍字号：${card.bodySize.toInt()}sp")
                    Slider(value = card.bodySize, onValueChange = { card = card.copy(bodySize = it) }, valueRange = 10f..24f)
                    Text("介绍颜色：")
                    ColorChoices(card.bodyColor) { card = card.copy(bodyColor = it) }
                }
            }
            Text("名称字号：${card.titleSize.toInt()}sp")
            Slider(value = card.titleSize, onValueChange = { card = card.copy(titleSize = it) }, valueRange = 18f..36f)
            Text("名称颜色：")
            ColorChoices(card.titleColor) { card = card.copy(titleColor = it) }
            Text("提示：当前版本提供基础文字编辑、拖动与 PNG 导出；素材替换、字体管理和更精细的排版将在后续版本完善。", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }

    if (editDialog) {
        AlertDialog(
            onDismissRequest = { editDialog = false },
            title = { Text("编辑$editTarget") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if(editTarget == "介绍") 4 else 1,
                    label = { Text("输入文字") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    card = when(editTarget) {
                        "名称" -> card.copy(title = editValue)
                        "标签一" -> card.copy(tagOne = editValue)
                        "标签二" -> card.copy(tagTwo = editValue)
                        else -> card.copy(description = editValue)
                    }
                    editDialog = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun CardPreview(
    card: EditableCard,
    bg: Color,
    selected: String,
    onSelect: (String) -> Unit,
    onDrag: (String, Float, Float) -> Unit,
    onEdit: (String) -> Unit
) {
    var lastSize by remember { mutableStateOf(IntSize.Zero) }
    var dragTarget by remember { mutableStateOf("介绍") }
    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(22.dp))
            .background(bg).border(2.dp, Color(0xFFBBA77F), RoundedCornerShape(22.dp))
    ) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        Canvas(Modifier.fillMaxSize().pointerInput(card, w, h) {
            detectDragGestures(
                onDragStart = { offset ->
                    val icon = Offset(card.iconX*w, card.iconY*h)
                    val title = Offset(card.titleX*w, card.titleY*h)
                    val body = Offset(card.bodyX*w, card.bodyY*h)
                    dragTarget = listOf("图标" to (offset-icon).getDistance(),
                        "名称" to (offset-title).getDistance(),
                        "介绍" to (offset-body).getDistance()).minBy { it.second }.first
                    onSelect(dragTarget)
                },
                onDrag = { change, drag ->
                    change.consume()
                    val dx = drag.x / w
                    val dy = drag.y / h
                    onDrag(dragTarget, dx, dy)
                }
            )
        }) {
            drawRoundRect(Color.White, topLeft = Offset(size.width*0.06f,size.height*0.04f),
                size = androidx.compose.ui.geometry.Size(size.width*0.88f,size.height*0.92f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()))
            drawCircle(Color(0xFF8C7658), radius = size.width*0.105f,
                center = Offset(size.width*card.iconX,size.height*card.iconY))
            drawContext.canvas.nativeCanvas.apply {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    color = android.graphics.Color.WHITE
                    textSize = size.width*0.11f
                }
                drawText("✦", size.width*card.iconX, size.height*card.iconY + p.textSize*0.35f, p)
                p.color = card.titleColor.toArgb()
                p.textSize = card.titleSize * density * 0.85f
                drawText(card.title, size.width*card.titleX, size.height*card.titleY, p)
                p.color = card.tagColor.toArgb()
                p.textSize = 13f*density
                drawRoundRect(size.width*.13f,size.height*.57f,size.width*.40f,size.height*.63f,10f,10f,p)
                drawRoundRect(size.width*.45f,size.height*.57f,size.width*.72f,size.height*.63f,10f,10f,p)
                p.color = android.graphics.Color.WHITE
                p.textSize = 12f*density
                drawText(card.tagOne,size.width*.265f,size.height*.61f,p)
                drawText(card.tagTwo,size.width*.585f,size.height*.61f,p)
                p.color = card.bodyColor.toArgb()
                p.textSize = card.bodySize*density*.75f
                p.textAlign = Paint.Align.CENTER
                val lines = card.description.chunked(18).take(7)
                lines.forEachIndexed { i, line ->
                    drawText(line,size.width*card.bodyX,size.height*card.bodyY+i*p.textSize*1.55f,p)
                }
            }
        }
        Text("拖动图标、名称或介绍可调整位置；点击下方按钮编辑文字",
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
            fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ColorChoices(current: Color, onChoose: (Color) -> Unit) {
    val colors = listOf(Color(0xFF242424), Color(0xFF386A9B), Color(0xFFB84B43),
        Color(0xFF54835D), Color(0xFF8A5CA8), Color(0xFFCC8B32))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).background(c)
                .border(if(c == current) 3.dp else 1.dp, if(c == current) Color.Black else Color.LightGray,
                    RoundedCornerShape(50))
                .pointerInput(c) { detectDragGestures(onDragStart = { onChoose(c) }) }
            )
        }
    }
}
