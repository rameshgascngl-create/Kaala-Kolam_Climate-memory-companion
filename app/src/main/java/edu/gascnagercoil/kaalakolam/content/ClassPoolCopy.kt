package edu.gascnagercoil.kaalakolam.content

data class ClassPlanOptionContent(
    val id: String,
    val title: LocalizedContent,
)

object ClassPoolCopy {
    val title = LocalizedContent("Class pool", "வகுப்புத் தொகுப்பு")
    val intro = LocalizedContent(
        "Everyone shares a code. One person pastes all the codes here. No server is needed: send codes by WhatsApp, Bluetooth or on paper.",
        "ஒவ்வொருவரும் ஒரு குறியீட்டைப் பகிர்கிறார்கள். ஒருவர் அனைத்தையும் இங்கே ஒட்டுகிறார். சேவையகம் தேவையில்லை: வாட்ஸ்அப், புளூடூத் அல்லது காகிதத்தில் குறியீடுகளை அனுப்பலாம்.",
    )
    val addCodes = LocalizedContent("Add codes", "குறியீடுகளைச் சேர்")
    val pastePlaceholder = LocalizedContent(
        "Paste codes here (each starts with KK1. or KP1.)",
        "குறியீடுகளை இங்கே ஒட்டுங்கள் (ஒவ்வொன்றும் KK1. அல்லது KP1. எனத் தொடங்கும்)",
    )
    val add = LocalizedContent("Add", "சேர்")
    val trySample = LocalizedContent("Try sample data (synthetic)", "மாதிரித் தரவைப் பார் (செயற்கை)")
    val removeSample = LocalizedContent("Remove sample data", "மாதிரித் தரவை நீக்கு")
    val sampleWarning = LocalizedContent(
        "Sample data are synthetic and invented to show how pooling works. They are not real observations.",
        "மாதிரித் தரவு செயற்கையானது; பகிர்வு எப்படி வேலை செய்கிறது என்பதைக் காட்ட உருவாக்கப்பட்டது. உண்மைக் கண்காணிப்புகள் அல்ல.",
    )
    val noPooled = LocalizedContent(
        "No pooled interviews yet. Finish an interview and copy its code, or try the sample data.",
        "தொகுக்கப்பட்ட நேர்காணல்கள் இல்லை. ஒரு நேர்காணலை முடித்து அதன் குறியீட்டை நகலெடுங்கள், அல்லது மாதிரித் தரவை முயலுங்கள்.",
    )
    val splitBy = LocalizedContent("Split by", "குழுவாகப் பிரி")
    val groupLabels = listOf(
        LocalizedContent("Everyone", "அனைவரும்"),
        LocalizedContent("Place", "இடம்"),
        LocalizedContent("Birth decade", "பிறந்த பத்தாண்டு"),
    )
    val strongest = LocalizedContent("Strongest shared memories", "மிக வலுவாகப் பகிரப்படும் நினைவுகள்")
    val agreementWarning = LocalizedContent(
        "Look at the red tags. When many people agree and other causes also explain the change, you have found a shared experience, not proof of a climate cause. This is the difference between agreement and evidence.",
        "சிவப்புக் குறிகளைப் பாருங்கள். பலர் ஒத்துக்கொண்டு, மாற்றத்துக்கு வேறு காரணங்களும் இருக்கும்போது, நீங்கள் கண்டது பகிரப்பட்ட அனுபவம்; காலநிலைக் காரணத்துக்கான சான்று அல்ல. ஒத்துப்போதலுக்கும் சான்றுக்கும் உள்ள வேறுபாடு இதுதான்.",
    )
    val noAnswers = LocalizedContent("No answers", "பதில்கள் இல்லை")
    val smallGroupWarning = LocalizedContent(
        "Some groups have fewer than 8 voices. Treat them as hints, not findings.",
        "சில குழுக்களில் 8-க்கும் குறைவான குரல்கள்; முடிவெடுக்க வேண்டாம்.",
    )
    val less = LocalizedContent("less", "குறைவு")
    val more = LocalizedContent("more", "அதிகம்")
    val colourMeaning = LocalizedContent(
        "Colour shows direction of change, not good or bad",
        "நிறம் = மாற்றத்தின் திசை (நல்லது/கெட்டது அல்ல)",
    )
    val distributionPrefix = LocalizedContent("Distribution: ", "பகிர்வு: ")
    val myCodes = LocalizedContent("My codes", "என் குறியீடுகள்")
    val copy = LocalizedContent("Copy", "நகலெடு")
    val councilPlans = LocalizedContent("Council plans", "ஊர்சபைத் திட்டங்கள்")
    val planCompare = LocalizedContent(
        "Compare: which options did groups with different roles choose, and who did each plan forget?",
        "ஒப்பிடுங்கள்: வெவ்வேறு பாத்திரங்களில் இருந்த குழுக்கள் எவற்றைத் தேர்ந்தெடுத்தன? ஒவ்வொரு திட்டமும் யாரை மறந்தது?",
    )

    val verdicts = mapOf(
        "consistent" to LocalizedContent(
            "Instrument records broadly agree",
            "கருவிப் பதிவுகள் பெரும்பாலும் ஒத்துப்போகின்றன",
        ),
        "mixed" to LocalizedContent(
            "Evidence is mixed or differs by place",
            "சான்றுகள் கலவையானவை; இடத்துக்கு இடம் மாறும்",
        ),
        "confounded" to LocalizedContent(
            "Other causes matter as much or more",
            "பிற காரணங்களும் சமமாக அல்லது அதிகமாகப் பங்களிக்கின்றன",
        ),
    )

    val planOptions = listOf(
        ClassPlanOptionContent("warn", LocalizedContent("Early warning and drills", "முன்னெச்சரிக்கை, பயிற்சி ஒத்திகை")),
        ClassPlanOptionContent("shelter", LocalizedContent("Cyclone shelter with assisted evacuation", "புயல் பாதுகாப்பு மையம், உதவியுடன் வெளியேற்றம்")),
        ClassPlanOptionContent("retrofit", LocalizedContent("Strong roofs for the poorest homes", "ஏழைக் குடும்ப வீடுகளுக்கு உறுதியான கூரை")),
        ClassPlanOptionContent("drains", LocalizedContent("Desilt drains, raise the low road", "வடிகால் தூர்வாரல், தாழ்வான சாலை உயர்த்தல்")),
        ClassPlanOptionContent("mangrove", LocalizedContent("Restore mangroves and the wetland", "அலையாத்திக் காடுகள், ஈரநிலம் மீட்பு")),
        ClassPlanOptionContent("income", LocalizedContent("Income support and crop or boat insurance", "வருமான உதவி, பயிர் / படகுக் காப்பீடு")),
        ClassPlanOptionContent("clean", LocalizedContent("Solar pumps and clean cooking", "சூரிய மின்சார பம்புகள், தூய்மையான சமையல்")),
    )
}
