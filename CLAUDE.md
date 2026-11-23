# CLAUDE.md

ChoresRPG is an Android app written in Java (package `com.zsoftware.choresrpg`). Don't upgrade dependencies or tooling unless the user asks.

## Rules

1. **Follow the project's coding style** (see below). Match the code around your change, even where it differs from standard Java/Android conventions.
2. **Never push to the remote repository.** If a push seems needed, tell the user and let them do it or approve it.
3. **You may commit, but ask before each commit.** Show what will be committed and the proposed message first.
4. **Do not deploy or install to any connected device or emulator** (no `installDebug`, `adb install`, `connectedAndroidTest`, etc.). If testing on a device is needed, ask first. Local builds and JVM unit tests (`gradlew assembleDebug`, `gradlew test`) are fine.

## Coding style

The code uses C#-style naming, not standard Java naming. Keep it consistent.

### Naming
- **Classes, enums:** PascalCase (`ChoreListFragment`, `ChoreIconEnum`).
- **Methods you write:** PascalCase (`GetChoreList()`, `SaveChoreList()`, `UpdateChoreListView()`, `ToSaveDataString()`, `Chore.Parse()`). Android/Java overrides keep their framework names (`onCreateView`, `getView`, `toString`).
- **Fields:** PascalCase, with no `m` prefix (`Contents`, `WeekDays`, `Icon`, `SaveKey`, `SharedPreferences`). One exception: `binding` for View Binding stays lowercase, as in the Android templates.
- **Static singleton backing field:** underscore + PascalCase (`_Instance`), exposed through a static `Instance()` method and set up by `Initialize(...)`.
- **Log tag:** `private static String LOGTAG = "ClassName";`, used as `Log.e(LOGTAG, "Method: message")`.
- **Enum constants:** PascalCase (`Monday`, `Tuesday`, ...).
- **Parameters and locals:** camelCase (`contents`, `weekdays`, `chores`).

### Formatting
- 4-space indentation.
- Class declarations, constructors and framework overrides put the opening brace on the same line.
- The project's own methods and loops often put the brace on a new line (Allman):
  ```java
  public List<Chore> GetChoreList()
  {
      ...
      for (int i = 0; i < strings.length; i++)
      {
  ```
  Follow whichever style the surrounding file or method uses.
- Single-statement `if`/`else` bodies are often written without braces.
- Listeners are written as anonymous inner classes (`new View.OnClickListener() { ... }`), not lambdas.

### Structure and Android conventions
- UI is built from Fragments in a single `MainActivity`. Navigation uses `res/navigation/nav_graph.xml` and `NavHostFragment.findNavController(...)`.
- Use View Binding (`FragmentXxxBinding.inflate(...)`) and set `binding = null` in `onDestroyView()`.
- Data is saved through the `SaveData` singleton, which stores everything in `SharedPreferences` as a delimited string (`;` between chores, `/` between fields; see `Chore.ToSaveDataString()` and `Chore.Parse()`). Don't break that format without asking.
- Code for one feature goes in its own subpackage (for example `choreicon/` holds the icon enum, dictionary, adapter and selector fragment).
- Resources:
  - Layout files are snake_case, often named after their fragment (`fragment_chore_list.xml`, `week_selector_fragment.xml`).
  - View IDs are snake_case, sometimes ending in the view type (`add_chore_fab`, `chore_list_ListView`).
  - All user-facing text goes in `res/values/strings.xml`. Don't hardcode it.

## Build

```
gradlew assembleDebug
gradlew test
```