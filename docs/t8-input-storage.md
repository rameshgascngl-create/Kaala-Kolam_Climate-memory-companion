# T8 Tamil input and storage

The persisted state schema now has dedicated UTF-8 strings for elder alias, notes and reflection. DataStore serialisation and KB1 backup/restore both preserve those strings. The restore editor hoists a Compose `TextFieldValue`, so selection and IME composition state are not reconstructed on each recomposition.

A real process-death, Tamil-keyboard, and `adb install -r` backup/restore test still requires an Android device or emulator and must be reported BLOCKED when none is available.

The current Class workflow is still a placeholder; it does not yet emit class codes. Therefore no claim is made that a production Class-code implementation has been device-tested.
