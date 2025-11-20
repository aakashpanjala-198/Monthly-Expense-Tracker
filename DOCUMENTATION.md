# Orbit Ledger Technical Narrative

Orbit Ledger is the third variation of the budgeting suite. It keeps the exact feature set demanded by the original assignment—monthly sheet creation, expense management, surplus tracking, and a custom chart—but applies a radically different presentation layer inspired by neon space dashboards. This document walks through every package, class, and composable in approximately fifteen hundred words so that reviewers can understand intent without reading the source code line by line.

---

## 1. Architecture overview

The codebase follows a layered Compose architecture:

- **Data layer** (`data` package) — Room entities `CycleRecord` and `ExpenseEntry`, DAOs, and the `OrbitDatabase` singleton.
- **Domain layer** — `OrbitRepository` wraps DAO calls and composes them into useful operations such as observables that combine expenses with totals.
- **View-model layer** (`viewmodel` package) — `CyclesViewModel` and `CycleDetailViewModel` expose `StateFlow` objects to the UI and route user actions back to the repository.
- **Presentation layer** (`screens` package) — Composable screens (`AuroraHomeScreen`, `CycleDetailScreen`, `ConstellationChartScreen`) implement the new orbit-themed UX.
- **Activities** — `MainActivity`, `CycleDetailActivity`, and `ConstellationChartActivity` provide navigation entry points and host the composables.

No dynamic color or Material defaults are used; the project defines an Aurora-inspired palette for a distinctive visual language. State flows and repository calls keep the UI reactive, so any database change is immediately reflected on screen.

---

## 2. Data layer

### 2.1 Entities (`data/Entities.kt`)

* `CycleRecord` models a budgeting cycle. Important fields:
  * `id` — auto-generated primary key.
  * `title` — user-facing label (may be blank).
  * `year`/`month` — integers representing the cycle’s period (1–12 for month). An index ensures uniqueness so there is at most one record per month/year combination.
  * `income` — expected income for the month.
  * `createdAt` — timestamp when the row is created (defaults to current time). Used for display ordering.
* `ExpenseEntry` represents a single outgoing transaction. Key fields:
  * `cycleId` — foreign key referencing `CycleRecord`. Cascade delete ensures that removing a cycle automatically deletes its expenses.
  * `title`, `amount`, `category` — textual name, monetary value, and optional category tag.
  * `spentAt` — epoch millis when the transaction occurred (defaults to `System.currentTimeMillis()`, satisfying the requirement to default dates).

Both entities include Room annotations for indices and foreign-key behavior to keep queries efficient and data consistent.

### 2.2 DAO interfaces (`data/Dao.kt`)

* `CycleDao` exposes:
  * `watchAll()` — returns a `Flow<List<CycleRecord>>` ordered by year/month descending; main screen uses this to display the orbit list.
  * `fetchAll()` — snapshot list used during chart preparation.
  * `watchOne(id)` and `findOne(id)` — reactive and suspend lookups for a single cycle.
  * `insert`, `update`, `delete` — standard CRUD.
* `ExpenseDao` provides:
  * `watchForCycle(cycleId)` — `Flow<List<ExpenseEntry>>` sorted newest-first for the detail screen.
  * `fetchForCycle(cycleId)` — snapshot list used when building summaries.
  * `watchTotal(cycleId)` / `fetchTotal(cycleId)` — streaming and snapshot total spend per cycle.
  * `findOne`, `insert`, `update`, `delete` — entity-level operations for dialog forms.

### 2.3 Database (`data/AppDatabase.kt`)

`OrbitDatabase` extends `RoomDatabase`. `companion object` maintains a single cached instance via `get(context)` which the view models use. The database registers both entities, exposes DAO getters, and names the file `orbit-ledger.db` to avoid colliding with the other projects.

### 2.4 Repository (`data/Repository.kt`)

`OrbitRepository` is the abstraction the UI talks to. Capabilities:

* `observeCycles()` — forwards `CycleDao.watchAll()`.
* `observeDigest(cycleId)` — combines the cycle flow, the expense list flow, and the total flow to produce a `CycleDigest` data structure (`CycleRecord`, `expenses`, `totalSpent`, and `balance`). Returns `null` if the cycle disappears (allowing the detail screen to close gracefully).
* `createCycle(title, year?, month?, income)` — defaults year/month to the current calendar when null, meeting the “default to current month/year” rule.
* `updateCycle`, `deleteCycle`, `getCycle` — wrappers around DAO calls.
* `addExpense(cycleId, title, amount, category, millis?)` — creates an `ExpenseEntry`, defaulting the timestamp to now, and inserts it.
* `updateExpense`, `deleteExpense`, `getExpense` — manage existing expenses.
* `loadFullSnapshot()` — returns a list of `(CycleRecord, totalExpenses)` pairs used by the chart screen to build historical data.

All methods run on the `Dispatchers.IO` dispatcher via `withContext`, ensuring Room queries do not block the main thread.

### 2.5 Domain data (`CycleDigest`)

`CycleDigest` is a simple container that groups cycle metadata with the expense list, total spent, and the resulting balance. This prevents the UI from manually combining flows or recalculating totals.

---

## 3. View-model layer (`viewmodel/ViewModels.kt`)

### 3.1 `CyclesViewModel`

* Extends `AndroidViewModel` to access the application context for the database.
* Holds a private `OrbitRepository`.
* `cycles`: `StateFlow<List<CycleRecord>>` produced by `repository.observeCycles()` using `stateIn`. The flow is lazily started and replays the latest list.
* `createCycleQuick(title, income)` — triggers when the quick composer is used. Calls `createCycle` with only title/income, letting the repository default the date.
* `createCycleExplicit(title, year, month, income)` — used by the advanced modal dialog.
* `updateCycle(record)` and `deleteCycle(record)` — pass through to repository.
* `chartSnapshot()` — suspend function returning the `(CycleRecord, totalExpenses)` snapshot for charting.

### 3.2 `CycleDetailViewModel`

* Accepts a `cycleId` through its factory.
* Exposes `digest`: `StateFlow<CycleDigest?>` built from `repository.observeDigest(cycleId)`.
* `adjustIncome(income)` — fetches the latest cycle, copies it with the new income, and updates the database.
* `updateCycleMeta(title, year, month)` — rewrites title/year/month.
* `addExpense`, `updateExpense`, `deleteExpense` — manage expenses via repository.

### 3.3 `CycleDetailViewModelFactory`

Implements `ViewModelProvider.Factory`, injecting the cycle ID when `CycleDetailActivity` requests a view model.

---

## 4. Activity layer

### 4.1 `MainActivity`

* Retrieves `CyclesViewModel`.
* Collects `cycles` state using `collectAsStateWithLifecycle`.
* Calls `buildHomeUiState` to produce summary metrics for the hero card.
* Passes callbacks into `AuroraHomeScreen` for quick create, advanced create, edit, delete, chart navigation, and cycle opening. Navigation uses explicit intents to `CycleDetailActivity` and `ConstellationChartActivity`.

### 4.2 `CycleDetailActivity`

* Reads the cycle ID from the intent; aborts if missing.
* Instantiates `CycleDetailViewModel` using `CycleDetailViewModelFactory`.
* Collects the digest flow and renders `CycleDetailScreen`, wiring up callbacks that call the view model.

### 4.3 `ConstellationChartActivity`

* Shares the `CyclesViewModel`.
* Collects `cycles` to react when data changes.
* Uses `produceState` to call `chartSnapshot()` in response to cycle changes and feed the resulting list into `ConstellationChartScreen`.

The manifest declares detail and chart activities as non-exported; only the launcher activity is exposed.

---

## 5. Theme (`ui/theme`)

* `Color.kt` defines a unique palette (`AuroraMidnight`, `AuroraPlum`, `AuroraTeal`, `AuroraCoral`, `AuroraSunrise`, `AuroraMist`, `AuroraSmoke`, `AuroraSlate`).
* `Theme.kt` disables dynamic color and assembles explicit light/dark schemes:
  * Light — bright backgrounds, plum primary, teal secondary, coral tertiary, and slate variants.
  * Dark — midnight background, teal primary, coral secondary, and muted slate surfaces.
* `MonthlyExpenseTrackerTheme` sets the status bar color and toggles light/dark status bar icons, then wraps `MaterialTheme`.

This palette creates a neon space aesthetic that differs clearly from MonthLedger, PocketSheets, and FlowBalance.

---

## 6. Home screen (`screens/HomeScreen.kt`)

### 6.1 `HomeUiState` and builders

`HomeUiState` gathers the raw cycle list plus two derived metrics: total income across stored cycles and the label of the latest cycle. `buildHomeUiState` computes these values.

### 6.2 `AuroraHomeScreen`

* Maintains state for dialogs: quick composer, advanced sheet, edit sheet, and delete confirmation.
* `Scaffold` with `LargeTopAppBar` titled “Orbit Ledger” and a floating action button.
* Background uses a vertical gradient from surface variant to background.
* `LazyColumn` shows:
  * `AuroraHeroCard` — large capsule summarizing total income and last cycle label, with an action chip to open the advanced composer.
  * `EmptyAuroraState` — displayed when no cycles exist.
  * Sticky header “Cycles in orbit”.
  * `LazyRow` of `AuroraCycleCard` — large gradient cards for quick navigation. Each includes edit/delete chips.
  * `OrbitListRow` — stacked list view with responsive delete/edit actions.
* Dialogs:
  * `QuickCycleComposer` — alert dialog for minimal input (title + income) with an “Advanced” button.
  * `CycleEditorSheet` — modal bottom sheet for creating or editing cycles with month/year fields and validation.
  * `ConfirmDeletionDialog` — confirmation before deleting a cycle.

The combination of a horizontal carousel, vibrant gradients, and modal sheets gives the screen a distinct UX compared to the other apps.

### 6.3 Helper functions/components

* `AuroraHeroCard`, `EmptyAuroraState`, `AuroraCycleCard`, `OrbitListRow`.
* `monthLabel(month)` helper converts month integers to localized names.
* `currencyFormatter` uses `NumberFormat` to format income.

---

## 7. Detail screen (`screens/CycleDetailScreen.kt`)

### 7.1 `CycleDetailScreen`

* Accepts `CycleDigest` plus callbacks for navigation and mutations.
* `LargeTopAppBar` shows the cycle name (or month label), with buttons to open the metadata sheet and chart.
* `LazyColumn` background uses gradient similar to home screen.
* Items include:
  * `BalanceCapsule` — gradient header card showing balance, income, spent totals, with a chip to adjust income.
  * `MetricsRow` — three data tiles: number of outgoings, category spread, and average spend.
  * `ExpenseCard` — for each expense, showing amount, date, and category with edit/delete icons. Accent circle uses a generated gradient based on category hash.
  * `EmptyExpensesHint` — friendly message if no expenses exist.

### 7.2 Dialogs and sheets

* `IncomeDialog` — simple alert dialog for updating income (numeric validation).
* `CycleMetaSheet` — bottom sheet to edit name/month/year.
* `ExpenseSheet` — bottom sheet for creating or editing an expense. Includes:
  * Title, amount, and category fields.
  * `SuggestionChip` that launches a `DatePickerDialog` to pick a date.
  * Saves the entry via callback when amount parses successfully.
* Delete confirmation uses `AlertDialog`.

These interactions provide the required functionality: editing income, editing/deleting expenses, editing sheet metadata, and defaulting dates to current time.

---

## 8. Chart screen (`screens/ConstellationChartScreen.kt`)

### 8.1 Data preparation

`ConstellationChartScreen` receives a list of `(CycleRecord, totalExpenses)` pairs. It sorts them by year/month descending and maps them into `ChartPoint` objects (label, income, expenses). The chart displays a maximum of four months at a time, showing the most recent cycles.

### 8.2 Custom Canvas Implementation

**Important:** The chart uses **custom canvas drawing with no external libraries**. All drawing is performed using built-in Jetpack Compose Canvas APIs:

* `androidx.compose.foundation.Canvas` - The main drawing surface (part of official Android Compose library)
* `drawLine()` - Draws axes and grid lines
* `drawPath()` - Draws income and expense trend lines
* `drawCircle()` - Draws data point markers
* `drawText()` - Renders axis labels (months on x-axis, amounts on y-axis)

**No third-party charting libraries are used** (no MPAndroidChart, AnyChart, Victory Charts, etc.). The implementation is entirely custom-built using only official Android/AndroidX libraries.

### 8.3 UI layout

* Top row features a back button and the title "Income/Expenses".
* `Box` containing `IncomeExpensesChart`:
  * **Square aspect ratio** - The chart maintains a 1:1 aspect ratio using `Modifier.aspectRatio(1f)`.
  * **X-axis (months)** - Displays month labels for up to 4 months maximum.
  * **Y-axis (amounts)** - Shows amount labels with grid lines for easy reading.
  * Axes, grid lines, and labels are rendered manually inside `Canvas` using custom drawing code.
  * Two lines (income purple, expenses pink) with circular markers at each data point.
* `LegendRow` explains color mapping (Income vs Expenses).

This chart design uses custom canvas drawing to meet the assignment requirement of a custom composable with no external charting libraries, while providing a clean visualization of income vs expenses over time.

---

## 9. Navigation flow

1. `MainActivity` → `AuroraHomeScreen`.
   * Floating action button → quick composer; advanced button opens bottom sheet.
   * Carousel card or list row → opens `CycleDetailActivity`.
   * Top app bar action → opens `ConstellationChartActivity`.
2. `CycleDetailActivity` → `CycleDetailScreen`.
   * Header edit button → `CycleMetaSheet`.
   * “Adjust income” chip → `IncomeDialog`.
   * FAB → `ExpenseSheet` for new entries.
   * Expense card edit/delete icons → edit sheet or delete confirmation.
3. `ConstellationChartActivity` → `ConstellationChartScreen`.
   * Dragging interacts with custom chart.
   * Back arrow returns to previous screen.

All data operations route through the repository, guaranteeing database persistence and reactive UI updates.

---

## 10. Requirement mapping

1. **Two Kotlin files** — `MainActivity.kt` and `screens` composable files. Additional files exist for the layered architecture.
2. **Create sheet with month/year** — `CycleEditorSheet` collects explicit month/year; repository defaults to current date when omitted.
3. **Clickable list** — `AuroraCycleCard` and `OrbitListRow` open the detail activity.
4. **Single page layout** — `CycleDetailScreen` uses `LazyColumn` with header and expenses.
5. **Display/edit income** — `BalanceCapsule` displays values; `IncomeDialog` updates them.
6. **Add expense** — `ExpenseSheet` bottom sheet adds new entries.
7. **Change income** — `IncomeDialog` + `adjustIncome`.
8. **Surplus display** — `BalanceCapsule` shows balance, and metrics recalculate automatically.
9. **Auto-update on expense add** — flows in `CycleDigest` recompute totals; UI updates immediately.
10. **Separate activity per month** — `CycleDetailActivity` shows an orbit-specific interface.
11. **Custom chart activity** — `ConstellationChartActivity` hosts `IncomeExpensesChart` with custom canvas drawing (no external libraries).
12. **Two-line chart** — `IncomeExpensesChart` draws income and expense lines simultaneously using custom Canvas APIs.
13. **Custom canvas drawing** — Chart uses only built-in Compose Canvas (`androidx.compose.foundation.Canvas`) with `drawLine()`, `drawPath()`, `drawCircle()`, and `drawText()` methods. No third-party charting libraries.
14. **SQLite database** — Room database with cycles and ledger entries.
15. **Edit/delete expenses** — `ExpenseSheet` and delete `AlertDialog`.
16. **Default month/year** — `createCycle` uses current calendar when not specified.
17. **Chart displays max 4 months** — X-axis shows maximum of 4 months as required.
18. **Square aspect ratio chart** — Chart maintains 1:1 aspect ratio using `Modifier.aspectRatio(1f)`.
19. **Default expense date** — `ExpenseEntry` default plus `ExpenseSheet` initialises to current millis.
20. **Remove/edit sheets** — `CycleEditorSheet` edits; `ConfirmDeletionDialog` removes.
21. **Unique UI design** — neon aurora palette, carousel, capsule headers, and custom canvas chart ensure a distinct visual identity.

---

## 11. Testing considerations

* DAO tests can run with an in-memory `OrbitDatabase` to verify sorting, cascade behavior, and totals.
* Repository tests can inject fake DAOs to confirm `CycleDigest` composition.
* View-model tests using coroutine test rules can assert that calls trigger repository methods.
* UI tests could exercise the dialogs and chart interactions using Compose’s testing APIs (drag gestures, chip clicks, etc.).

---

## 12. Extensibility notes

Potential enhancements:

* Sync cycles to cloud storage by layering a network data source on top of the repository.
* Introduce categories with per-category budgets (extend `ExpenseEntry` and `MetricsRow`).
* Add reminders via WorkManager to log outgoings or review balance mid-month.
* Provide export to CSV by streaming repository data and using Android’s storage access framework.

The clear separation between data, repository, and UI layers means these features require minimal cross-cutting changes.

---

## 13. Conclusion

Orbit Ledger reuses the proven data and business logic from MonthLedger but reimagines the interface with a neon orbit aesthetic. The home screen's carousel, the capsule detail header, and the custom canvas chart (built without external libraries) ensure the UX differs markedly from PocketSheets and FlowBalance. Each class described above aligns with the assignment's checklists, and the documentation equips maintainers and reviewers with a comprehensive mental model of how the application works.






