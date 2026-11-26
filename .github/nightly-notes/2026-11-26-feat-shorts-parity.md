## Improved

- The bottom bar now uses Material's short navigation bar, so screens above it get the same height in split screen as full screen.
- Shorts' action buttons sit on the same scrim discs as the app's, and the rail, labels, channel row and metadata line up with the reference in both window layouts.
- The Subscribe button on a Short now uses Material's compact size, so it matches the reference's height.
- The comments sheet on a Short is a flat full width panel with a drag handle and a titled header, sits above the bottom bar, and opens at the same height the reference gives it.
- While a Short's comments are open, the reel shrinks to the space above the sheet and drops its channel row, metadata and action rail, so the sheet no longer covers the video the way it used to.
- The comments sheet now rests at the height the reference gives it, in full screen and in a split pane, so its top edge, drag handle, title row and close button sit exactly where the reference's do.
- The music tab heads every shelf with the app's large emphasized title on the same 12dp inset and spacing, and its bar carries four action buttons at the bar's end, so its headings and bar now sit where the reference's do.
- The music tab now opens with the same filter chip row the reference shows, and each shelf picks the body its contents call for: dense tiles for mixes, square cards for albums and collection pages, and round portraits for artists, on the reference's own spacing.
- Subscriptions opens on the same Quick access band the reference shows, on its own 12dp inset and 4dp feed margin, with an All filter row under it, so the channel shortcuts, their labels and the chip row sit at the reference's measured positions.- Subscriptions lists its uploads as the reference's rows: a still one feed column wide, the title in the app's small title style, the channel line and the views line under it, each row 226 px tall at the reference's measured frames (rows x5-795, title x400, channel +36 and views +78 from the title top), with the bar's toggle view mode, search and profile discs landing on the reference's first, second and fourth slots.
- The accessibility dump of a phone screen no longer dies on a title that opens with an emoji: the still's monogram took one char of a surrogate pair, and the platform's XML writer refuses a string holding half of one.
