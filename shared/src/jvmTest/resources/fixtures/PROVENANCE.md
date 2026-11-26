# Fixture provenance

Every file in this directory is a **captured live InnerTube response**, trimmed to the entities
the parser reads and otherwise unedited. Captured on 2026-11-23 against the `WEB` client
(`clientVersion 2.20260710.06.00`) and the `IOS` client (`clientVersion 21.03.1`, `osVersion
18.2.22C152`), from the user's machine.

Trimming means: whole sibling objects and unrelated subtrees were removed to keep the files
readable. No field that the parser reads was altered, and no value was invented.

## `community_post_comments.json`

Produced by `browse` with `{browseId: "FEpost_detail", params: "<post params>"}`, followed by the
continuation request `{continuation: "<post detail continuation>"}` — the comments themselves only
arrive on the continuation, which is the shape `communityPostComments(continuation = …)` sends and
the shape this test asserts.

- Post: `UgkxLvuNrYrKUGnkCWbLxd5VoPZIxuTY-8n_` (`https://www.youtube.com/post/…`)
- Kept: the first two `commentThreadRenderer`s, one `continuationItemRenderer`, and the
  `commentEntityPayload` / `engagementToolbarStateEntityPayload` mutations they reference.
- Dropped: the post itself, the remaining five threads and their entities.

**Note on the first-page request.** `browse` with `browseId: "FEpost_detail"` and only
`canonicalBaseUrl` returns HTTP 400 `INVALID_ARGUMENT` for every real post id tried; the post page
supplies a `params` token and that form answers 200. The facade already accepts `params`, so this
is a caller-shape issue rather than a parser one. See "Still open" in the campaign report.

## `timed_comments.json`

Produced by `next` with the `IOS` client, `X-Goog-FieldMask:
frameworkUpdates.entityBatchUpdate.mutations`, and `formData.commentsFilterFormData.commentsFilter.sortBy
= SORT_BY_TIMED`, using the comments continuation taken from the same video's `WEB` watch page.

- Video: `dQw4w9WgXcQ`.
- Kept: `timedMarkersListSyncEntity` with the first three non-welcome timed items (1000 ms,
  9000 ms, 18000 ms) and the three `commentEntityPayload` entities plus their toolbar-state
  entities they name.
- Dropped: the composer/shared/surface mutations and the other 29 comment entities. The
  `timed_comments_welcome_sync_id` placeholder items are dropped as the parser drops them.

## `live_chat_seed.json`

Produced by `next` with the `WEB` client for live stream `rFZHOHl-L8A`, trimmed to
`contents.twoColumnWatchNextResults.conversationBar`. The seed token is 192 characters;
`isReplay` is absent from this capture, so the model leaves it null.

## `live_chat_poll.json`

Produced by `live_chat/get_live_chat` with the `WEB` client and the seed token from the file above.

- Kept: the `invalidationContinuationData` continuation (526 characters, `timeoutMs` 10000) and
  the first two `liveChatTextMessageRenderer` actions.
- Dropped: the other 68 text messages and the gift/viewer-engagement actions, none of which this
  parser surfaces.
- The first retained message carries no `timestampText`; that absence is real and the test asserts
  it stays null.
- `liveChatFeed` derives its second poll from exactly these two values, so the feed test's
  continuation and `playerOffsetMs` are body-derived, not mock-derived.

## `search_suggestions.json`

Produced by `music/get_search_suggestions` with the `WEB_REMIX` client (`clientName 67`,
`clientVersion 1.20260213.01.00`) and `{"input": "arijit"}`, captured on 2026-11-25. It is not
trimmed: the response is 43 KB and every renderer in it is read.

- The query section holds six `searchSuggestionRenderer`s whose `suggestion.runs` are split where
  YouTube highlights the typed part — `["arijit", " singh"]`, `["arijit", " singh songs"]`, … —
  which is why joining the runs is what produces six distinct terms rather than "arijit" six times.
- The recommendation section holds five `musicResponsiveListItemRenderer`s: an artist, a playlist
  and three songs. The music row reader takes the artist (as an `ArtistItem`) and the three songs
  (as `SongItem`) and not the playlist row, so `SearchSuggestions.recommendedItems` has four
  entries; the fixture test asserts exactly that, so a future reader change shows up here.

`SearchSuggestionsFixtureTest` decodes this file with the client's own `Json` settings
(`ignoreUnknownKeys`, `explicitNulls = false`) and runs the same `toSearchSuggestions()` the app
uses, so the capture proves the parser rather than a hand-written model.

## `channel_search_page.json`, `channel_search_more.json`

Produced by `browse` on `www.youtube.com` with the `WEB` client (`clientVersion 2.20260710.06.00`),
`browseId: "UCsXVk37bltHxD1rDPwtNM8Q"` (Kurzgesagt), the channel search tab's own token
`params: "EgZzZWFyY2jyBgQKAloA"` and `query: "space"`, captured on 2026-11-25. The next page was
captured with the first page's token as a continuation.

- The first page arrives as an `expandableTabRenderer` with `sectionListRenderer.contents`: five rows
  per section, the first of them a `playlistRenderer` and the rest `videoRenderer`s, and a final
  `continuationItemRenderer`. Trimmed to the playlist row, the first two videos and the
  continuation item. The playlist row is kept deliberately: it is what YouTube ranks above the
  videos, and the port now draws it (the playlist's cover is the first thumbnail group; the other
  groups are the stacked videos, so the largest thumbnail is the card's picture).
- The next page answers with `onResponseReceivedActions[0].appendContinuationItemsAction`, whose
  items are one `itemSectionRenderer` per video, one renderer deeper than the first page's rows.
  Trimmed to four rows plus the next token.
- The two shapes are why `parseChannelSearchResponse` reads both: a parser that only knew the
  initial payload returned no rows at all for page two.

## `channel_videos_page_one.json`, `channel_videos_page_more.json`

The same channel's Videos tab, produced by `browse` with `params: "EgZ2aWRlb3PyBgQKAjoA"`, and its
next page through the token the first page ended on.

- The first page's rows are `richGridRenderer` items whose content is a `lockupViewModel` (the
  modern shape: title at `metadata.lockupMetadataViewModel.title.content`), not `videoRenderer`.
  Trimmed to four rows plus the continuation item.
- The next page answers with `appendContinuationItemsAction` and the same lockup rows, trimmed to
  four plus a token. `ChannelTabPage.more()` appends them and drops repeats.

## `channel_posts_page.json`, `channel_posts_more.json`

Kurzgesagt's Posts tab: `browse` with `browseId: "UCsXVk37bltHxD1rDPwtNM8Q"` and the tab's own
token `params: "EgVwb3N0c_IGBAoCSgA="` (`WEB` client, 2026-11-26), then the first page's token as a
continuation.

- Page one is a `sectionListRenderer` with one `itemSectionRenderer` holding nine
  `backstagePostThreadRenderer`s and a `continuationItemRenderer`. Trimmed to the first three posts
  plus the token.
- A post's payload sits at `post.backstagePostRenderer`: `postId`, `contentText.runs`,
  `backstageAttachment` (`postMultiImageRenderer`, `videoRenderer` or a poll),
  `voteCount.simpleText` ("2.1K"), `publishedTimeText.runs`, and the comment count inside
  `actionButtons.commentActionButtonsRenderer.replyButton`.
- The next page answers with **`onResponseReceivedEndpoints[0].appendContinuationItemsAction`**, not
  `onResponseReceivedActions`. That is the newer envelope, and the community-post reader walks the
  whole response rather than one fixed path, so it reads both; the capture is here to keep it that
  way. Trimmed to three posts plus the next token.

## `channel_releases_page.json`

Billie Eilish's Releases tab: `browse` with `browseId: "UCiGm_E4ZwYSHV3bcW1pnSeQ"` and
`params: "EghyZWxlYXNlc_IGBQoDsgEA="` (`WEB` client, 2026-11-26).

- The tab is a `sectionListRenderer` of twelve `shelfRenderer`s, one per release: the shelf's title
  is the release's name, its `endpoint` is that release's playlist (`browseId: "VL<playlistId>"`,
  which `ShelfParser` already exposes as `morePlaylistId`), and its content is a
  `horizontalListRenderer` of `lockupViewModel` items (the release's songs as videos).
- Two shelves were kept, each trimmed to four songs; the channel's own `channelVideoPlayerRenderer`
  section and the other ten shelves were dropped. The four continuation tokens in the capture expand
  a shelf's carousel, so the tab's page is one response and the port offers no "load more" row for
  it.

## `channel_home_page.json`

Kurzgesagt's Home tab: `browse` with `browseId: "UCsXVk37bltHxD1rDPwtNM8Q"` and the tab's own token
`params: "EghmZWF0dXJlZPIGBAoCMgA%3D"` (`WEB` client `2.20260710.06.00`, 2026-11-26). The same tab was
re-captured with `MWEB` and with two other channels (MrBeast, Coldplay) to check the paging shape
below; those captures are not fixtures.

- The selected tab's content is a `sectionListRenderer` of **13 sections**, in YouTube's order:
  1. `itemSectionRenderer` → `channelVideoPlayerRenderer` — the channel trailer, with `videoId`
     `_27eD49ePQE`, `title.runs`, `viewCountText` and `publishedTimeText`, and no thumbnail of its
     own (the player renders it).
  2. `shelfRenderer` "Videos": 12 `lockupViewModel` items, `endpoint.browseEndpoint` = the channel's
     own Videos tab (`browseId` = channel id, `params` `EgZ2aWRlb3MYAyAAcAPyBg0KCzoEIgIIBKIBAggB`).
  3. "Our Best Stuff", then six playlist shelves ("The Existential Crisis Playlist", "The Universe
     and Space stuff", "Human Stuff", "Futurism", "Medicine & Biology", "Science and other stuff
     explained"). Every one of these is a `horizontalListRenderer` of **videos**, not playlists;
     the playlist is only the shelf's `endpoint.browseEndpoint.browseId: "VL<playlistId>"`.
  4. `reelShelfRenderer` "Shorts": 20 items, each a `shortsLockupViewModel` (the same shape the
     Shorts grids carry), not a `reelItemRenderer`.
  5. `shelfRenderer` "Good Stuff": nine `gridChannelRenderer` items — the related-channels shelf.
  6. `shelfRenderer` "Posts": 12 `postRenderer` items (the Posts tab's own renderer), `endpoint`
     linking the channel's Posts tab. This is a carousel of posts, not a post feed.
  7. `shelfRenderer` "Collaborations": one `lockupViewModel` video and a `playAllButton`.
- Trimmed to two items per carousel (three Shorts) and the trailer; `trackingParams`,
  `clickTrackingParams`, `themedPalette` and `rendererContext` were stripped as unread. The whole
  selected tab is kept, so shelf order, names and each item's real payload are what the tests see.
- **There is no page-level continuation.** The section list has `contents`, `trackingParams`,
  `targetId` and `disablePullToRefresh` and no `continuations`; no section or shelf carries a
  `continuationItemRenderer`. The only five continuation tokens in the payload sit inside
  header/description `showEngagementPanelEndpoint` panels (the "read more" previews), which is why
  the parser must not offer Home a footer: `channelItemContinuation()` would surface one of those
  tokens and append nothing. The MWEB capture and the two other channels showed the same shape
  (Coldplay 13 sections, MrBeast 5), so Home is one response whoever serves it.
- The related-channels shelf exposed two dropped card types: `gridChannelRenderer` items were
  parsed but mapped to `null` in the channel feed (the shelf disappeared entirely), and the Posts
  shelf's `postRenderer`s had no card shape in a shelf (also dropped). Both are now carried —
  channels as `FeedEntry`s with `channelId`, posts in `ChannelShelf.posts` — and the shelf's
  `endpoint.browseEndpoint.browseId` is only treated as a playlist when it starts with `PL`/`RD`
  after a `VL` prefix, so a video shelf's heading is not a link to a channel id-as-playlist page.

`ChannelPageFixtureTest` parses this capture through the client's own reader and asserts the
section order, the trailer as a featured shelf, each shelf's mapping, the Shorts items, the related
channels, the posts shelf and the absent continuation.

## `post_page.json`, `post_comments.json`, `post_replies.json`

A community post's own page and its comment section, captured 2026-11-26 with the `WEB` client
(`2.20260710.06.00`) on the main site's key. The page request is `browse` with
`browseId: "FEpost_detail"` and the post's own token
`params: "wgNaEhhVQ3NYVmszN2JsdEh4RDFyRFB3dE5NOFEaJFVna3hmSWc5ZGJNdUZaa1dWX1BBRk52dV85QXVUaURsa1ptS1oYVUNzWFZrMzdibHRIeEQxckRQd3ROTThR"`
(the `browseEndpoint.params` of the post's comment button), not its id: `browse` with only
`canonicalBaseUrl: "/post/<id>"` answers HTTP 400 `INVALID_ARGUMENT`, as the older post-comments
note above records. The post is Kurzgesagt's Black Hole Week post
(`UgkxfIg9dbMuFZkWV_PAFNvu_9AuTiDlkZmK`).

- The page arrives in the **channel-page shell**, not as a standalone renderer:
  `contents.twoColumnBrowseResultsRenderer.tabs[0].tabRenderer` (title "Posts") →
  `content.sectionListRenderer.contents`, two `itemSectionRenderer`s. The first has
  `sectionIdentifier: "backstage-item-section"` and a `backstagePostThreadRenderer` whose `post`
  holds the `backstagePostRenderer`; the second has `sectionIdentifier: "comment-item-section"` and a
  single `continuationItemRenderer` — **the page carries no comments at all**, only the token the
  comment section opens from.
- The post's page copy differs from the Posts tab's copy of the same post: `actionButtons` has no
  `replyButton` on the detail page, so the page's post renderer carries no comment count and no
  `params`. The card's token (from the tab renderer) is therefore what opens the page, and the
  page's own count comes from the comment section's header.
- The post's attachment is a `postMultiImageRenderer` with two `backstageImageRenderer` pictures;
  `voteCount.simpleText` is "2.1K" and `publishedTimeText.runs` "2 days ago".
- **The author's channel** rides two fields, both the same target: `authorEndpoint.browseEndpoint`
  (`browseId: "UCsXVk37bltHxD1rDPwtNM8Q"`, `canonicalBaseUrl: "/@kurzgesagt"`) and the author
  text's own run (`authorText.runs[0].navigationEndpoint.browseEndpoint.browseId`). The post page
  and the tab's copy both carry `authorEndpoint`; the parser reads it first and falls back to the
  run so a copy without the endpoint still links. `authorChannelId` is the only new field the
  parser keeps from this shape.
- **The gallery's wire shape.** A multi-image post is
  `backstageAttachment.postMultiImageRenderer.images[]`, each entry a
  `backstageImageRenderer.image.thumbnails[]` at 120/320/640-ish widths — the pager takes each
  entry's largest thumbnail. A single-image post is the *shorter* shape,
  `backstageAttachment.backstageImageRenderer.image` with no wrapper and no list, which is why
  `toPostAttachment` tests the multi-image case first; both collapse to one `PostAttachment.Images`
  list, so one page for a single image and N pages for a gallery. Neither shape carries the "1 / 2"
  badge or any page count: the position label is the app's own, formatted from the pager's state.
  The captured `channel_posts_page.json` holds all three shapes at once — a 2-image post, a
  single-image post and an **8-image** post
  (`UgkxUaw_MDnWP1NzTuH869ho-wL459HpWkM6`) — which is where the multi-page bounds are proven
  (`PostPageFixtureTest.everyPostShapeKeepsItsGalleryAndItsAuthorsChannel`).
  Compared with the nearby image shapes already ported, this is not a thumbnail lockup
  (`thumbnailRenderer` on a video card) and not the player's hero artwork: it is a bare ordered
  list of equally-sized pictures, and a picture that fails to load has no fallback URL (the size
  ladder is one entry, not the `hq720`→`hqdefault` pair video posters carry), so the card draws a
  neutral placeholder instead of retrying a size YouTube never served.
- `post_comments.json` is that token's answer. It is **two `onResponseReceivedEndpoints`, both
  `reloadContinuationItemsCommand`**, not the append action the Posts tab pages with: endpoint 0
  targets `comments-section` slot `RELOAD_CONTINUATION_SLOT_HEADER` with one
  `commentsHeaderRenderer` (its `countText.runs` are "22" + " Comments"), endpoint 1 targets the
  same section's `RELOAD_CONTINUATION_SLOT_BODY` with 16 `commentThreadRenderer`s followed by a
  `commentFilterContextViewModel`. There is **no trailing continuation item**: this comment list is
  one response. Trimmed to the header, the first three threads, the filter context, and the seven
  `frameworkUpdates` mutations those threads and the shared payload reference; the header's
  `customEmojis` list (the reason this response is 238 KB) was dropped as unread.
- Each thread's replies live behind a `commentRepliesRenderer.contents[0].continuationItemRenderer`
  token. That token's answer (`post_replies.json`) arrives under
  **`onResponseReceivedEndpoints[0].appendContinuationItemsAction`** with a bare
  `commentViewModel` item — no `commentThreadRenderer` wrapper — plus five `frameworkUpdates`
  mutations. It is a `/browse` continuation (`CONTINUATION_REQUEST_TYPE_BROWSE`), not the `/next`
  continuation video replies use, which is why the post page's replies go through
  `postCommentsBrowse(continuation = …)`. Trimmed to that one reply and its mutations.
- A parser gap this capture exposed: `commentCountText` only read `engagementPanels`, which a post
  comments page does not carry. It now falls back to the captured `commentsHeaderRenderer.countText`
  ("22 Comments"), so the post page shows YouTube's own total.

## Regenerating

The commands and the client contexts are recorded in the campaign notes. Any refresh must keep
the same parser-read fields, or the test will fail rather than silently pass.

## `home_feed.json`

Produced by `browse` with the `WEB_REMIX` client (`clientId` 67) and `{browseId: "FEmusic_home"}`.
Trimmed to the `Trending community playlists` carousel with its first four
`musicTwoRowItemRenderer` items, plus the real `responseContext`.

The `Quick picks` carousel was dropped deliberately: its items are
`musicResponsiveListItemRenderer`, which `HomePage.Section.fromMusicCarouselShelfRenderer` skips
unless `chipPage` is set, so keeping it would have produced an empty fixture that still looked
populated.

## `search_results.json`

Produced by `search` with the `WEB` client and `{query: "rick astley"}`. Trimmed to the first three
`videoRenderer` results and the real `estimatedResults`.

## `video_detail.json`

Produced by `next` with the `WEB` client for `i1o1p_DD6TU` — the first playable entry the trimmed
home feed links to. Trimmed to `videoPrimaryInfoRenderer` and `videoSecondaryInfoRenderer`, which is
all `WatchMetadataResponse`'s `title()`, `channelName()`, `description()` and `viewCountText()` read.

`viewCountText()` is null for this capture: the response carried no `videoViewCountRenderer`. The
player screen therefore renders the view line only when it is present, and the test does not assert
it.

## `video_related.json`

Produced by `next` with the `WEB` client for `dQw4w9WgXcQ`. Trimmed to the
`videoPrimaryInfoRenderer` and `videoSecondaryInfoRenderer` (the watch header, as in
`video_detail.json`) and the first four `lockupViewModel`s of
`contents.twoColumnWatchNextResults.secondaryResults.secondaryResults.results` — the related rail as
YouTube serves it to a signed-out WEB client, which publishes recommendations as lockups rather
than `compactVideoRenderer`s.

The four ids (`E8gmARGvPlI`, `djV11Xbc914`, `TicIH5VBSqM`, `eNvUS-6PTbs`) and their order are
whatever that request answered. `dQw4w9WgXcQ` itself does not appear in the rail, which is what lets
the loader's self-exclusion assert hold instead of being untested.

## `video_comments_entry.json`

Produced by the same `next` request as `video_related.json` (WEB client, `dQw4w9WgXcQ`) and trimmed
to the shape the comment loader reads: the `videoPrimaryInfoRenderer` and
`videoSecondaryInfoRenderer`, the `itemSectionRenderer` whose `sectionIdentifier` is
`comment-item-section` with its `continuationItemRenderer` token, and the
`engagement-panel-comments-section` entry of `engagementPanels`, whose header carries YouTube's
`2.4M` comment total.

The token is 146 characters and was answered for the live video on 2026-11-23. Comments are open, so
the section holds an `Open` token rather than a `messageRenderer` notice.

## `video_comments_page.json`

Produced by `next` with that token (WEB client). Trimmed to the two `reloadContinuationItemsCommand`
endpoints a first page splits itself across — the `commentsHeaderRenderer` command and the thread
list — keeping the first six `commentThreadRenderer`s, the list's `continuationItemRenderer`, and
only the `entityBatchUpdate` mutations those six threads name (`commentKey`, `commentSurfaceKey`,
`toolbarStateKey`).

The six authors, their texts, `14` / `73K` / `109K` / `294K` / `352` / `429K` like labels and their
reply totals are exactly what YouTube returned, as is the leading non-breaking space in some
thumbnails' metadata. None of the six captured threads is pinned and none is from the uploader, so
the model keeps those flags false; the loader's pinned and uploader handling is implemented but not
exercised by this capture.

## `video_comments_pinned_entry.json`

Produced by `next` with the WEB client for `fJ9rUzIMcZQ` (Queen — Bohemian Rhapsody), trimmed the
same way as `video_comments_entry.json`: the watch header, the `comment-item-section` with its
continuation token, and the `engagement-panel-comments-section` panel whose header carries YouTube's
`701K` comment total.

Chosen by probing live watch pages for comments that actually carry the flags: this section's first
thread is pinned by the channel and authored by it. Several other popular videos were probed and had
neither.

## `video_comments_pinned_page.json`

Produced by `next` with that token, trimmed to the first six `commentThreadRenderer`s, the header and
continuation item, and only the `entityBatchUpdate` mutations those threads name — same trimming as
`video_comments_page.json`.

The first captured thread is the real pinned, uploader-authored comment and everything the flags are
read from is in this file unaltered:

- thread `renderingPriority`: `RENDERING_PRIORITY_PINNED_COMMENT`
- thread `commentViewModel.pinnedText`: `Pinned by @Queen`
- entity `author.isCreator`: `true`, `author.displayName`: `@Queen`, `author.isVerified`: `true`
- entity figures: `533K` likes, `934` replies, published `7 years ago`

The other five threads carry no `pinnedText`, `RENDERING_PRIORITY_UNKNOWN`, `isCreator: false`, so
the same page proves both flags are read rather than assumed for every row.

## `video_comment_replies.json`

Produced by `next` with the WEB client for `fJ9rUzIMcZQ`, using the continuation token carried by
the pinned `@Queen` thread's `replies.commentRepliesRenderer` in `video_comments_pinned_page.json`
(208 characters; the pinned thread has 934 replies). The response answered with one
`appendContinuationItemsAction` whose `targetId` is `comment-replies-item-UgwiGbrw8tqCj-TE5NV4AaABAg`.

Trimmed the same way as the other comment fixtures: the first four reply items, the list's
`continuationItemRenderer` (YouTube still has more pages of this thread), and only the
`entityBatchUpdate` mutations those items name.

Real fields in this capture, unaltered:

- reply authors, in order: `@williamdebarros196`, `@benjadominguez9600`, `@the_jay_21.87`, `@sharlott9993`
- like labels: `16K`, `17K`, `8.1K`, `4.7K`; published: `7 years ago (edited)`, `7 years ago`
- every reply carries `isCreator: false` and no `pinnedText`, and its toolbar has no `replyCount`,
  so a reply is a comment with zero replies and no replies token of its own
- the first reply begins `Seek God. It's not about religion, it's not about rituals`

One reply is Japanese (`@名無-x7x`) and is not among the four kept items.

## `channel_page.json`

Produced by `browse` with the WEB client and `{browseId: "UCiMhD4jzUqG-IgPzUmmytRQ"}` (Queen
Official). Trimmed to `header` and `contents.twoColumnBrowseResultsRenderer.tabs`: every tab
descriptor is kept with its endpoint (so the Videos tab's token is real), but only the selected Home
tab's first `itemSectionRenderer` is kept as content, because this port fetches the Videos tab by its
own token instead of rendering Home's shelves.

The header is YouTube's `pageHeaderRenderer` for the channel: title `Queen Official`, handle, avatar
and the subscriber line the header carries.

## `channel_videos_page.json`

Produced by `browse` with the WEB client and the Videos tab's `browseEndpoint.params`
(`EgZ2aWRlb3PyBgQKAjoA`) for the same channel. Trimmed to that tab's `richGridRenderer`: the first
six `richItemRenderer` items plus the grid's continuation item. The six items are `lockupViewModel`s,
which is what the channel grid serves to a signed-out WEB client, and the first is `QIbT7qqnEDo` —
`Queen - Sheer Heart Attack (Musikbox, February 11, 1979)`.

## `shorts_sequence.json` and `shorts_sequence_more.json`

Produced by `reel/reel_watch_sequence` with the WEB client. The seedless first request is
`{sequenceParams: "CA8%3D"}` and answers with exactly one entry plus a `continuationEndpoint`; the
next page is the same request with that endpoint's `continuationCommand.token` in `sequenceParams`
again (probed live: nine entries), not a `continuation` field. Both are the same body the app sends —
no client signature, no seed: the response never contains the reel that seeded it.

`shorts_sequence.json` is that first page, and `shorts_sequence_more.json` is the continuation page
trimmed to its first three entries. Each entry keeps its whole `command.reelWatchEndpoint` except
`unserializedPrefetchData`, a 600 KB prefetch blob this UI never reads; nothing else was edited, so
every `videoId`, `params`, `playerParams` and `thumbnail` is YouTube's. The entries are real reels:
`QUIJ8UtYBzA`, `xMHuKwf18vM` and `U9KjvoAoQZA`, and each entry's still is its
`https://i.ytimg.com/vi/<id>/frame0.jpg` frame.

## `shorts_overlay_QUIJ8UtYBzA.json`, `shorts_overlay_xMHuKwf18vM.json`, `shorts_overlay_U9KjvoAoQZA.json`

Produced by `reel/reel_item_watch` with the WEB client,
`{playerRequest: {videoId: <id>}, disablePlayerResponse: true}`, once per reel above. A sequence
entry carries no title or owner (checked on the captured pages), so the reel's title, channel,
avatar and counts come only from this response.

Each fixture keeps the response's `overlay.reelPlayerOverlayRenderer` with its header and
`playerOverlay` (the metadata panel and action bar), and drops the sibling `engagementPanels`,
`frameworkUpdates`, `responseContext` and the overlay's `menu`/`nextItemButton`/`prevItemButton`
widgets. The captured values the UI asserts are:
`QUIJ8UtYBzA` — "@shorts_kingdom01", title beginning "Uncle's kindness towards his friend";
`xMHuKwf18vM` — "@Virat_zxv", title "November 18, 2026" (YouTube sends a date here);
`U9KjvoAoQZA` — "@PetsSwag", title "Saved a sea snakes and gave them another chance at life 🥹".

## `playlist_page.json`

Produced by `browse` against `https://music.youtube.com/youtubei/v1/browse` with the `WEB_REMIX`
client (`clientVersion 1.20250101.00.00`), `browseId: "VLRDCLAK5uy_kiDNaS5nAXxdzsqFElFKKKs0GUEFJE26w"`
— the `VL`-prefixed id of the "90s Bollywood Dance" playlist the captured Home feed shows.

- Kept: `responseContext`, the first tab's single `musicResponsiveHeaderRenderer`, and the
  `secondaryContents` shelf with its **first three** `musicResponsiveListItemRenderer`s and the
  shelf's continuation tokens.
- Dropped: the tab's remaining sections, the other tabs and the other 56 songs.
- Read by `loadPlaylist`, which maps the header to the screen header and each song to a row whose
  own id is the video id, so an entry opens the player. Asserted by `PlaylistFeedTest`;
  `AppNavigationTest.theCapturedHomeFeedGivesPlaylistCardsAPlaylistId` covers the Home card side.

## `library_landing_signed_out.json`

The reference app's library request is `YouTube.library(browseId = "FEmusic_library_landing",
tabIndex = 0)`: a `browse` POST to `music.youtube.com/youtubei/v1/browse` with the WEB_REMIX client
(`clientName: "WEB_REMIX"`, `clientVersion: "1.20260710.01.00"`, `hl: "en"`, `gl: "US"`) and the
signed-in cookies attached. With no account behind it, YouTube answers with no grid and no shelf at
all: the tab's `itemSectionRenderer` holds a `messageRenderer`, `Explore your favorites`, whose
button is a `signInEndpoint` and whose subtext is `Sign in to access tracks that you liked or
saved`. There is no continuation and no item.

That is the state this fixture captures, and it is why the loader reads the message from the raw
body: the shared typed browse model's item-section content knows only music renderers, so decoding
the signed-out body with it throws a `MissingFieldException`. `YouTube.library` therefore parses the
JSON tree for the notice and treats the typed decode as best-effort, which turns YouTube's empty
sign-in state into an empty `LibraryFeed` with YouTube's own wording instead of a failure.

The file keeps that tab whole and the response's `trackingParams`; the only sibling removed is
`responseContext`. The library's own content is local-first in the reference branch (its
`LibraryViewModel` builds sections from Room: playlists, liked videos, history, downloads), so the
saved and downloaded rows this port renders come from the shared Room store, not from this response.

## Port inventory: what the shared app covers, and what stays on Android

Next surface after Channel Home (2026-11-26): the community post page. It is the only surface the
tracker named as missing rather than merely narrower, and it is now real; see the Channel section's
post-page note and the `post_page.json` fixture section above.

The shared app (`shared/src/commonMain`) is the port of the reference branch's UI and network layer.
Everything below was checked against `app/src/main/java/io/github/aedev/flow/ui/screens/` and the
data layer it calls.

Covered end to end in shared, with captured fixtures above: Home feed, Search (results, counts,
filter chips), Channel (header, tabs, in-channel search with playlist rows, tab paging, Posts, Releases,
grid/list layout), community post pages (post, comments, replies), Shorts (reel sequence + overlays,
swipe pager),
Player (watch metadata, real Android playback, immersive landscape fullscreen with the system
back gesture leaving fullscreen first, related rail, comments first page, pinned/uploader flags,
reply threads, live chat), Library (saved/downloaded state in shared Room), navigation,
theming, imagery.

Live chat is the newest consumer of already-captured traffic: the player reads the chat token out of
the watch page it already fetched (`contents.twoColumnWatchNextResults.conversationBar`), then polls
`live_chat/get_live_chat` through `YouTube.liveChatFeed` with the server's own `timeoutMs`, mapping
each `ChatMessageRenderer` into `LiveChatMessage`. Two differences from the reference app are
deliberate: the chat is an inline section under the comments instead of a modal bottom sheet (the
port has no sheet host for the player), and the server's ARGB super-chat colours are not applied —
the row uses theme tokens, the shared module's rule for every colour. The poll loop is a cold flow,
so leaving the player cancels it; no platform timer is involved.

### Explore: categories, shelves and charts

The reference's Categories page is now a tab of its own (`ExploreDestination.Explore`, `FlowTab`
`EXPLORE`), drawn from the same `browse` traffic the reference app reads:

- The tab's chips are the reference's own seven destinations (Live, Gaming, Music, News, Sports,
  Learning, Fashion, in that order) with the reference's string names. Trending, Podcasts, Movies
  and Shopping stay out for the reason the reference lists them out: 400, 429 or an empty page for
  an anonymous client.
- A destination lands as its own shelves (`toExploreDestinationShell` + `exploreShelves`), each
  shelf's videos, reels, playlists and channel items mapped by `FeedShelf.toExploreSection` into
  the same `VideoCard` the rest of the port draws; a shelf with nothing the card can open is
  dropped rather than left as a bare header.
- A shelf's **See all** pushes `ExploreShelfScreen`, which pages `channelTab(browseId, moreParams)`
  and its continuation, so Back returns to the category with its chip still selected.
- Gaming is the reference's **paged grid**: it browses with the destination's own token
  (`GGpbdT5D5zE`) and pages in place, while Music is the reference's **chart**: the
  `FEmusic_charts` browse names the trending chart playlist and `videoPlaylistPage` reads its
  ranked rows through `VL<id>`. Both open playback through the one session queue, exactly like a
  card on Home.

Remains in this area, each named rather than silent:

- **Shelf expand-in-place.** The reference `FeedShelfSections` can expand a collapsed shelf under
  its header with a chevron; the port draws every shelf item in the grid and offers only the
  shelf's See all.
- **Grid/list toggle and column preference.** The reference top bar switches a category between
  grid and list and honours `HomeFeedColumns`; the port fixes three columns, as Home already does.
- **Category sub-tab menus.** The reference `CategoryTabBar` opens a destination's second-level tab
  menu (a destination can ship its own sub-tabs); the port exposes only the chip row.
- **Region picker.** The reference lets a chart be read for a chosen country; the port takes the
  server's default region.
- **Moods and genres under Music.** The music destination's own mood shelf is not seeded; the index
  is reached from the music tab's bar instead (see *Music: moods and genres* below).
- **Shorts shelves open the ordinary player.** A reel card in Explore plays through the session
  queue; it does not enter the Shorts pager.
- **Community-post shelves are dropped.** The News destination ships a post shelf; the port has no
  post card, so that shelf is filtered out rather than rendered empty.

### Music: moods and genres

The reference's `MoodsAndGenresScreen` is now two destinations in the port, both off the music tab
(which is itself a rail and bottom-bar item):

- **The index** (`AppDestination.Moods`, `MoodsScreen`) is one WEB_REMIX browse of
  `FEmusic_moods_and_genres`. Captured live on 2026-11-25, that response is two grids —
  "Moods & moments" and "Genres" — holding 50 `musicNavigationButtonRenderer` buttons with one
  `gridHeaderRenderer` title each, so `YouTube.moodsAndGenres()` returns both categories.
- **A collection** (`AppDestination.Mood`, `MoodScreen`) is a WEB_REMIX browse of the endpoint the
  button carried. Captured live, `browseId=FEmusic_moods_and_genres_category` with the button's
  `params` token answers 13 `musicCarouselShelfRenderer` shelves of `musicTwoRowItemRenderer` cards
  and no continuation; the page runs through the same `toMusicSections()` reader the music tab uses,
  so its albums and playlists open the playlist page, its songs play through the one session and its
  artists open the artist page.

Every mood button on the index browses the same category id and differs only by its token, which is
why `MoodGenre.id` is the token-qualified key — a browseId-only key collapsed all 50 buttons into
one, and the test pins that.

Deliberate differences from the reference: the `solid.leftStripeColor` YouTube sends with each button
is not painted (the shared module allows no colour outside the theme), so the reference's coloured
buttons are Material filter chips in the app's own palette; the collection page has no sort/filter
chrome because the reference's own browse page carries none for a mood; and the music tab does not
seed a "Moods & genres" shelf — the index sits behind the tab bar's grid action instead.

What remains unported in this area:

- **Mood button artwork.** The reference draws each button with the stripe colour and, in its
  `MoodCategorySection`, a size/width policy of its own; the port uses one chip size for every
  button.
- **Music playback surfaces.** A mood's songs open the same player every other song does; the
  reference's music player, queue sheet and lyrics stay unported (see the playback section).

### Search: field, completions, recents, results

The reference's search screen is one page with two modes — what you may be looking for while you
type, and what you found once you submit. Both are now in the port, on the shared innertube layer:

- **Completions.** `YouTube.searchSuggestions()` POSTs `music/get_search_suggestions` (WEB_REMIX).
  Captured live on 2026-11-25 with `input=arijit`, the response is two suggestion sections: six
  `searchSuggestionRenderer` rows whose `suggestion.runs` are split into the typed part and the rest
  (`["arijit", " singh"]`, `["arijit", " singh songs"]`, …), and five
  `musicResponsiveListItemRenderer` recommendations. The existing parser joins the runs, which is
  what makes the six rows six different terms; `toSearchQueries()` then trims, drops blanks,
  de-duplicates by casing and caps the panel at eight.
- **Recents.** `SearchHistory` keeps the last 20 terms, newest first, moves a repeated term back to
  the front instead of duplicating it, and is stored per host (`flow_onboarding` preferences on
  Android, `NSUserDefaults` on iOS, `~/.flow/flow-search-history.json` on desktop). Each row can be
  removed, and the list cleared from its heading.
- **Results.** The page is `search` through `videoSearch`, whose `SearchResultsPage.continuation`
  now reaches the UI: `SearchFeed.more()` appends the next page under the shown results and
  de-duplicates by id, and the grid's footer asks for it as it comes into view. A channel result —
  previously dropped because the grid could not draw it — is now a `FeedNavRow` with the channel's
  avatar, name, handle and audience, opening the channel page; videos and playlists keep the card
  the feeds use, and playlists open the playlist page.
- **Narrowing.** YouTube's own chips and the app's filter dialog (type, duration, upload date, sort,
  features, each encoded into the one `params` token) were already ported and are unchanged.
- **Restore.** The cached session now carries its continuation too, so a relaunch keeps paging.

`SearchFlowUiTest` drives the real screen offscreen against these captured pages: typing shows the
completion list and picking one runs it and writes it to recents, an empty field shows the recents
(with each row removable and the list clearable), and the grid's footer asks for one continuation
and appends what it returns. `SearchHistoryTest`, `SearchFeedTest` and
`SearchSuggestionsFixtureTest` cover the pure parts — dedupe, cap, the sanitising read, `more()`,
the suggestion trim and the run joining.

The live captures used here (both reproducible with the WEB_REMIX client and key the app already
sends): `POST /youtubei/v1/music/get_search_suggestions` with
`{"input":"arijit"}` → 6 completions + 5 recommended items; and the results side is the same
`/youtubei/v1/search` first page the port has used since Search shipped, whose `continuation` is what
this pass started reading.

What remains unported in this area, named rather than silent:

- **Voice search.** The reference launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` from the field;
  that is an Android-only activity contract and the shared screen has no equivalent entry.
- **Search as a bar destination.** The reference has a `FlowTab.Search` nav item but ships
  `NavigationVisibility.search = false`, so it is not in its bar by default either; the port keeps
  the app's top-bar search entry (Home, Music, Library, Library's bar) and its `Search` tab route.
- **Search inside a channel** is still a channel-page gap rather than a search-screen one; the
  reference's channel tab bar can search that channel's videos.
- **Music search.** The reference's `MusicSearchScreen` (music-only result vocabulary) is not ported;
  the port's search returns the video/playlist/channel mix the main site sends.
- **Shorts from search.** The reference can open a Shorts queue seeded from a search; the port's
  search opens reels as ordinary cards in the one session.

### Channel: tabs, in-channel search and paging

The channel page is the surface a search result or a feed card opens into, and it now reads all of
the channel traffic the reference does, on the same shared loaders:

- **Tabs.** `channelTabs()` draws every tab YouTube published that answers with a surface this UI
  can draw: **Home**, Videos, Shorts, Live, Playlists, **Posts** and — where a channel publishes
  one — **Releases**. A playlist tab's rows are `playlistRenderer` items mapped to `FeedEntry`s with
  `playlistId` set, so the card opens the playlist page through the one session. Store and Search
  stay hidden for the reference's own reasons; the channel still opens on Videos, whose first page
  the landing fetch already carries, and Home loads its shelves when tapped.
- **Home.** The tab is the channel's featured trailer followed by its named shelves, in YouTube's
  order: the trailer as one full-width card, video carousels as the two-card grid (or one row per
  card in the list layout the bar's control switches), the Shorts shelf's `shortsLockupViewModel`s
  as cards, the related-channels shelf as `ChannelNavRow`s that open the channel page, and the Posts
  shelf as the same post cards the Posts tab draws. An empty answer draws the shared empty line and
  a failed one the retry line, not a spinner. Home is a single response: the capture carries no page
  token, so no footer is offered and `channelItemContinuation()` is not consulted (the only tokens
  in the payload belong to header description panels).
- **Posts.** The tab is a feed of `backstagePostRenderer`s, not a grid, so `ChannelTabContent`
  carries `posts` and the screen draws a card of its own: author line and age, the text, the
  attachment (the gallery's pager, a shared video's still and title, or a poll's choices),
  and the like/comment counts. `LoadMoreRow` reads the next page through the same
  `channelTabContinuation`; the community-post reader accepts both continuation envelopes, which the
  captured second page proved necessary (`onResponseReceivedEndpoints`).
- **Releases.** The tab is one `shelfRenderer` per release; each becomes a `ChannelShelf` with the
  release's title, its songs as cards, and the release's playlist id, so the heading opens the
  playlist page. The page is a single response (its tokens expand a carousel), so no footer is
  drawn rather than a button that would append nothing.
- **Grid/list toggle.** The channel bar's layout control switches the grid tabs between two cards a
  row and one row per card (`VideoRowCard`: still on the left, title and byline on the right). The
  choice lives in `ChannelTabState`, so it holds across tabs and, because the activity handles the
  orientation change itself, across rotation.
- **Tab paging.** `ChannelTabPage.more()` appends the next page and de-duplicates by id;
  `loadChannelTabMore()` reads it through `channelTabContinuation`, which the grid parser already
  accepted in its four shapes. The landing page's Videos tab keeps its own next-page token
  (`ChannelFeed.videosContinuation`), so the tab the landing opened can page without a second
  request, and the grid's footer is a `LoadMoreRow` that asks once while a page is in flight.
- **In-channel search.** The magnifier on the channel bar opens the channel's own search, which is
  `browse` on that channel with the search tab's token and `query` — no client-side filtering.
  Completions are not requested here (the reference shows only the field), the term is asked after a
  350 ms typing pause, an empty answer renders "No videos in this channel match …", and a failed
  request offers a retry. Its footer pages the same term through the captured continuation, and the
  results open playback in the existing single session. Rows keep the server's order, so the
  playlist block YouTube ranks above the videos is a playlist card there and opens the playlist
  page.
- **State.** The search state lives in `ChannelBook` beside the tab state, so opening a video and
  coming back keeps the term and its rows; closing the field clears both, as the reference does.

Captured live for this pass (2026-11-25/26, the `WEB` client and the app's own key): a channel
search for `space` on Kurzgesagt and its next page; the same channel's Videos tab and its next page;
its Posts tab and that tab's next page; Billie Eilish's Releases tab; Kurzgesagt's Home tab, plus
MWEB and two other channels to check its paging shape. Each capture exposed a shape the port had to
learn: a search's later pages arrive as appended `itemSectionRenderer` rows (which the parser did
not read at all before this pass), a tab's rows arrive as `lockupViewModel` items, a post's second
page arrives under `onResponseReceivedEndpoints`, a release page is shelves of lockups rather than a
grid, and a Home page is a single shelf list whose only continuation tokens belong to header
engagement panels.

`ChannelSearchUiTest` drives the real screen against those captures: the magnifier opens the field,
typing `space` shows the captured rows and sends one request that names this channel and its term,
closing the field returns to the grid, and the grid's footer reads exactly one next page and shows
its rows. `ChannelPagingTest`, `ChannelPageFixtureTest` and the extended `ChannelTabsTest` cover the
append, the de-dupe, the paging guard, the tab row (Home included), both captured search page
shapes, the playlist row's rank above the videos, the posts page and its newer continuation
envelope, the releases shelves, the Home page's shelves and its absent page token, the
related-channel and post shelves surviving the mapping, a shelf's browse id becoming a playlist id
only when it names one, and the layout switch.

What remains unported in this area, named rather than silent:

- **Home paging.** WEB and MWEB both serve the channel Home as one complete section list (see
  `channel_home_page.json`), so there is no page token to follow. If YouTube starts truncating Home
  and sending the remainder through `sectionListRenderer.continuations` or a section-level
  `continuationItemRenderer`, the parser would need a Home branch for that token; the evidence
  needed is a capture whose selected Home tab carries one of those fields. Until then the tab draws
  every shelf it was served and no footer.
- **Shelf "see all" pages.** A Home shelf's endpoint (the Videos tab, a playlist, the Posts tab) is
  not followed inline; the playlist case is already the shelf heading's link, and the tab cases are
  reachable from the tab row.
- **Post page.** Ported this pass: a post card that carries a `FEpost_detail` token opens
  `PostScreen`, which reads the page (post + comment-section token) and the comment section, draws
  the post with its card, the comment total from `commentsHeaderRenderer.countText`, the comments
  with their reply toggle (replies fetched from the post's own browse token), and the empty/retry
  states. `CommunityPostLoadTest` and `PostScreenUiTest` cover the two-request load, populated,
  reply, empty and failed paths against the captured payloads.
- **Post actions.** The like button and comment composer need a Google session (see *Not
  portable*); the detail page draws the post's like count but does not toggle it.
- **Post gallery and author link.** Both are real now: every picture the renderer serves is a page
  of a swipeable gallery (one unbadged page for a single-image post), and the author row opens the
  author's channel through `authorEndpoint`'s browse id — no new traffic, both shapes were already
  in the captures above. A post whose renderer carries no author endpoint and no author run (an
  older copy) leaves the row unlinked rather than guessing a channel.)
- **Release shelves beyond the first page.** The tab is drawn in full for the releases YouTube
  serves in the one response; the per-shelf tokens that expand a carousel are not followed.
- **Album audio playlists (`OLAK5uy_…`).** A release card can name an album's audio playlist, and
  WEB_REMIX answers both `VL<id>` and `<id>` for such an id with an empty `contents`, so the page
  cannot load. Probed live on 2026-11-26 (the `Moon Music` album ids from a Coldplay releases page):
  the album's own `MPREb_…` page loads, the audio-playlist id does not. Shell headings are therefore
  not clickable for such ids rather than offering a link to a failed page; fixing it needs the
  album's `MPREb_…` browse id, which the lockup does not carry on the shelves captured so far.
- **Suggestions inside a channel** — the reference's channel search is a plain field, and so is the
  port's.

### Playback: `player_android_vr.json`

Captured 2026-11-23 with `POST https://www.youtube.com/youtubei/v1/player`, key
`AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8`, body
`{"context":{"client":{"clientName":"ANDROID_VR","clientVersion":"1.61.48","hl":"en","gl":"US",...}},"videoId":"dQw4w9WgXcQ","contentCheckOk":true,"racyCheckOk":true}`,
and the same `X-YouTube-Client-Name: 28` / `X-YouTube-Client-Version` / VR user agent headers the
shared client sends. Trimmed by removing sibling objects only: `streamingData.adaptiveFormats` kept
its first two entries, and unused top-level keys (`trackingParams`, `adBreakHeartbeatParams`) were
dropped; `formats`, `playabilityStatus`, `videoDetails`, `playerConfig` and `playbackTracking` are
untouched.

What it establishes:

- The WEB client answered `UNPLAYABLE` for this video without a PO token, while ANDROID_VR answered
  `OK` with a **direct** GVS URL for progressive itag 18 (360p, `avc1` + `mp4a`) **and** direct
  URLs for all 26 adaptive formats. No signature deciphering is needed, which is why the port
  resolves playback with this client: the reference app's cipher path and its NewPipe fallback are
  not part of shared code.
- A byte-range request to the captured URL returned `206` with `Content-Type: video/mp4`, so the
  URL is genuinely playable, not just present.
- The progressive format is the only one the port can hand to a single media engine: nothing here
  merges separate video and audio tracks. The captured response carries a 2160p VP9 adaptive track,
  which is exactly what the selector must *not* pick.
- Two decode bugs surfaced and were fixed in `PlayerResponse`: `playabilityStatus.reason` and the
  `Format` fields `contentLength`, `averageBitrate`, `loudnessDb`, `signatureCipher`, `width`,
  `height` (and the rest of the nullable set) were declared required, so a *successful* ANDROID_VR
  response threw `MissingFieldException`. A playable response omits all of them.

Playback itself is Android-only: `VideoPlayerSurface` is an `expect` with a Media3 `ExoPlayer` +
`PlayerView` actual in `androidMain`.

- **Client fallback.** Resolution tries `VISIONOS`, four ANDROID_VR builds, the stock `ANDROID`
  client, then IOS — the reference app's direct-client order, on the **main site**
  (`www.youtube.com`), not the music endpoint. The endpoint matters as much as the client list: for
  `0J2QdDbelmY` ("Seven Nation Army") the music host answered ANDROID `LOGIN_REQUIRED`, the old
  ladder fell through to IOS, and the IOS adaptive URL answered `403` to a range request past ~60 s
  while the main site served the same ANDROID client a muxed 360p file that returned `206` at 70 s
  and 150 s. What each client answers still depends on the video and YouTube's current gating: the
  same video can answer `LOGIN_REQUIRED` to one client and `OK` to another. `VISIONOS` is first
  because it is the only rung GVS serves without a PO token or an `n` parameter; it answers ordinary
  videos with 1080p/2160p adaptive tracks and refuses most music videos. The stock `ANDROID` client
  still answers the YouTube Music playlist tracks (for example `Dg7Z0YPlHY4`, "Saat Samundar Paar")
  with a muxed progressive `itag 18` URL that GVS serves, so it comes before IOS. IOS fills the
  remaining gap but its adaptive URLs are the ones GVS most often gates behind a PO token, so it is
  last.
- **Track selection.** A muxed progressive format wins when one exists. Otherwise the tallest
  H.264 track that decodes everywhere is paired with the best AAC audio track and ExoPlayer merges
  the two (`MergingMediaSource`). Pictures are capped at 1080p, and AV1 is the last codec choice
  because many devices and emulators cannot decode it.
- **User agent binding.** GVS binds a stream URL to the client that minted it: with the VR agent on
  an IOS-minted URL every request answered `403`, and the same URL fetched with the minting agent
  returned `206`. `PlayableStream` therefore carries the client's user agent and the Media3 data
  source presents it.
- **Verified on a device.** On an arm64 Android 10 emulator: the app launched to a live home feed,
  Search returned results, tapping one opened the player, ExoPlayer resolved and played, and the
  picture advanced (a temporary progress ticker logged `pos=312 playing=true state=3`,
  `pos=4160 playing=true`, `pos=5799`). On the final APK without the ticker, three screenshots taken
  six seconds apart show different frames of the same video and logcat contains no app error.
  Playback is not universal: videos YouTube serves without a PO token play, while gated ones answer
  `403` from GVS on every attempt and leave the poster showing. Porting the reference app's
  `PoTokenWebView` BotGuard flow is the remaining work for those.
- **Playlist path, also verified on that emulator.** Home's "90s Bollywood Dance" card opened a
  playlist screen that listed the captured 59-song header and its rows with artwork, artists and
  lengths; tapping "Saat Samundar Paar" opened the player at `00:12 / 06:26` and it advanced to
  `00:24 / 06:26` with different frames and no `FlowPlayback` error. A search result ("Full 2025
  Kotlin Crash Course For Beginners") still played at `00:11` and `00:22` in the same run.
- The desktop and iOS actuals report `isVideoPlaybackSupported = false`, so those targets keep the
  poster instead of showing an empty black surface. Known limits, on purpose: no background audio,
  media session, notification, PiP or casting yet, and no quality picker.

### Playback: `player_visionos_direct.json`, `player_android_main_0J2.json`, `player_visionos_refused_0J2.json`

Captured 2026-11-23 with the same endpoint and key as the VR fixture, on `www.youtube.com`:

- `player_visionos_direct.json` — `clientName: VISIONOS`, `clientVersion: 1.02`, `videoId:
  dQw4w9WgXcQ`. `playabilityStatus` `OK`; **no** `streamingData.formats`, 27 adaptive tracks with
  direct GVS URLs. `streamingData.adaptiveFormats` was trimmed to the 1080p and 720p `avc1` tracks
  plus one `mp4a` audio track; `playabilityStatus` and `videoDetails` are untouched.
- `player_android_main_0J2.json` — `clientName: ANDROID`, `clientVersion: 21.03.38`, `videoId:
  0J2QdDbelmY`. `playabilityStatus` `OK`; one progressive `itag 18` (360p, `avc1` + `mp4a`) with a
  direct URL, which the test asserts the selector prefers. `contentLength` is absent in the capture
  (the response omitted it), which is why the model keeps it nullable.
- `player_visionos_refused_0J2.json` — the same VISIONOS client on `0J2QdDbelmY`:
  `playabilityStatus` `LOGIN_REQUIRED`, reason "Sign in to confirm you're not a bot", no
  `streamingData`. It is the real refusal the ladder has to skip; the test routes VISIONOS and the
  VR builds to this file and ANDROID to the main-site answer above, then asserts the resolved stream
  is the captured muxed file and every request went to `www.youtube.com`.

Every top-level object in these files is the client's own captured answer (`responseContext`,
`playabilityStatus`, `streamingData`, `playbackTracking`, `playerConfig`, `videoDetails`); only
sibling `streamingData` formats and unused top-level keys were removed, as in the VR fixture. The
VISIONOS capture has no `responseContext.serviceTrackingParams` and its `playerConfig.audioConfig`
omits `loudnessDb`/`perceptualLoudnessDb`. Decoding it exposed those fields as required-but-nullable
in the shared model — a real decode failure the app would have hit on its new first rung — so they
took `null` defaults, together with `PlayerResponse.playbackTracking`. With that repair the genuine
captures decode unchanged; no section of any fixture is borrowed from another response.

What the captures establish:

- The failing path was **endpoint and client selection**, not a missing PO token on the rung we
  request: ANDROID/IOS/VR all returned formats with `url` and no `signatureCipher` (cipher count 0 in
  every sampled response), so signature deciphering is not on this path. The gate that actually bit
  was GVS refusing an IOS-minted adaptive URL mid-playback (`403` at byte offset ≈ bitrate/8 × 70 s),
  while the main-site ANDROID muxed URL answered `206` at 0 s, 70 s and 150 s.
- VISIONOS serves a 2160p adaptive set for an ungated video; the port still caps at 1080p and pairs
  the video track with audio, as `PlayableStream` already did for IOS.
- This is the reference app's own fast path: `InnerTubeVideoStreamExtractor` asks VISIONOS first with
  `apiUrl = YouTubeClient.API_URL_YOUTUBE`, then the app clients. The BotGuard/PO-token and SABR
  paths remain app-only (see below).

### Not portable, with the reason and what remains

These are the reference surfaces that are still app-only. They are not stubbed or faked in shared:

- **Background playback, media session, notification controls, PiP and casting.** Android
  foreground playback is in place (Media3 `ExoPlayer` in `androidMain`), but the reference's
  `MediaSessionService`, notification, PiP and Cast wiring is not. Remains: a media session service
  and the notification/PiP/Cast integrations. Also not ported, with the measured reason each one is
  not on the direct path: **signature deciphering** (`SignatureCipher` + the reference's remote `n`
  decoder) because every direct-client response sampled here returned `url` and no
  `signatureCipher`; **SABR/`serverAbrStreamingUrl`** because WEB/MWEB with a valid
  `signatureTimestamp` answer `OK` with SABR-only formats and no direct URLs, which needs a UMP
  client, not a URL; **BotGuard/PO-token minting** (`PoTokenWebView`, `TizenStreamResolver`) because
  a third-party Android build has no Django/DroidGuard attestation and TV_TIZEN answered
  `LOGIN_REQUIRED "Sign in to confirm you're not a bot"` in this environment; and the reference's
  visitor **re-roll** after a player wall, which is the fallback that would help when every client
  refuses at once. Videos that require a Google session (age-gated, members-only, or the IDs YouTube
  walls for every client) still leave the poster showing.
- **Downloads as media files (WorkManager, app storage, progress UI).** The shared app persists the
  saved/downloaded *state* in Room, but the reference downloads actual media through WorkManager and
  file storage, both Android-only. Remains: a download worker and file store per platform.
- **Music playback, queue and lyrics.** Reference uses Media3 audio, a shared queue ViewModel and
  LRC parsing against the player manager; no shared playback engine exists. The music home feed is
  ported, the playback side is not.
- **Account features (Sign in, profile switching, likes/subscribes, comment posting, library
  shelves from YouTube Music).** These need a Google session: cookies plus `SAPISIDHASH` request
  signing, and the signed-in library landing. The workspace has no credentials and cannot obtain
  them; the signed-out library response is captured above and renders as YouTube's own sign-in
  notice. Remains: authenticated mutation clients once a session exists.
- **Sync (device-to-device), casting, notifications, lock-screen controls, widgets (Glance), Android
  TV screens, equalizer, audio recognition, recap/stats, notes, playlist CRUD, history and
  subscriptions.** Each depends on an Android-only facility (sockets + service, Cast SDK, Glance,
  `android.media`, `AudioRecord`, Leanback) or on the cloud-backed account data above. Remains:
  platform modules with their own tests, out of scope for the shared UI port.
- **Settings, onboarding, update checker and theming persistence.** Reference stores these in
  DataStore with Android flows; the shared shell uses one fixed SYSTEM theme and a single profile
  placeholder. Remains: a shared preferences store plus a settings surface.
