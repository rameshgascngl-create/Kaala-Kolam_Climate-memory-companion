# Locale decisions

T9 retains Android's application-locale mechanism: `AppCompatDelegate.setApplicationLocales` with `locales_config.xml` containing exactly `en` and `ta`. The selected language is already persisted in DataStore and therefore also governs app-owned resource dialogs after recreation.

Tamil Compose text styles carry `LocaleList("ta")`. English scientific acronyms inside Tamil can be rendered through `LocaleText.annotateTamil`, which tags allow-listed acronyms with an English locale span while leaving the displayed text unchanged.

Digits deliberately remain Latin (0–9), matching the audited HTML prototype and the scientific source values. This is a formatting decision, not a translation-review decision.
