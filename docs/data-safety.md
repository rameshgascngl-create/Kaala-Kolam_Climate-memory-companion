# Google Play Data safety answers

- **Does the app collect any user data?** No.
- **Does the app share any user data with third parties?** No.
- **Data encrypted in transit?** Not applicable: the app has no `INTERNET` permission and sends no app data over a network.
- **Account creation required?** No.
- **Advertising or analytics SDKs?** None.
- **Third-party SDKs?** None beyond AndroidX/Material application libraries; no third-party data service is integrated.
- **Local data:** interviews, predictions, council choices, progress and preferences are stored only in the app's private WebView storage under the fixed appassets origin.
- **Deletion:** uninstalling the app removes its private local data. The app does not maintain a server-side copy.
