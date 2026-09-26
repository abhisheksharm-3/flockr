# Design

Flockr's visual system: the Cobalt direction. Read [PRODUCT.md](PRODUCT.md) first for who this is for.
Every screen is built from the tokens and components below; nothing is styled inline that a token
already covers.

## Visual theme

Bright, friendly, trustworthy. A cobalt block opens the screens that have one headline number or
sentence; below it the page is calm, cool paper (light) or deep ink (dark), with content sitting
straight on the page. One warm colour, sun yellow, is kept for the single most important action on a
hero. Warmth comes from copy, avatars and small moments, never from decoration.

## Colour

Defined in OKLCH, hue 262, in `ui/theme/Color.kt`. Every text pairing meets WCAG AA in both themes.

| Role | Light | Dark | Use |
|---|---|---|---|
| `hero` (`flockrColors`) | `#1957D2` | `#093DA1` | The headline block on hub-like screens |
| `onHero` / `onHeroVariant` | `#F7FAFF` / `#D8E5FD` | `#F1F5FC` / `#B9CBEC` | Text on the hero, primary / quieter |
| `sun` / `onSun` | `#FDDC5B` / `#142139` | `#F4D660` / `#0D1A32` | The hero's main action only; a badge tint |
| `positive` | `#0B764D` | `#82D2A8` | Money owed to the viewer |
| `negative` | `#C2272D` | `#F98F87` | Money the viewer owes |
| `primary` | `#1957D2` | `#9BBEFF` | Buttons, selection, links, focus |
| `background` = `surface` | `#F4F7FB` | `#0F141D` | The page |
| `surfaceContainer*` | cool steps | ink steps | Sheets, menus, inputs; never a card on the page |

Strategy: **Restrained** below the hero, **Drenched** in the hero. Money direction is always carried
by words or a sign as well as `positive`/`negative` (`balanceColor`, `balanceHeadline`).

## Typography

Figtree, bundled (`res/font/figtree.ttf`, variable), one family for everything. All styles use
tabular figures so amounts line up. Scale ratio ~1.2. Display and headline are ExtraBold with slight
negative tracking; titles SemiBold/Bold; body Regular; labels SemiBold.

- Hero figure: `displayMediumEmphasized` (via `HeroAmount`).
- Screen title in the top bar: default `FlockrTopAppBar`.
- Row headline: `titleSmall`; supporting line: `bodySmall` in `onSurfaceVariant`.
- Section headings: `SectionTitle` (small caps-style uppercase label).

## Shape and spacing

Shapes (`ui/theme/Shape.kt`): fields and badges `medium` (14dp), sheets and images `large` (20dp), the
hero's bottom corners 32dp. Spacing tokens in `ui/theme/Dimensions.kt`; page gutter is `Spacing.lg`
(16dp). Vary rhythm: tight inside a row, generous between sections.

## Layout laws

1. **No cards. Anywhere.** No `Card`, no `ElevatedCard`, no `OutlinedCard`, no rounded tinted box
   floating on the page, and never a card inside a card. Group with `Section`/`SectionTitle` and
   space. The only filled blocks are the full-bleed `HeroHeader`, full-width tinted bands for
   something that waits on the user (edge to edge, no radius, no inset), sheets, and dialogs.
2. Lists are `ListRow`s straight on the page, edge to edge, no dividers; rhythm separates them.
3. A screen with one headline number or sentence opens with `HeroHeader` (in a `Scaffold` with no
   top bar, first item of the scrolling content). Every other screen uses `FlockrTopAppBar`.
4. Forms are sentences (below), with the primary action pinned in the bottom bar.
5. Long pickers use `OptionSheet`, a searchable lazy sheet that opens at once however long the list.
6. Empty states teach: say what goes here and offer the action. Errors say what failed and how to fix.

## Rules added from the phone reviews

These override anything above that disagrees.

1. **No back arrows.** Android's system back gesture and button go back. No top bar, hero or screen
   shows a back arrow. `FlockrTopAppBar` and `HeroHeader` no longer take `onNavigateBack`.
2. **Icons are full circles or bare glyphs.** Never rounded squares ("squircles"). `IconBadge` and
   `MemberAvatar` are circles; inline glyphs need no container.
3. **Forms are sentence forms** (`ui/components/forms/SentenceForm.kt`), never a stack of outlined
   fields. The reference implementations are `ExpenseFormScreen.kt` and `SettleUpScreen.kt`:
   - `FormHero(title)` holds the one value the form is about, typed large: `HeroAmountInput` for
     money, `HeroTextInput` for a name. `HeroNote` under it gives live meaning or the error.
   - Everything else is a `Sentence` of `SentenceWords` and tappable `SentenceToken`s ("Paid by
     **you** on **today** for **Groceries**"). A token opens `OptionSheet` (lists), the date
     picker dialog, or a `ModalBottomSheet` holding the richer editor.
   - Optional text is `SentenceNote`. Genuinely free-text inputs that can't be a token (an email,
     a password, an address) sit below the sentence as `FlockrTextField`s, full width.
   - The save action is `FormSubmitBar` in the `Scaffold` bottom bar, always full width.
   - No stacks of labelled fields: use tokens and `OptionSheet`.
4. **Shortcuts are pills.** A row of jumps to other screens is `ShortcutPills`, never icon tiles.
5. **Tabs are `PillSelector`** (the Expressive connected button group), everywhere.
6. **Expressive buttons.** Every `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton` and
   `IconButton` family member passes the Expressive `shapes` (`ButtonDefaults.shapes()`,
   `IconButtonDefaults.shapes()`) so it morphs under the finger. `FlockrPrimaryButton` already does.
7. **Money wording is fixed.** Viewer is owed: action "Record payment". Viewer owes: "Settle up".
   Balances read "owes you ₹x", "you owe ₹x", "gets back", "owes".
8. **Cobalt screens get the status strip.** A screen that opens with `HeroHeader` or `FormHero` in
   a scrolling list overlays `HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)` last in
   a full-screen `Box`, so content never runs under the clock. A screen built on a plain scrolling
   column, such as every sentence form, uses `HeroColumn(hero = { FormHero(...) { ... } })`, which
   does the same. No screen may let content scroll under a bare status bar.
9. **Screens with a headline number lead with cobalt** (`HeroHeader`): Money, Bills (what's due),
   Balances, Usage, Reports, Chores, and any screen whose first question is "how much" or "how many".
10. **Animated icons.** `AnimatedGlyph(icon, trigger, motion)` plays once when `trigger` changes:
    WIGGLE for attention (a bell with news), POP for made or done (an item added, a chore ticked),
    SPIN for swap or refresh, NUDGE for sent. Use it where an icon reacts to an action; never loop.
11. **Haptics** (`rememberHaptics()`): `select()` on every tab, pill selector, chip, token, pager
    page change and picker step; `toggle()` on switches and check-offs; `tap()` on primary actions;
    `success()`/`error()` on save results; `gestureThreshold()`/`gestureEnd()` on swipes and drags.
    Plain navigation stays silent.

## Components (`ui/components`)

- `HeroHeader`, `HeroLabel`, `HeroAmount`, `HeroCaption`, `HeroActions`, `HeroButton` (sun),
  `HeroSecondaryButton` (translucent).
- `Section`, `SectionTitle`, `ShortcutPills` + `Shortcut`, `HeroStatusBarScrim`, `AnimatedGlyph`.
- `forms/SentenceForm.kt`: `FormHero`, `HeroAmountInput`, `HeroTextInput`, `HeroNote`, `Sentence`,
  `SentenceWords`, `SentenceToken`, `SentenceNote`, `FormSubmitBar`. `inputs/OptionSheet`.
- `ListRow`, `TrailingAmount`, `IconBadge` + `BadgeTone` (COBALT, JADE, SUN, SLATE, ROSE).
- `MemberAvatar`, `balanceColor`, `balanceHeadline`.
- `FlockrTopAppBar`, `FlockrPrimaryButton`, `FlockrExtendedFab`, `FlockrFabMenu`.
- `forms/ToggleRow`, `inputs/*` (text, amount, date dialog, option sheet, pills).
- `states/EmptyState`, `states/ErrorState`, `dialogs/ConfirmDialog`.

## Motion

Material 3 Expressive motion scheme (`ui/theme/Motion.kt`): spatial springs for movement, monotonic
effects for colour and alpha. 150 to 250 ms feel; motion only shows a change of state. The system's
remove-animations setting is honoured by Compose.

## Copy

Short, human, second person. "You're owed", "Karan owes you ₹560.01", "Nobody owes anybody. Nice."
No em dashes. Buttons say what happens ("Record payment", "Join Maple Street").
