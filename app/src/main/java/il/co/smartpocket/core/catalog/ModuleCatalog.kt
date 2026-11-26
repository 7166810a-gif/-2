package il.co.smartpocket.core.catalog

enum class ModuleStage(val label: String) {
    ACTIVE("פעיל"),
    FOUNDATION("תשתית מוכנה"),
    PLANNED("בתכנון")
}

data class MegaModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: String,
    val stage: ModuleStage
)

object MegaModuleCatalog {
    val modules = listOf(
        MegaModule("legacy", "96 כלים קיימים", "כלים פעילים כבר עכשיו", "מחשבונים, המרות, טקסט, אבטחה, זמן, QR וחיישני מכשיר.", "🧰", ModuleStage.ACTIVE),
        MegaModule("storage", "קבצים ואחסון", "אחסון פנימי, SD ו-USB", "תשתית לבחירת תיקיות וקבצים באמצעות בורר Android, עם הרשאות שנשמרות לפי בחירת המשתמש.", "📁", ModuleStage.FOUNDATION),
        MegaModule("media", "מרכז מדיה", "מוזיקה ווידאו מקומיים", "נגן מדיה, ספרייה מקומית, רשימות השמעה וכלי ניהול קובצי שמע ווידאו.", "🎬", ModuleStage.PLANNED),
        MegaModule("images", "סטודיו תמונות", "עריכה ועיבוד תמונה", "חיתוך, שינוי גודל, דחיסה, התאמות צבע ועיבוד קבוצתי.", "🖼️", ModuleStage.PLANNED),
        MegaModule("audio", "אולפן שמע", "הקלטה ועיבוד אודיו", "הקלטה, חיתוך, חיבור קטעים, נרמול ועיבוד קבצי שמע.", "🎙️", ModuleStage.PLANNED),
        MegaModule("documents", "מסמכים ו-PDF", "קוראים וכלי מסמכים", "קורא PDF, סריקה, OCR מקומי, מסמכי טקסט וכלי המרה.", "📄", ModuleStage.PLANNED),
        MegaModule("device", "מעבדת מכשיר", "חיישנים ואבחון", "מידע על חומרה, סוללה, אחסון וחיישנים הזמינים במכשיר.", "📟", ModuleStage.PLANNED),
        MegaModule("engineering", "הנדסה ומחשבונים", "חישובים וכלי תכנון", "מחשבונים מדעיים, המרות הנדסיות, גרפים וכלי מדידה.", "📐", ModuleStage.PLANNED),
        MegaModule("vault", "כספת וגיבויים", "מידע מקומי מוגן", "כספת מוצפנת, ניהול גיבויים, בדיקות שלמות והגנה על קבצים.", "🔐", ModuleStage.PLANNED),
        MegaModule("ai", "בינה מלאכותית מקומית", "ייבוא מודלים ללא רשת", "בחירת קובצי מודל מקומיים, שמירת הרשאת גישה והכנת תשתית למנוע הסקה מקומי.", "🧠", ModuleStage.FOUNDATION),
        MegaModule("developer", "מעבדת פיתוח", "קוד ואוטומציה מקומית", "עורכי טקסט וקוד, כלי JSON, Regex, SQLite ומתכוני אוטומציה מקומיים.", "💻", ModuleStage.PLANNED),
        MegaModule("creative", "מפות, יצירה ומשחקים", "כלים יצירתיים אופליין", "מפות שהורדו מראש, נקודות GPS, ציור, תרשימים ומשחקים מקומיים.", "🗺️", ModuleStage.PLANNED)
    )
}
