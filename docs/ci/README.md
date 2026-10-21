# CI diagnostics

Artifacts the **Smoke test** workflow commits back to the branch so failures are
readable without downloading Actions artifacts (those links are not reachable from
every environment).

| File | Written when | Contents |
| --- | --- | --- |
| `jvm-test.log` | `JVM launch test` fails | Full `./gradlew testDebugUnitTest --tests '*LaunchSmokeTest*'` output, including the `AssertionError` line that broke. Deleted again on the next green run. |
| `logcat.txt` | manual `Boot app on emulator` run | Filtered `FATAL` / `AndroidRuntime` / `Caused by` / `khajabad` lines, last 400. |
| `emulator.png` | manual emulator run | Screenshot of the running app. |
| `activity.txt` | manual emulator run | `dumpsys activity activities` excerpt (present only when non-empty). |

Force-added (`git add -f`) because `*.log` is gitignored, and pushed with
`[skip ci]` so publishing a log never re-triggers the workflows.
