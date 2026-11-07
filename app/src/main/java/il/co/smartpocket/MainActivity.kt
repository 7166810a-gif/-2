package il.co.smartpocket

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.Base64
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.json.JSONTokener
import java.net.URLEncoder
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.*

private val Brand = Color(0xFF315C9B)
private val Ink = Color(0xFF17243B)
private val Muted = Color(0xFF718096)
private val Bg = Color(0xFFF3F6FB)

data class PocketTool(val id:String,val title:String,val desc:String,val cat:String,val icon:String,val mode:String)

private val categories = listOf("הכול","מתמטיקה","המרות","טקסט","אבטחה","זמן ותכנון","מכשיר ויצירה")
private val rawTools = """
calculator|מחשבון מדעי|ביטויים עם סוגריים, חזקה ופעולות חשבון|מתמטיקה|🧮|calc
percent|מחשבון אחוזים|אחוז מתוך מספר|מתמטיקה|％|math
discount|מחשבון הנחות|מחיר לאחר הנחה|מתמטיקה|🏷️|math
vat|מחשבון מס|הוספה או חילוץ אחוז מס|מתמטיקה|🧾|math
tip|מחשבון טיפ|חישוב טיפ וחשבון כולל|מתמטיקה|🍽️|math
split|חלוקת חשבון|חלוקת סכום בין אנשים|מתמטיקה|👥|math
average|ממוצע|ממוצע של רשימת מספרים|מתמטיקה|📊|math
median|חציון|הערך האמצעי ברשימה|מתמטיקה|📈|math
stdev|סטיית תקן|מדידת פיזור מספרים|מתמטיקה|〽️|math
minmax|מינימום ומקסימום|מציאת ערכים קיצוניים|מתמטיקה|↕️|math
ratio|כלל שלוש|פתרון יחס ישר משלושה ערכים|מתמטיקה|⚖️|math
fraction|שבר לעשרוני|המרת שבר למספר עשרוני|מתמטיקה|➗|math
roman|ספרות רומיות|המרת מספר לספרות רומיות|מתמטיקה|Ⅻ|math
numberbase|בסיסי מספרים|בינארי, אוקטלי והקסדצימלי|מתמטיקה|🔢|math
length|ממיר אורך|מטרים, ס״מ, רגל ואינץ׳|המרות|📏|convert
weight|ממיר משקל|ק״ג, גרם, פאונד ואונקיה|המרות|⚖️|convert
temperature|ממיר טמפרטורה|צלזיוס, פרנהייט וקלווין|המרות|🌡️|convert
area|ממיר שטח|מטר רבוע, רגל רבוע ואקר|המרות|◻️|convert
volume|ממיר נפח|ליטרים, מ״ל וגלונים|המרות|🧪|convert
speed|ממיר מהירות|קמ״ש, מטר לשנייה ומייל לשעה|המרות|🚀|convert
datasize|ממיר נפח נתונים|MB, KB, GB ו-Bytes|המרות|💾|convert
timeunits|ממיר זמן|שניות, דקות, שעות וימים|המרות|⏱️|convert
pressure|ממיר לחץ|bar, PSI ו-Pascal|המרות|🧭|convert
energy|ממיר אנרגיה|Joule, קלוריות ו-kWh|המרות|⚡|convert
power|ממיר הספק|וואט, קילוואט וכוח סוס|המרות|🔌|convert
cooking|מידות מטבח|כפיות, כפות וכוסות מדידה|המרות|🥄|convert
pixels|פיקסלים ו-DP|התאמת יחידות למסכי Android|המרות|📱|convert
textcount|מונה טקסט|ספירת תווים, מילים ושורות|טקסט|🔤|text
uppercase|אותיות גדולות|המרת אותיות לאותיות גדולות|טקסט|🔠|text
lowercase|אותיות קטנות|המרת אותיות לאותיות קטנות|טקסט|🔡|text
reverse|היפוך טקסט|הפיכת סדר התווים|טקסט|↔️|text
spaces|ניקוי רווחים|הסרת רווחים מיותרים|טקסט|🧹|text
sortlines|מיון שורות|מיון שורות לפי א״ב|טקסט|📚|text
unique|הסרת כפילויות|השארת שורות ייחודיות|טקסט|🧬|text
linecount|מונה שורות|ספירת מילים בכל שורה|טקסט|📝|text
compare|השוואת טקסט|השוואת שני קטעים|טקסט|🔍|text
json|מעצב JSON|עיצוב JSON לקריאה|טקסט|{ }|text
base64enc|Base64 קידוד|קידוד טקסט ל-Base64|טקסט|🔐|text
base64dec|Base64 פענוח|פענוח טקסט Base64|טקסט|🔓|text
urlenc|קידוד כתובת|קידוד טקסט ל-URL|טקסט|🌐|text
urldec|פענוח כתובת|פענוח URL מקודד|טקסט|🔗|text
slug|יוצר Slug|יצירת מזהה URL מכותרת|טקסט|🔗|text
frequency|תדירות מילים|ספירת הופעות מילים|טקסט|📊|text
shuffle|ערבוב רשימה|ערבוב שורות אקראי|טקסט|🔀|text
binary|טקסט לבינארי|הצגת טקסט בבינארי UTF-8|טקסט|💻|text
unbinary|בינארי לטקסט|פענוח בינארי ל-UTF-8|טקסט|🧩|text
wordwrap|עטיפת שורות|חלוקת טקסט לאורך קבוע|טקסט|↩️|text
trimlines|ניקוי שורות|הסרת רווחים מסביב לשורה|טקסט|🧽|text
duplicate|שכפול טקסט|שכפול טקסט מספר פעמים|טקסט|📋|text
morse|קוד מורס|המרת טקסט לאותות מורס|טקסט|📡|text
sha256|SHA-256|חישוב Hash לטקסט מקומי|אבטחה|#️⃣|secure
password|מחולל סיסמאות|סיסמה אקראית חזקה|אבטחה|🔑|secure
passcheck|בודק סיסמה|בדיקת חוזק ואורך|אבטחה|🛡️|secure
uuid|מחולל UUID|מזהה אקראי ייחודי|אבטחה|🆔|secure
random|מספר אקראי|הגרלה בטווח שתבחר|אבטחה|🎲|random
dice|הטלת קוביות|מספר קוביות ומספר צדדים|אבטחה|🎲|random
coin|הטלת מטבע|עץ או פלי|אבטחה|🪙|random
passphrase|צירוף מילים|צירוף אקראי שקל לזכור|אבטחה|🗝️|secure
datediff|הפרש תאריכים|מספר הימים בין תאריכים|זמן ותכנון|🗓️|date
adddays|הוספת ימים|חישוב תאריך קדימה או אחורה|זמן ותכנון|📆|date
weekday|יום בשבוע|יום השבוע לפי תאריך|זמן ותכנון|📅|date
leap|שנה מעוברת|בדיקה אם שנה מעוברת|זמן ותכנון|🗓️|date
yearday|יום בשנה|מספר היום בשנה|זמן ותכנון|🔢|date
unixtime|חותמת זמן|המרת תאריך ל-Unix ולהפך|זמן ותכנון|⌛|date
stopwatch|שעון עצר|מדידת זמן והשהיה|זמן ותכנון|⏱️|timer
timer|טיימר|ספירה לאחור בדקות|זמן ותכנון|⏳|timer
pomodoro|פומודורו|מחזורי עבודה והפסקה|זמן ותכנון|🍅|timer
notes|פתקים|שמירת פתקים במכשיר|זמן ותכנון|📒|notes
checklist|רשימת משימות|רשימה אישית שנשמרת|זמן ותכנון|✅|notes
tally|מונה לחיצות|מונה פלוס ומינוס|זמן ותכנון|🔢|counter
metronome|מטרונום|טיימר קצב לפי BPM|זמן ותכנון|🎵|timer
qr|יוצר QR|יצירת קוד QR מתוכן|מכשיר ויצירה|▦|qr
barcode|יוצר ברקוד|יצירת ברקוד Code 128|מכשיר ויצירה|▥|qr
device|מידע על המכשיר|דגם, יצרן וגרסת Android|מכשיר ויצירה|📲|device
battery|מצב סוללה|רמת טעינה ומצב סוללה|מכשיר ויצירה|🔋|device
storage|אחסון פנוי|שטח פנוי באחסון המכשיר|מכשיר ויצירה|💽|device
sensors|רשימת חיישנים|חיישנים זמינים במכשיר|מכשיר ויצירה|🧭|device
compass|מצפן|כיוון לפי חיישני המכשיר|מכשיר ויצירה|🧭|sensor
level|פלס דיגיטלי|זווית לפי מד תאוצה|מכשיר ויצירה|📐|sensor
color|ממיר RGB ל-HEX|המרת רכיבי צבע לקוד HEX|מכשיר ויצירה|🎨|misc
aspect|יחס תמונה|חישוב יחס רוחב וגובה|מכשיר ויצירה|🖼️|misc
dp|ממיר DP ל-PX|המרת יחידות תצוגה|המרות|📲|convert
romanread|בדיקת מספר רומי|המרת ספרות רומיות למספר|מתמטיקה|Ⅳ|math
countdown|ספירה לאחור|טיימר מהיר עם איפוס|זמן ותכנון|⌛|timer
"""
private val allTools = rawTools.trim().lines().map { line ->
    val p = line.split("|")
    PocketTool(p[0],p[1],p[2],p[3],p[4],p[5])
}

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=android.graphics.Color.rgb(243,246,251)
        window.navigationBarColor=android.graphics.Color.rgb(243,246,251)
        window.decorView.systemUiVisibility=android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { SmartPocketApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartPocketApp() {
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("smart_pocket",Context.MODE_PRIVATE) }
    var selected by remember { mutableStateOf<PocketTool?>(null) }
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("הכול") }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites",emptySet()) ?: emptySet()) }
    var favoritesOnly by remember { mutableStateOf(false) }
    val visible=remember(query,group,favorites,favoritesOnly) {
        allTools.filter { t -> (group=="הכול" || t.cat==group) && (!favoritesOnly || t.id in favorites) && (query.isBlank() || t.title.contains(query,true) || t.desc.contains(query,true) || t.cat.contains(query,true)) }
    }
    MaterialTheme(colorScheme=lightColorScheme(primary=Brand,secondary=Color(0xFF16856B),background=Bg,surface=Color.White,onSurface=Ink)) {
        if(selected!=null) {
            PocketToolScreen(
                tool=selected!!,
                favorite=selected!!.id in favorites,
                onFavorite={
                    val next=favorites.toMutableSet()
                    if(!next.add(selected!!.id)) next.remove(selected!!.id)
                    favorites=next
                    prefs.edit().putStringSet("favorites",next).apply()
                },
                onBack={selected=null}
            )
        } else {
            Scaffold(
                containerColor=Bg,
                topBar={
                    TopAppBar(
                        title={Column { Text("כיס חכם",fontWeight=FontWeight.ExtraBold,color=Ink);Text("ארגז כלים אישי • עובד ללא אינטרנט",style=MaterialTheme.typography.labelSmall,color=Muted) }},
                        actions={IconButton(onClick={favoritesOnly=!favoritesOnly}){Text(if(favoritesOnly)"★" else "☆",fontSize=24.sp,color=Brand)}},
                        colors=TopAppBarDefaults.topAppBarColors(containerColor=Bg)
                    )
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Column(Modifier.padding(horizontal=16.dp)) {
                        Surface(color=Color(0xFF244C83),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Text("הכוח שלך. בכיס שלך.",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)
                                Text("יותר מ-70 כלים שימושיים. בלי חשבון, בלי שרת ובלי רשת.",color=Color.White.copy(alpha=.88f))
                                Text("✓ אופליין מלא     ✓ נתונים מקומיים",color=Color.White,style=MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("חיפוש כלי...")},singleLine=true,shape=RoundedCornerShape(16.dp))
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            categories.forEach { g -> FilterChip(selected=group==g,onClick={group=g;favoritesOnly=false},label={Text(g)}) }
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text(if(favoritesOnly)"מועדפים" else "כל הכלים",fontWeight=FontWeight.Bold,color=Ink)
                            Text(visible.size.toString()+" כלים",color=Muted,style=MaterialTheme.typography.labelMedium)
                        }
                    }
                    LazyVerticalGrid(
                        columns=GridCells.Adaptive(150.dp),
                        contentPadding=PaddingValues(start=16.dp,end=16.dp,top=4.dp,bottom=24.dp),
                        horizontalArrangement=Arrangement.spacedBy(10.dp),
                        verticalArrangement=Arrangement.spacedBy(10.dp)
                    ) {
                        items(visible,key={it.id}) { tool ->
                            Surface(modifier=Modifier.fillMaxWidth().height(132.dp).clickable{selected=tool},shape=RoundedCornerShape(18.dp),color=Color.White,shadowElevation=1.dp) {
                                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                        Text(tool.icon,fontSize=24.sp)
                                        Spacer(Modifier.weight(1f))
                                        if(tool.id in favorites) Text("★",color=Color(0xFFE39A38))
                                    }
                                    Text(tool.title,color=Ink,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    Text(tool.desc,color=Muted,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PocketToolScreen(tool:PocketTool,favorite:Boolean,onFavorite:()->Unit,onBack:()->Unit) {
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("smart_pocket",Context.MODE_PRIVATE) }
    var input by remember(tool.id) { mutableStateOf(if(tool.mode=="notes")prefs.getString("note_"+tool.id,"")?:"" else "") }
    var second by remember(tool.id) { mutableStateOf("") }
    var result by remember(tool.id) { mutableStateOf("") }
    var qr by remember(tool.id) { mutableStateOf<Bitmap?>(null) }
    var running by remember(tool.id) { mutableStateOf(false) }
    var elapsed by remember(tool.id) { mutableLongStateOf(0L) }
    var tally by remember(tool.id) { mutableIntStateOf(0) }
    var sensorValue by remember(tool.id) { mutableFloatStateOf(0f) }

    if(tool.mode=="timer" && running) LaunchedEffect(running,tool.id) { while(running){kotlinx.coroutines.delay(1000);elapsed++} }
    if(tool.mode=="sensor") DisposableEffect(tool.id) {
        val manager=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor=manager.getDefaultSensor(if(tool.id=="compass")Sensor.TYPE_ROTATION_VECTOR else Sensor.TYPE_ACCELEROMETER)
        val listener=object:SensorEventListener {
            override fun onSensorChanged(e:SensorEvent) {
                sensorValue=if(tool.id=="compass") {
                    val matrix=FloatArray(9);val orientation=FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(matrix,e.values)
                    SensorManager.getOrientation(matrix,orientation)
                    (Math.toDegrees(orientation[0].toDouble()).toFloat()+360f)%360f
                } else Math.toDegrees(atan2(e.values[0].toDouble(),e.values[1].toDouble())).toFloat()
            }
            override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
        }
        if(sensor!=null)manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI)
        onDispose{manager.unregisterListener(listener)}
    }

    Scaffold(
        containerColor=Bg,
        topBar={
            TopAppBar(
                title={Column {Text(tool.title,color=Ink,fontWeight=FontWeight.Bold);Text(tool.cat,color=Muted,style=MaterialTheme.typography.labelSmall)}},
                navigationIcon={IconButton(onClick=onBack){Text("‹",fontSize=30.sp,color=Ink)}},
                actions={IconButton(onClick=onFavorite){Text(if(favorite)"★" else "☆",fontSize=24.sp,color=Color(0xFFE39A38))}},
                colors=TopAppBarDefaults.topAppBarColors(containerColor=Bg)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Surface(color=Color.White,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(tool.icon,fontSize=32.sp);Text(tool.desc,color=Muted)}
            }
            when(tool.mode) {
                "timer" -> {
                    val target=if(tool.id=="stopwatch")Long.MAX_VALUE else (input.toLongOrNull()?:1L)*60L
                    val remaining=if(tool.id=="stopwatch")elapsed else (target-elapsed).coerceAtLeast(0)
                    Text(clock(remaining),modifier=Modifier.fillMaxWidth(),fontSize=44.sp,fontWeight=FontWeight.Bold,color=Ink,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    if(tool.id!="stopwatch")OutlinedTextField(value=input,onValueChange={input=it.filter(Char::isDigit);elapsed=0;running=false},label={Text("משך בדקות")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
                    if(tool.id=="pomodoro")Text("הזן 25 דקות לעבודה או 5 דקות להפסקה.",color=Muted)
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()) {
                        Button(onClick={running=!running},modifier=Modifier.weight(1f),enabled=tool.id=="stopwatch"||remaining>0){Text(if(running)"השהה" else "התחל")}
                        OutlinedButton(onClick={running=false;elapsed=0},modifier=Modifier.weight(1f)){Text("איפוס")}
                    }
                }
                "counter" -> {
                    Text(tally.toString(),modifier=Modifier.fillMaxWidth(),fontSize=64.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=Ink)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick={tally--},modifier=Modifier.weight(1f)){Text("−")}
                        Button(onClick={tally++},modifier=Modifier.weight(1f)){Text("+")}
                        OutlinedButton(onClick={tally=0}){Text("איפוס")}
                    }
                }
                "sensor" -> {
                    Text(if(tool.id=="compass")"כיוון: "+sensorValue.roundToInt()+"°" else "זווית: "+sensorValue.roundToInt()+"°",fontSize=32.sp,color=Brand,fontWeight=FontWeight.Bold)
                    Text("הנתון מגיע מחיישני Android המקומיים.",color=Muted)
                }
                "notes" -> {
                    OutlinedTextField(value=input,onValueChange={input=it;prefs.edit().putString("note_"+tool.id,it).apply()},modifier=Modifier.fillMaxWidth().heightIn(min=200.dp),label={Text(if(tool.id=="checklist")"משימות – שורה לכל פריט" else "כתוב פתק")},minLines=7)
                    Text("נשמר אוטומטית במכשיר.",color=Color(0xFF16856B))
                }
                "device" -> {
                    val info=when(tool.id) {
                        "device"->"יצרן: "+Build.MANUFACTURER+"\nדגם: "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+"\nAPI: "+Build.VERSION.SDK_INT+"\nמכשיר: "+Build.DEVICE
                        "battery"->batteryInfo(context)
                        "storage"->storageInfo()
                        else->sensorInfo(context)
                    }
                    ResultCard(info){copyText(context,info)}
                }
                "qr" -> {
                    OutlinedTextField(value=input,onValueChange={input=it;qr=null},modifier=Modifier.fillMaxWidth(),label={Text("הזן טקסט או כתובת")},minLines=2)
                    Button(onClick={
                        runCatching {
                            val format=if(tool.id=="barcode")BarcodeFormat.CODE_128 else BarcodeFormat.QR_CODE
                            val matrix=MultiFormatWriter().encode(input,format,if(tool.id=="barcode")640 else 512,if(tool.id=="barcode")220 else 512)
                            val bitmap=Bitmap.createBitmap(matrix.width,matrix.height,Bitmap.Config.ARGB_8888)
                            for(x in 0 until matrix.width)for(y in 0 until matrix.height)bitmap.setPixel(x,y,if(matrix[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                            qr=bitmap
                        }.onFailure{result=it.message?:"שגיאה ביצירת קוד"}
                    },modifier=Modifier.fillMaxWidth(),enabled=input.isNotBlank()){Text("צור קוד")}
                    qr?.let{Image(it.asImageBitmap(),contentDescription="קוד שנוצר",modifier=Modifier.fillMaxWidth().heightIn(max=320.dp).padding(12.dp))}
                    if(result.isNotBlank())Text(result,color=Color.Red)
                }
                else -> {
                    OutlinedTextField(value=input,onValueChange={input=it;result=""},modifier=Modifier.fillMaxWidth(),label={Text(inputLabel(tool))},placeholder={Text(inputHint(tool))},minLines=if(tool.mode=="text"||tool.mode=="math")3 else 1)
                    if(secondNeeded(tool))OutlinedTextField(value=second,onValueChange={second=it;result=""},modifier=Modifier.fillMaxWidth(),label={Text(secondLabel(tool))},minLines=if(tool.id=="compare")3 else 1)
                    Button(onClick={result=runCatching{execute(tool,input,second,context)}.getOrElse{"שגיאה: "+(it.message?:"בדוק את הנתונים")}},modifier=Modifier.fillMaxWidth(),enabled=input.isNotBlank()){Text("בצע")}
                    if(result.isNotBlank())ResultCard(result){copyText(context,result);android.widget.Toast.makeText(context,"הועתק ללוח",android.widget.Toast.LENGTH_SHORT).show()}
                }
            }
            Text("עיבוד מקומי במכשיר בלבד.",modifier=Modifier.align(Alignment.CenterHorizontally),color=Muted,style=MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ResultCard(value:String,onCopy:()->Unit) {
    Surface(color=Color.White,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("תוצאה",color=Muted,style=MaterialTheme.typography.labelMedium)
            Text(value,color=Ink)
            TextButton(onClick=onCopy,modifier=Modifier.align(Alignment.End)){Text("העתק")}
        }
    }
}
private fun secondNeeded(t:PocketTool)=t.id in setOf("percent","discount","vat","tip","split","ratio","compare","datediff","adddays","random","dice","pixels","dp")
private fun inputLabel(t:PocketTool)=when(t.id) {
    "calculator"->"ביטוי מתמטי";"fraction"->"שבר, למשל 3/4";"average","median","stdev","minmax"->"מספרים מופרדים בפסיקים"
    "datediff","weekday","yearday","adddays"->"תאריך YYYY-MM-DD";"leap"->"שנה";"ratio"->"שלושה ערכים a,b,c"
    "random","dice"->"גבול תחתון או מספר קוביות";"numberbase","roman","romanread"->"מספר";"color"->"ערכי RGB, לדוגמה 255,120,20";"aspect"->"רוחבxגובה";else->"הזן טקסט או מספר"
}
private fun inputHint(t:PocketTool)=when(t.id) {
    "calculator"->"(12+8)*3/2";"average","median","stdev","minmax"->"12, 8, 15, 20";"datediff","weekday","yearday","adddays"->"2026-11-08";"fraction"->"7/8";"ratio"->"4,10,8";"aspect"->"1920x1080";else->"כתוב או הדבק כאן..."
}
private fun secondLabel(t:PocketTool)=when(t.id) {
    "percent"->"אחוז";"discount","vat","tip"->"אחוז";"split"->"מספר אנשים";"compare"->"טקסט שני";"datediff"->"תאריך שני YYYY-MM-DD";"adddays"->"מספר ימים להוספה";"random"->"גבול עליון";"dice"->"מספר צדדים";"pixels","dp"->"DPI";else->"ערך שני"
}

private fun execute(t:PocketTool,a:String,b:String,c:Context):String {
    val x=a.trim();val y=b.trim();val n=x.toDoubleOrNull();val m=y.toDoubleOrNull();val rnd=SecureRandom()
    return when(t.id) {
        "calculator"->Expr(x).parse().fmt()
        "percent"->((n?:error("הזן מספר"))*(m?:error("הזן אחוז"))/100).fmt()
        "discount"->{val v=n?:error("מחיר");val r=m?:error("אחוז");"מחיר לאחר הנחה: "+(v*(1-r/100)).fmt()+"\nחיסכון: "+(v*r/100).fmt()}
        "vat"->{val v=n?:error("סכום");val r=m?:error("אחוז");"כולל מס: "+(v*(1+r/100)).fmt()+"\nמס: "+(v*r/100).fmt()}
        "tip"->{val v=n?:error("סכום");val r=m?:error("אחוז");"טיפ: "+(v*r/100).fmt()+"\nסה״כ: "+(v*(1+r/100)).fmt()}
        "split"->{val v=n?:error("סכום");val p=(m?:error("מספר אנשים")).toInt();require(p>0); "כל אחד: "+(v/p).fmt()}
        "average","median","stdev","minmax"->{
            val list=x.split(Regex("[,;\\s]+")).filter{it.isNotBlank()}.map{it.toDoubleOrNull()?:error("ערך אינו מספר")};require(list.isNotEmpty())
            when(t.id) {
                "average"->"ממוצע: "+list.average().fmt()
                "median"->{val s=list.sorted();val i=s.size/2;"חציון: "+(if(s.size%2==0)(s[i-1]+s[i])/2 else s[i]).fmt()}
                "stdev"->{val avg=list.average();"סטיית תקן: "+sqrt(list.sumOf{(it-avg).pow(2)}/list.size).fmt()}
                else->"מינימום: "+list.minOrNull()!!.fmt()+"\nמקסימום: "+list.maxOrNull()!!.fmt()
            }
        }
        "ratio"->{val p=x.split(",", ";").mapNotNull{it.trim().toDoubleOrNull()};val a1=p.getOrNull(0)?:error("הזן a,b,c");val a2=p.getOrNull(1)?:error("הזן a,b,c");val a3=p.getOrNull(2)?:m?:error("הזן ערך שלישי");require(a1!=0.0);(a2*a3/a1).fmt()}
        "fraction"->{val p=x.split("/");require(p.size==2);val d=p[1].trim().toDouble();require(d!=0.0);(p[0].trim().toDouble()/d).fmt()}
        "roman"->{val v=x.toInt();require(v in 1..3999);toRoman(v)}
        "romanread"->romanToInt(x).toString()
        "numberbase"->{val v=x.toLong();"בינארי: "+v.toString(2)+"\nאוקטלי: "+v.toString(8)+"\nHEX: "+v.toString(16).uppercase()}
        "length"->{val v=n?:error("מטרים");"ס״מ: "+(v*100).fmt()+"\nמ״מ: "+(v*1000).fmt()+"\nק״מ: "+(v/1000).fmt()+"\nרגל: "+(v*3.28084).fmt()+"\nאינץ׳: "+(v*39.3701).fmt()}
        "weight"->{val v=n?:error("ק״ג");"גרם: "+(v*1000).fmt()+"\nפאונד: "+(v*2.20462).fmt()+"\nאונקיות: "+(v*35.274).fmt()}
        "temperature"->{val v=n?:error("צלזיוס");"פרנהייט: "+(v*9/5+32).fmt()+"\nקלווין: "+(v+273.15).fmt()}
        "area"->{val v=n?:error("מ״ר");"סמ״ר: "+(v*10000).fmt()+"\nרגל רבוע: "+(v*10.7639).fmt()+"\nאקר: "+(v/4046.856).fmt()}
        "volume"->{val v=n?:error("ליטר");"מ״ל: "+(v*1000).fmt()+"\nגלון: "+(v/3.78541).fmt()+"\nכוסות: "+(v*4.22675).fmt()}
        "speed"->{val v=n?:error("קמ״ש");"מטר לשנייה: "+(v/3.6).fmt()+"\nמייל לשעה: "+(v/1.60934).fmt()+"\nקשר: "+(v/1.852).fmt()}
        "datasize"->{val v=n?:error("MB");"Bytes: "+(v*1_000_000).fmt()+"\nKB: "+(v*1000).fmt()+"\nGB: "+(v/1000).fmt()+"\nGiB: "+(v/1024).fmt()}
        "timeunits"->{val v=n?:error("שניות");"דקות: "+(v/60).fmt()+"\nשעות: "+(v/3600).fmt()+"\nימים: "+(v/86400).fmt()}
        "pressure"->{val v=n?:error("bar");"PSI: "+(v*14.5038).fmt()+"\nPascal: "+(v*100000).fmt()+"\nאטמוספרות: "+(v/1.01325).fmt()}
        "energy"->{val v=n?:error("Joule");"קלוריות: "+(v/4.184).fmt()+"\nkWh: "+(v/3600000).fmt()+"\nWh: "+(v/3600).fmt()}
        "power"->{val v=n?:error("W");"kW: "+(v/1000).fmt()+"\nכוח סוס: "+(v/745.7).fmt()}
        "cooking"->{val v=n?:error("מ״ל");"כפיות: "+(v/5).fmt()+"\nכפות: "+(v/15).fmt()+"\nכוסות: "+(v/240).fmt()}
        "pixels","dp"->{val px=n?:error("פיקסלים");val dpi=m?:c.resources.displayMetrics.densityDpi.toDouble();require(dpi>0); "DP: "+(px*160/dpi).fmt()+" לפי "+dpi+" DPI"}
        "textcount"->"תווים: "+x.length+"\nללא רווחים: "+x.count{!it.isWhitespace()}+"\nמילים: "+x.trim().split(Regex("\\s+")).count{it.isNotEmpty()}+"\nשורות: "+x.lines().size+"\nBytes UTF-8: "+x.toByteArray().size
        "uppercase"->x.uppercase(Locale.ROOT)
        "lowercase"->x.lowercase(Locale.ROOT)
        "reverse"->x.reversed()
        "spaces"->x.trim().replace(Regex("\\s+")," ")
        "sortlines"->x.lines().sorted().joinToString("\n")
        "unique"->x.lines().distinct().joinToString("\n")
        "linecount"->x.lines().mapIndexed{i,s->(i+1).toString()+". "+s.length+" תווים / "+s.trim().split(Regex("\\s+")).count{it.isNotEmpty()}+" מילים"}.joinToString("\n")
        "compare"->{val left=x.lines();val right=y.lines();(0 until maxOf(left.size,right.size)).joinToString("\n"){i->val l=left.getOrNull(i)?: "∅";val r=right.getOrNull(i)?: "∅";(i+1).toString()+": "+(if(l==r)"זהה" else "שונה")+" | "+l+" | "+r}}
        "json"->JSONTokener(x).nextValue().let{when(it){is org.json.JSONObject->it.toString(2);is org.json.JSONArray->it.toString(2);else->it.toString()}}
        "base64enc"->Base64.encodeToString(x.toByteArray(Charsets.UTF_8),Base64.NO_WRAP)
        "base64dec"->String(Base64.decode(x,Base64.DEFAULT),Charsets.UTF_8)
        "urlenc"->URLEncoder.encode(x,"UTF-8")
        "urldec"->URLDecoder.decode(x,"UTF-8")
        "slug"->x.lowercase(Locale.ROOT).trim().replace(Regex("[^a-z0-9\\u0590-\\u05FF]+"),"-").trim('-')
        "frequency"->Regex("[\\p{L}\\p{N}']+").findAll(x.lowercase()).groupingBy{it.value}.eachCount().entries.sortedByDescending{it.value}.joinToString("\n"){it.key+": "+it.value}
        "shuffle"->x.lines().shuffled(rnd).joinToString("\n")
        "binary"->x.toByteArray(Charsets.UTF_8).joinToString(" "){(it.toInt() and 255).toString(2).padStart(8,'0')}
        "unbinary"->String(x.split(Regex("\\s+")).filter{it.isNotBlank()}.map{it.toInt(2).toByte()}.toByteArray(),Charsets.UTF_8)
        "wordwrap"->{val width=y.toIntOrNull()?.coerceIn(10,200)?:40;x.chunked(width).joinToString("\n")}
        "trimlines"->x.lines().joinToString("\n"){it.trim()}
        "duplicate"->{val times=y.toIntOrNull()?.coerceIn(1,100)?:2;List(times){x}.joinToString("\n")}
        "morse"->toMorse(x)
        "sha256"->MessageDigest.getInstance("SHA-256").digest(x.toByteArray()).joinToString(""){"%02x".format(it)}
        "password"->{val len=x.toIntOrNull()?.coerceIn(8,64)?:20;val chars="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%&*+-_";buildString{repeat(len){append(chars[rnd.nextInt(chars.length)])}}}
        "passcheck"->{val score=listOf(x.length>=12,x.any{it.isLowerCase()},x.any{it.isUpperCase()},x.any{it.isDigit()},x.any{!it.isLetterOrDigit()}).count{it};"חוזק: "+(when{score<=2->"חלשה";score<=4->"בינונית";else->"חזקה"})+" ("+score+"/5)\nאורך: "+x.length+" תווים"}
        "uuid"->UUID.randomUUID().toString()
        "passphrase"->{val words=listOf("ענן","כוכב","נהר","אור","אבן","יער","רוח","ים","שקד","ברק","חלון","שמש","מפתח","ספר","דרך","גשר","פרח","ירח","גל","צוק");List(x.toIntOrNull()?.coerceIn(3,8)?:4){words[rnd.nextInt(words.size)]}.joinToString("-")}
        "random"->{val low=n?.toInt()?:error("גבול תחתון");val high=m?.toInt()?:error("גבול עליון");require(high>=low);rnd.nextInt(low,high+1).toString()}
        "dice"->{val count=(n?:1.0).toInt().coerceIn(1,100);val sides=(m?:6.0).toInt().coerceIn(2,1000);val rolls=List(count){rnd.nextInt(sides)+1};"תוצאות: "+rolls.joinToString(", ")+"\nסכום: "+rolls.sum()}
        "coin"->if(rnd.nextBoolean())"עץ" else "פלי"
        "datediff"->{val d1=parseDate(x);val d2=parseDate(y);"הפרש: "+abs((d2.time-d1.time)/86400000L)+" ימים"}
        "adddays"->{val d=parseDate(x);Date(d.time+(y.toLongOrNull()?:error("מספר ימים"))*86400000L).formatLocal()}
        "weekday"->SimpleDateFormat("EEEE",Locale("he","IL")).format(parseDate(x))
        "leap"->{val year=x.toInt();if(year%400==0||(year%4==0&&year%100!=0))"השנה מעוברת" else "השנה אינה מעוברת"}
        "yearday"->"יום מספר "+SimpleDateFormat("D",Locale.ROOT).format(parseDate(x))+" בשנה"
        "unixtime"->{val v=x.toLongOrNull();if(v!=null)Date(if(v<10000000000L)v*1000 else v).formatLocal() else (parseDate(x).time/1000).toString()}
        "color"->{val c=x.split(",", ";").map{it.trim().toInt()};require(c.size==3&&c.all{it in 0..255});"#%02X%02X%02X".format(c[0],c[1],c[2])}
        "aspect"->{val p=x.lowercase().replace("×","x").split("x",",").map{it.trim().toInt()};require(p.size==2&&p.all{it>0});val g=gcd(p[0],p[1]);(p[0]/g).toString()+":"+(p[1]/g)}
        else->error("פעולה לא מוכרת")
    }
}

private fun parseDate(s:String):Date{val f=SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);f.isLenient=false;return f.parse(s)?:error("תאריך לא תקין YYYY-MM-DD")}
private fun Date.formatLocal()=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT).format(this)
private fun Double.fmt():String{require(isFinite());return if(abs(this-roundToLong())<1e-7&&abs(this)<9e15)roundToLong().toString() else String.format(Locale.ROOT,"%.6f",this).trimEnd('0').trimEnd('.')}
private fun gcd(a:Int,b:Int):Int=if(b==0)abs(a) else gcd(b,a%b)
private fun toRoman(n0:Int):String{var n=n0;val items=listOf(1000 to "M",900 to "CM",500 to "D",400 to "CD",100 to "C",90 to "XC",50 to "L",40 to "XL",10 to "X",9 to "IX",5 to "V",4 to "IV",1 to "I");return buildString{items.forEach{(v,s)->while(n>=v){append(s);n-=v}}}}
private fun romanToInt(s:String):Int{val m=mapOf('I' to 1,'V' to 5,'X' to 10,'L' to 50,'C' to 100,'D' to 500,'M' to 1000);var total=0;for(i in s.indices){val v=m[s[i].uppercaseChar()]?:error("תו רומי לא תקין");total+=if(i+1<s.length&&v<(m[s[i+1].uppercaseChar()]?:0))-v else v};return total}
private fun clock(s:Long)="%02d:%02d:%02d".format(s/3600,(s%3600)/60,s%60)
private fun batteryInfo(c:Context):String{val i=c.registerReceiver(null,android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));val l=i?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)?:-1;val scale=i?.getIntExtra(BatteryManager.EXTRA_SCALE,100)?:100;val status=i?.getIntExtra(BatteryManager.EXTRA_STATUS,-1)?:-1;val state=when(status){BatteryManager.BATTERY_STATUS_CHARGING->"בטעינה";BatteryManager.BATTERY_STATUS_FULL->"מלאה";BatteryManager.BATTERY_STATUS_DISCHARGING->"לא בטעינה";else->"לא ידוע"};return "טעינה: "+(if(l>=0)l*100/scale else -1)+"%\nמצב: "+state}
private fun storageInfo():String{val s=StatFs(Environment.getDataDirectory().path);val total=s.totalBytes/1024/1024/1024;val free=s.availableBytes/1024/1024/1024;return "נפח כולל: "+total+" GB\nפנוי: "+free+" GB\nבשימוש: "+(total-free)+" GB"}
private fun sensorInfo(c:Context):String{val s=(c.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getSensorList(Sensor.TYPE_ALL);return if(s.isEmpty())"לא נמצאו חיישנים" else s.joinToString("\n"){"• "+it.name+" ("+it.vendor+")"}}
private fun copyText(c:Context,s:String){(c.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("כיס חכם",s))}
private fun toMorse(text:String):String{val m=mapOf('A' to ".-",'B' to "-...",'C' to "-.-.",'D' to "-..",'E' to ".",'F' to "..-.",'G' to "--.",'H' to "....",'I' to "..",'J' to ".---",'K' to "-.-",'L' to ".-..",'M' to "--",'N' to "-.",'O' to "---",'P' to ".--.",'Q' to "--.-",'R' to ".-.",'S' to "...",'T' to "-",'U' to "..-",'V' to "...-",'W' to ".--",'X' to "-..-",'Y' to "-.--",'Z' to "--..",'0' to "-----",'1' to ".----",'2' to "..---",'3' to "...--",'4' to "....-",'5' to ".....",'6' to "-....",'7' to "--...",'8' to "---..",'9' to "----.");return text.uppercase().map{m[it]?:if(it.isWhitespace())"/" else "?"}.joinToString(" ")}

private class Expr(private val s:String){
    private var p=0
    fun parse():Double{val v=expression();skip();require(p==s.length){"ביטוי לא תקין"};return v}
    private fun skip(){while(p<s.length&&s[p].isWhitespace())p++}
    private fun take(c:Char):Boolean{skip();if(p<s.length&&s[p]==c){p++;return true};return false}
    private fun expression():Double{var v=term();while(true){v=when{take('+')->v+term();take('-')->v-term();else->return v}}}
    private fun term():Double{var v=power();while(true){v=when{take('*')->v*power();take('/')->{val d=power();require(d!=0.0);v/d};take('%')->{val d=power();require(d!=0.0);v%d};else->return v}}}
    private fun power():Double{var v=unary();if(take('^'))v=v.pow(power());return v}
    private fun unary():Double{if(take('+'))return unary();if(take('-'))return -unary();return atom()}
    private fun atom():Double{if(take('(')){val v=expression();require(take(')')){"חסר סוגר"};return v};skip();val st=p;while(p<s.length&&(s[p].isDigit()||s[p]=='.'))p++;require(p>st){"חסר מספר"};return s.substring(st,p).toDouble()}
}
