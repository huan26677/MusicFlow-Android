# MusicFlow — Modular Test Catalog & Impact Map

> Generated from actual `@Test` methods by `py -3 scripts/impact_regression.py catalog`.
> Canonical QA map: `qa/impact_modules.json`. The cross-session brain points here; this is not another brain.

**Selection rule:** direct module = all cases; transitive dependent module = smoke/contract cases;
shared build/core contract/unknown/high-risk = full review; mapped UI component changes select downstream consumers. Tests require their appropriate runner; this catalog
does not attest that they passed. IDs are `MODULE/Class#method` and do not renumber.

## Module coverage summary

| Module | Area | Cases linked | Depends on |
|---|---|---:|---|
| `MEDIA_SAF` | MediaStore, tệp offline và quyền SAF | 30 |  |
| `LIBRARY` | Thư viện, album, nghệ sĩ, thể loại | 18 | MEDIA_SAF, FAVORITES, UI_ACCESS |
| `SEARCH` | Tìm kiếm không dấu, chỉ mục, lọc nhóm | 25 | MEDIA_SAF, LIBRARY, UI_ACCESS |
| `PLAYBACK` | Player service, hàng đợi, runtime, audio focus | 87 | MEDIA_SAF, ONLINE_RIGHTS, UI_ACCESS |
| `PLAYLIST` | Playlist Room, reorder, import/export | 50 | MEDIA_SAF, PLAYBACK, UI_ACCESS |
| `FAVORITES` | Yêu thích bài hát và album | 17 | MEDIA_SAF, UI_ACCESS |
| `SMART` | Smart Library và gợi ý thư viện | 19 | LIBRARY, FAVORITES, HISTORY, UI_ACCESS |
| `HISTORY` | Lịch sử nghe và Room recent | 17 | PLAYBACK |
| `LYRICS` | Lời bài hát LRC, đồng bộ lời và SAF | 9 | MEDIA_SAF, PLAYBACK, UI_ACCESS |
| `AUTH` | Đăng nhập, khách, OAuth, hồ sơ | 40 | UI_ACCESS |
| `ROLES_ADMIN` | Owner, quản trị viên, Developer và RBAC | 74 | AUTH, UI_ACCESS |
| `CREATOR` | Creator, draft, xét duyệt nội dung | 23 | AUTH, ROLES_ADMIN, UI_ACCESS |
| `BILLING_DEMO` | Premium và thanh toán giả lập | 17 | AUTH, ROLES_ADMIN, UI_ACCESS |
| `CLOUD_SYNC` | Cloud library/history và đồng bộ Supabase demo | 8 | AUTH, PLAYLIST, HISTORY |
| `ONLINE_RIGHTS` | Online catalog và quyền phát có giấy phép | 16 | AUTH, UI_ACCESS |
| `HOME_NAV` | Trang chủ, mini-player, điều hướng | 18 | LIBRARY, SEARCH, PLAYBACK, HISTORY, UI_ACCESS |
| `UI_ACCESS` | Thiết kế Ocean Flow, focus, TalkBack semantics | 44 |  |
| `DEMO_CENTER` | Trung tâm demo và điều hướng theo tài khoản | 11 | AUTH, ROLES_ADMIN, BILLING_DEMO |
| `RELEASE_BACKUP` | Sao lưu, versioning, APK ký QA và nâng cấp | 15 | MEDIA_SAF, PLAYLIST, FAVORITES |
| `COMMUNITY_RATING` | Chấm sao 1–5 và dữ liệu Demo cộng đồng | 11 | ONLINE_RIGHTS |
| `COMMUNITY_COMMENTS` | Đọc, đăng, sửa, xóa bình luận và báo cáo | 23 | COMMUNITY_RATING, AUTH, UI_ACCESS |
| `COMMUNITY_MODERATION` | Kiểm duyệt bình luận, phân quyền và nhật ký | 11 | COMMUNITY_COMMENTS, ROLES_ADMIN |

## Test cases by module

### MEDIA_SAF — MediaStore, tệp offline và quyền SAF

**Source paths:** `app/src/main/java/com/example/musicflow/core/media/**`

**Downstream contract/smoke:** `MEDIA_SAF/OfflineDiscoveryMediaStoreTest#scansLongWavInMusicWhenProviderInitiallyMarksItNonMusic`, `MEDIA_SAF/RealPickerSafTest#realOpenMultipleDocumentsGrantsPersistedReadAndPlays`, `MEDIA_SAF/SafPermissionRestartTest#verifySafAfterForceStop`.

**OfflineDiscoveryMediaStoreTest** (`app/src/androidTest/java/com/example/musicflow/core/media/OfflineDiscoveryMediaStoreTest.kt`; android)

- `MEDIA_SAF/OfflineDiscoveryMediaStoreTest#lightweightScanSkipsGenreLookupButStillFindsRealMusic`
- `MEDIA_SAF/OfflineDiscoveryMediaStoreTest#scansLongWavInMusicWhenProviderInitiallyMarksItNonMusic`

**TrackMediaItemsTest** (`app/src/androidTest/java/com/example/musicflow/core/media/TrackMediaItemsTest.kt`; android)

- `MEDIA_SAF/TrackMediaItemsTest#deviceMediaItemRetainsLegacyIdAndMetadata`
- `MEDIA_SAF/TrackMediaItemsTest#oldStoredOnlineUrlsCannotBypassRights`
- `MEDIA_SAF/TrackMediaItemsTest#onlineRejectsNonHttpsAndCredentialOrFragmentUrls`
- `MEDIA_SAF/TrackMediaItemsTest#onlineSelectionIsStableWithDuplicateIdsAndUnplayableTracks`
- `MEDIA_SAF/TrackMediaItemsTest#onlineUsesSameIdAndPreservesSignedQuery`
- `MEDIA_SAF/TrackMediaItemsTest#personalDownloadedOfflineFilesPlayEvenWithLegacyOnlineIds`
- `MEDIA_SAF/TrackMediaItemsTest#pickerAndStoredItemsRetainExactUriAndId`
- `MEDIA_SAF/TrackMediaItemsTest#playlistSelectionKeepsLocalIndicesAndSkipsOldOnlineRecords`
- `MEDIA_SAF/TrackMediaItemsTest#unverifiedOnlineIsRejectedEvenWithHttpsUrl`

**RealPickerSafTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt`; android)

- `MEDIA_SAF/RealPickerSafTest#realOpenMultipleDocumentsGrantsPersistedReadAndPlays`
- `MEDIA_SAF/RealPickerSafTest#realOpenMultipleDocumentsSelectsTwoFilesAndNavigates`

**SafPermissionRestartTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/SafPermissionRestartTest.kt`; android)

- `MEDIA_SAF/SafPermissionRestartTest#cleanupSafDocument`
- `MEDIA_SAF/SafPermissionRestartTest#prepareSafDocument`
- `MEDIA_SAF/SafPermissionRestartTest#verifySafAfterForceStop`

**OfflineReliabilityUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/OfflineReliabilityUiTest.kt`; android)

- `MEDIA_SAF/OfflineReliabilityUiTest#backupToolsCollapseByDefaultAndExpandOnDemand`
- `MEDIA_SAF/OfflineReliabilityUiTest#relinkActionIsVisibleAndDoesNotTriggerDeleteAtLargeFonts`

**PlaylistQuickAddRealSafUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/PlaylistQuickAddRealSafUiTest.kt`; android)

- `MEDIA_SAF/PlaylistQuickAddRealSafUiTest#realOpenDocumentAddsPersistedReadableTrackToSelectedPlaylist`

**MediaSourceContractTest** (`app/src/test/java/com/example/musicflow/core/media/MediaSourceContractTest.kt`; jvm)

- `MEDIA_SAF/MediaSourceContractTest#deviceLegacyMediaIdIsStable`
- `MEDIA_SAF/MediaSourceContractTest#explicitDeviceMediaIdIsNotOverwritten`
- `MEDIA_SAF/MediaSourceContractTest#explicitFileUriClassifiesAsPickedAudio`
- `MEDIA_SAF/MediaSourceContractTest#onlineIdCannotBeEmpty`
- `MEDIA_SAF/MediaSourceContractTest#onlineIdUsesExistingPrefix`
- `MEDIA_SAF/MediaSourceContractTest#pickerCannotBecomeArbitraryRemoteUrl`
- `MEDIA_SAF/MediaSourceContractTest#pickerCompatibilityAcceptsLegacyFileUris`
- `MEDIA_SAF/MediaSourceContractTest#pickerIdIsTheExistingUriString`
- `MEDIA_SAF/MediaSourceContractTest#storedKnownIdsArePreserved`
- `MEDIA_SAF/MediaSourceContractTest#storedLegacyIdIsPreserved`
- `MEDIA_SAF/MediaSourceContractTest#syntheticFavoriteUsingRawContentIdClassifiesAsSaf`


### LIBRARY — Thư viện, album, nghệ sĩ, thể loại

**Source paths:** `app/src/main/java/com/example/musicflow/feature/library/**`, `app/src/main/java/com/example/musicflow/core/catalog/**`, `app/src/main/java/com/example/musicflow/feature/catalog/**`

**Downstream contract/smoke:** `LIBRARY/LibraryPerformanceV4UiTest#songSearchPlaysOnlyFilteredQueueAndPreservesIndex`, `LIBRARY/LibraryAccessibilityV7Test#albumTracksStayVirtualizedInRealLibraryAt200Percent`.

**AdvancedLibraryRuntimeTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/AdvancedLibraryRuntimeTest.kt`; android)

- `LIBRARY/AdvancedLibraryRuntimeTest#mediaStoreAlbumsArtistsAndGenresOpenGroupsAndPlay`
- `LIBRARY/AdvancedLibraryRuntimeTest#realMediaStoreLibraryRouteScansAndStartsPlayback`

**LibraryAccessibilityV7Test** (`app/src/androidTest/java/com/example/musicflow/feature/library/LibraryAccessibilityV7Test.kt`; android)

- `LIBRARY/LibraryAccessibilityV7Test#albumRowsPreservePlaybackIndicesAndDoNotCreateDuplicateKeys`
- `LIBRARY/LibraryAccessibilityV7Test#albumTracksStayVirtualizedInRealLibraryAt200Percent`
- `LIBRARY/LibraryAccessibilityV7Test#artistTracksStayVirtualizedAndKeepCorrectPlaybackQueueAt200Percent`
- `LIBRARY/LibraryAccessibilityV7Test#collapsedCollectionsDoNotCreateHundredsOfSongRows`
- `LIBRARY/LibraryAccessibilityV7Test#darkThemePrimaryTextTokensMeetWcagAa`
- `LIBRARY/LibraryAccessibilityV7Test#lightThemePrimaryTextTokensMeetWcagAa`

**LibraryPerformanceV4UiTest** (`app/src/androidTest/java/com/example/musicflow/ui/LibraryPerformanceV4UiTest.kt`; android)

- `LIBRARY/LibraryPerformanceV4UiTest#deniedLibraryPermissionOffersFriendlyOfflineAlternative`
- `LIBRARY/LibraryPerformanceV4UiTest#genreTabRequestsMetadataOnlyAfterItIsSelected`
- `LIBRARY/LibraryPerformanceV4UiTest#largeFontSearchAndSortRemainReachableInOneScrollableList`
- `LIBRARY/LibraryPerformanceV4UiTest#noResultsHaveClearActionAndSearchDoesNotDeleteMusic`
- `LIBRARY/LibraryPerformanceV4UiTest#pickedMusicSearchIsAccentInsensitiveAndQueueMatchesFilter`
- `LIBRARY/LibraryPerformanceV4UiTest#preparedIndexMatchesVietnameseAndPreservesLargeCatalogOrder`
- `LIBRARY/LibraryPerformanceV4UiTest#songSearchPlaysOnlyFilteredQueueAndPreservesIndex`

**MusicFlowPlayerLibraryUxV3Test** (`app/src/androidTest/java/com/example/musicflow/ui/MusicFlowPlayerLibraryUxV3Test.kt`; android)

- `LIBRARY/MusicFlowPlayerLibraryUxV3Test#advancedPlayerControlsStartCollapsedAndExpandOnDemand`
- `LIBRARY/MusicFlowPlayerLibraryUxV3Test#librarySortMenuChangesPlaybackOrderWithoutEditingSongs`
- `LIBRARY/MusicFlowPlayerLibraryUxV3Test#sortingUsesActualMediaStoreDatesAndKeepsDeterministicOrders`


### SEARCH — Tìm kiếm không dấu, chỉ mục, lọc nhóm

**Source paths:** `app/src/main/java/com/example/musicflow/feature/search/**`

**Downstream contract/smoke:** `SEARCH/AdvancedSearchIndexV8Test#refreshRevocationAndQueryChangesDoNotReuseStaleCatalog`, `SEARCH/AdvancedSearchRuntimeTest#searchRouteAccentInsensitivePlaybackAndQueueActions`.

**AdvancedSearchRuntimeTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/AdvancedSearchRuntimeTest.kt`; android)

- `SEARCH/AdvancedSearchRuntimeTest#searchRouteAccentInsensitivePlaybackAndQueueActions`
- `SEARCH/AdvancedSearchRuntimeTest#searchRouteAlbumArtistAndGenreFilterTabsUseRealScanner`

**AdvancedSearchIndexV8Test** (`app/src/androidTest/java/com/example/musicflow/feature/search/AdvancedSearchIndexV8Test.kt`; android)

- `SEARCH/AdvancedSearchIndexV8Test#blankQueriesNeverExposeAllCatalogItems`
- `SEARCH/AdvancedSearchIndexV8Test#filteringSameCatalogReusesOriginalGroupObjectWithoutRebuilding`
- `SEARCH/AdvancedSearchIndexV8Test#largeCatalogSearchIsCorrectAndKeepsPlaybackQueueOrder`
- `SEARCH/AdvancedSearchIndexV8Test#precomputedGroupMatchesPreserveAccentInsensitiveAndWordOrder`
- `SEARCH/AdvancedSearchIndexV8Test#refreshRevocationAndQueryChangesDoNotReuseStaleCatalog`

**AccessibilityOfflineSearchV5Test** (`app/src/androidTest/java/com/example/musicflow/ui/AccessibilityOfflineSearchV5Test.kt`; android)

- `SEARCH/AccessibilityOfflineSearchV5Test#compactSongRowMakesFavoritesAndPlaylistEasyToReach`
- `SEARCH/AccessibilityOfflineSearchV5Test#missingPermissionShowsTwoDistinctOptInPaths`
- `SEARCH/AccessibilityOfflineSearchV5Test#narrowLibraryDoesNotForcePermissionAndShowsBothActions`
- `SEARCH/AccessibilityOfflineSearchV5Test#noPermissionStillSearchesAndPlaysOnlySelectedSafResults`
- `SEARCH/AccessibilityOfflineSearchV5Test#offlineSearchRepositoryMergesDeduplicatesAndRespectsRevocation`
- `SEARCH/AccessibilityOfflineSearchV5Test#unavailableMediaStoreFallsBackToReadablePickedDocuments`

**MusicFlowSearchUxV3Test** (`app/src/androidTest/java/com/example/musicflow/ui/MusicFlowSearchUxV3Test.kt`; android)

- `SEARCH/MusicFlowSearchUxV3Test#categoryCountsGuideUserToAlbumWhenNoDirectSongMatches`
- `SEARCH/MusicFlowSearchUxV3Test#noScannedMusicGuidesUserToLibrary`
- `SEARCH/MusicFlowSearchUxV3Test#permissionScreenOffersManualFileAlternative`
- `SEARCH/MusicFlowSearchUxV3Test#searchHeaderRemainsUsableWithLargeTextAndNarrowWidth`

**SongSearchTest** (`app/src/test/java/com/example/musicflow/feature/search/SongSearchTest.kt`; jvm)

- `SEARCH/SongSearchTest#emptyQueryDoesNotMatch`
- `SEARCH/SongSearchTest#ignoreExtraSpaces`
- `SEARCH/SongSearchTest#ignoreUppercase`
- `SEARCH/SongSearchTest#normalizeVietnameseLetterD`
- `SEARCH/SongSearchTest#searchByArtist`
- `SEARCH/SongSearchTest#searchBySongTitle`
- `SEARCH/SongSearchTest#searchMultipleWordsInDifferentOrder`
- `SEARCH/SongSearchTest#unrelatedQueryDoesNotMatch`


### PLAYBACK — Player service, hàng đợi, runtime, audio focus

**Source paths:** `app/src/main/java/com/example/musicflow/core/playback/**`, `app/src/main/java/com/example/musicflow/feature/player/**`

**Downstream contract/smoke:** `PLAYBACK/PlaybackQueuePersistenceTest#savedQueueRestoresPausedAndResumesPlayback`, `PLAYBACK/PlaybackRuntimeAutoSkipTest#singleMissingTrackAutoSkips`.

**AdvancedLibraryRuntimeTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/AdvancedLibraryRuntimeTest.kt`; android)

- `PLAYBACK/AdvancedLibraryRuntimeTest#mediaStoreAlbumsArtistsAndGenresOpenGroupsAndPlay`
- `PLAYBACK/AdvancedLibraryRuntimeTest#realMediaStoreLibraryRouteScansAndStartsPlayback`

**AdvancedSearchRuntimeTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/AdvancedSearchRuntimeTest.kt`; android)

- `PLAYBACK/AdvancedSearchRuntimeTest#searchRouteAccentInsensitivePlaybackAndQueueActions`
- `PLAYBACK/AdvancedSearchRuntimeTest#searchRouteAlbumArtistAndGenreFilterTabsUseRealScanner`

**BackgroundControlsTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/BackgroundControlsTest.kt`; android)

- `PLAYBACK/BackgroundControlsTest#notificationButtonsControlPlaybackV2`
- `PLAYBACK/BackgroundControlsTest#playbackSurvivesBackgroundAndSystemControls`
- `PLAYBACK/BackgroundControlsTest#transientAudioFocusLossV2`

**CrossfadeRuntimeTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/CrossfadeRuntimeTest.kt`; android)

- `PLAYBACK/CrossfadeRuntimeTest#disablingCrossfadeDuringMixingRestoresNormalPlayback`
- `PLAYBACK/CrossfadeRuntimeTest#editingQueueDuringMixingCancelsSecondaryWithoutGhostItem`
- `PLAYBACK/CrossfadeRuntimeTest#enabledCrossfadeMixesAndHandsOffAtOneTwoThreeSeconds`
- `PLAYBACK/CrossfadeRuntimeTest#pauseDuringMixingRestoresPrimaryAndKeepsPosition`
- `PLAYBACK/CrossfadeRuntimeTest#seekingBackwardDuringMixingCancelsSecondary`
- `PLAYBACK/CrossfadeRuntimeTest#transitionCommandValidatesAndPersistsSupportedDurations`
- `PLAYBACK/CrossfadeRuntimeTest#variableSpeedFallsBackToOrdinaryForwardTransition`
- `PLAYBACK/CrossfadeRuntimeTest#zeroDurationUsesNaturalMedia3ForwardTransition`

**CrossfadeUiE2eTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/CrossfadeUiE2eTest.kt`; android)

- `PLAYBACK/CrossfadeUiE2eTest#nowPlayingCrossfadeChipsReachRealMediaSession`

**EqualizerRuntimeTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/EqualizerRuntimeTest.kt`; android)

- `PLAYBACK/EqualizerRuntimeTest#enableBandPersistAndControllerReconnect`
- `PLAYBACK/EqualizerRuntimeTest#validationAndPlaybackContinuity`

**OfflineAcceptanceFlowTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/OfflineAcceptanceFlowTest.kt`; android)

- `PLAYBACK/OfflineAcceptanceFlowTest#mediaStorePlaylistServiceHistoryAndQueueRestoreOffline`

**PlaybackProcessRestartTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackProcessRestartTest.kt`; android)

- `PLAYBACK/PlaybackProcessRestartTest#cleanupAfterRestart`
- `PLAYBACK/PlaybackProcessRestartTest#prepareBeforeRestart`
- `PLAYBACK/PlaybackProcessRestartTest#verifyAfterRestart`

**PlaybackQueueHardeningTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackQueueHardeningTest.kt`; android)

- `PLAYBACK/PlaybackQueueHardeningTest#deletedSelectedFileIsRemoved`
- `PLAYBACK/PlaybackQueueHardeningTest#deniedContentAccessClearsUnplayableQueue`
- `PLAYBACK/PlaybackQueueHardeningTest#malformedSnapshotIsCleared`
- `PLAYBACK/PlaybackQueueHardeningTest#validSelectionRetainsPlaybackPosition`

**PlaybackQueuePersistenceTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackQueuePersistenceTest.kt`; android)

- `PLAYBACK/PlaybackQueuePersistenceTest#corruptedSnapshotDoesNotCrashPlayer`
- `PLAYBACK/PlaybackQueuePersistenceTest#downloadedOfflineTrackWithOldOnlineIdRestoresNormally`
- `PLAYBACK/PlaybackQueuePersistenceTest#legacyOnlineSnapshotCannotAutoResumeWithoutRights`
- `PLAYBACK/PlaybackQueuePersistenceTest#reorderedRoomPlaylistKeepsOrderInSnapshot`
- `PLAYBACK/PlaybackQueuePersistenceTest#savedQueueRestoresPausedAndResumesPlayback`

**PlaybackRuntimeAutoSkipTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackRuntimeAutoSkipTest.kt`; android)

- `PLAYBACK/PlaybackRuntimeAutoSkipTest#allMissingTracksStopWithoutLoop`
- `PLAYBACK/PlaybackRuntimeAutoSkipTest#consecutiveMissingTracksAutoSkip`
- `PLAYBACK/PlaybackRuntimeAutoSkipTest#injectedErrorExercisesRecoveryBranches`
- `PLAYBACK/PlaybackRuntimeAutoSkipTest#pauseWhileRecoveryPendingCancelsSkip`
- `PLAYBACK/PlaybackRuntimeAutoSkipTest#pausedPlayerDoesNotAutoSkip`
- `PLAYBACK/PlaybackRuntimeAutoSkipTest#queueReplacementWhilePendingCancelsSkip`
- `PLAYBACK/PlaybackRuntimeAutoSkipTest#realServiceReceivesExoPlayerError`
- `PLAYBACK/PlaybackRuntimeAutoSkipTest#singleMissingTrackAutoSkips`

**PlaybackRuntimeRaceHardeningTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackRuntimeRaceHardeningTest.kt`; android)

- `PLAYBACK/PlaybackRuntimeRaceHardeningTest#disposeInvalidatesPendingRecovery`
- `PLAYBACK/PlaybackRuntimeRaceHardeningTest#identicalQueueReplacementInvalidatesPendingRecovery`
- `PLAYBACK/PlaybackRuntimeRaceHardeningTest#noNewIntentStillSkipsNextTrack`
- `PLAYBACK/PlaybackRuntimeRaceHardeningTest#pauseThenResumeInvalidatesPendingRecovery`
- `PLAYBACK/PlaybackRuntimeRaceHardeningTest#seekWithinSameIndexInvalidatesPendingRecovery`

**PlaybackServiceIntegrationTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackServiceIntegrationTest.kt`; android)

- `PLAYBACK/PlaybackServiceIntegrationTest#mvpHomePlaylistNowPlayingThroughMediaSession`
- `PLAYBACK/PlaybackServiceIntegrationTest#mvpPickerResultStartsRealPlayback`
- `PLAYBACK/PlaybackServiceIntegrationTest#playbackServiceIntegration`

**PlaybackSkipRecoveryTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackSkipRecoveryTest.kt`; android)

- `PLAYBACK/PlaybackSkipRecoveryTest#missingFileCanSkipToValidTrack`
- `PLAYBACK/PlaybackSkipRecoveryTest#noNextTrackDoesNotSkip`

**PlaybackSpeedRuntimeTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackSpeedRuntimeTest.kt`; android)

- `PLAYBACK/PlaybackSpeedRuntimeTest#nowPlayingSpeedChipsControlRealMediaSession`
- `PLAYBACK/PlaybackSpeedRuntimeTest#physicalTouchOnSpeedChipsWorksAcrossHorizontalScroll`
- `PLAYBACK/PlaybackSpeedRuntimeTest#playbackControllerRejectsInvalidSpeeds`

**QueueActionsRuntimeTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/QueueActionsRuntimeTest.kt`; android)

- `PLAYBACK/QueueActionsRuntimeTest#libraryPlayNextThenSearchAppendPreservesPlaybackAndSnapshot`
- `PLAYBACK/QueueActionsRuntimeTest#nextRejectsShuffleRepeatOneAndEmptyQueueWithoutInterruptingAudio`

**QueueManagerIntegrationTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/QueueManagerIntegrationTest.kt`; android)

- `PLAYBACK/QueueManagerIntegrationTest#reorderRemoveClearKeepCurrentPlayingAndPersisted`

**QueueManagerScreenTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/QueueManagerScreenTest.kt`; android)

- `PLAYBACK/QueueManagerScreenTest#actionsUseCurrentTimelineVersionAndCurrentRowIsProtected`
- `PLAYBACK/QueueManagerScreenTest#clearingRequiresConfirmationAndKeepsCurrent`

**RealPickerSafTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt`; android)

- `PLAYBACK/RealPickerSafTest#realOpenMultipleDocumentsGrantsPersistedReadAndPlays`
- `PLAYBACK/RealPickerSafTest#realOpenMultipleDocumentsSelectsTwoFilesAndNavigates`

**SafPermissionRestartTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/SafPermissionRestartTest.kt`; android)

- `PLAYBACK/SafPermissionRestartTest#cleanupSafDocument`
- `PLAYBACK/SafPermissionRestartTest#prepareSafDocument`
- `PLAYBACK/SafPermissionRestartTest#verifySafAfterForceStop`

**SleepTimerRuntimeTest** (`app/src/androidTest/java/com/example/musicflow/core/playback/SleepTimerRuntimeTest.kt`; android)

- `PLAYBACK/SleepTimerRuntimeTest#cancellingActiveShortTimerDoesNotPauseOfflineMusic`
- `PLAYBACK/SleepTimerRuntimeTest#expiryPausesRealMediaSessionWithoutClearingOfflineQueue`
- `PLAYBACK/SleepTimerRuntimeTest#validatesSetQueryReplaceAndCancel`

**MusicFlowPlayerLibraryUxV3Test** (`app/src/androidTest/java/com/example/musicflow/ui/MusicFlowPlayerLibraryUxV3Test.kt`; android)

- `PLAYBACK/MusicFlowPlayerLibraryUxV3Test#advancedPlayerControlsStartCollapsedAndExpandOnDemand`
- `PLAYBACK/MusicFlowPlayerLibraryUxV3Test#librarySortMenuChangesPlaybackOrderWithoutEditingSongs`
- `PLAYBACK/MusicFlowPlayerLibraryUxV3Test#sortingUsesActualMediaStoreDatesAndKeepsDeterministicOrders`

**PlaybackErrorTextTest** (`app/src/test/java/com/example/musicflow/core/playback/PlaybackErrorTextTest.kt`; jvm)

- `PLAYBACK/PlaybackErrorTextTest#missingFileHasVietnameseMessage`
- `PLAYBACK/PlaybackErrorTextTest#missingPermissionHasVietnameseMessage`
- `PLAYBACK/PlaybackErrorTextTest#unknownErrorHasFallbackMessage`

**QueueEditRulesTest** (`app/src/test/java/com/example/musicflow/core/playback/QueueEditRulesTest.kt`; jvm)

- `PLAYBACK/QueueEditRulesTest#cannotMoveOutOfBoundsOrToSamePlace`
- `PLAYBACK/QueueEditRulesTest#clearOthersKeepsCurrent`
- `PLAYBACK/QueueEditRulesTest#currentItemIsFixedAndCannotBeCrossed`
- `PLAYBACK/QueueEditRulesTest#currentTrackCannotBeRemoved`
- `PLAYBACK/QueueEditRulesTest#futureTracksMoveWithinFutureSegment`
- `PLAYBACK/QueueEditRulesTest#invalidOrEmptyQueueCannotBeEdited`
- `PLAYBACK/QueueEditRulesTest#pastTracksMoveWithinPastSegment`
- `PLAYBACK/QueueEditRulesTest#shuffleDisablesReorder`

**QueueInsertRulesTest** (`app/src/test/java/com/example/musicflow/core/playback/QueueInsertRulesTest.kt`; jvm)

- `PLAYBACK/QueueInsertRulesTest#emptyQueueIsRejected`
- `PLAYBACK/QueueInsertRulesTest#endAlwaysAppendsEvenUnderShuffle`
- `PLAYBACK/QueueInsertRulesTest#invalidIndexIsRejected`
- `PLAYBACK/QueueInsertRulesTest#nextAfterLastAppends`
- `PLAYBACK/QueueInsertRulesTest#nextInsertsAfterCurrentNotAtEnd`
- `PLAYBACK/QueueInsertRulesTest#nextUnderRepeatOneIsRejectedRatherThanMisleading`
- `PLAYBACK/QueueInsertRulesTest#nextUnderShuffleIsRejectedRatherThanMisleading`
- `PLAYBACK/QueueInsertRulesTest#oneTrackQueueCanGrow`


### PLAYLIST — Playlist Room, reorder, import/export

**Source paths:** `app/src/main/java/com/example/musicflow/core/playlist/**`, `app/src/main/java/com/example/musicflow/feature/playlist/**`

**Downstream contract/smoke:** `PLAYLIST/PlaylistReorderDatabaseTest#reorderPersistsAfterDatabaseReopen`, `PLAYLIST/PlaylistLocalBackupIntegrationTest#invalidDocumentCannotPartiallyModifyRoom`.

**PlaylistLocalBackupIntegrationTest** (`app/src/androidTest/java/com/example/musicflow/core/playlist/PlaylistLocalBackupIntegrationTest.kt`; android)

- `PLAYLIST/PlaylistLocalBackupIntegrationTest#atomicImportCreatesCopiesWithSameOrderAndLeavesOriginalsUntouched`
- `PLAYLIST/PlaylistLocalBackupIntegrationTest#invalidDocumentCannotPartiallyModifyRoom`
- `PLAYLIST/PlaylistLocalBackupIntegrationTest#jsonRoundTripPreservesVietnameseAndOrderedTrackMetadata`
- `PLAYLIST/PlaylistLocalBackupIntegrationTest#onDemandFileCheckDistinguishesReadableLocalFileAndMissingFile`
- `PLAYLIST/PlaylistLocalBackupIntegrationTest#rejectsCorruptionWrongVersionAndOversizedInput`
- `PLAYLIST/PlaylistLocalBackupIntegrationTest#relinkingPreservesTrackPositionAndRejectsDuplicateOrUnknown`

**PlaylistMigrationTest** (`app/src/androidTest/java/com/example/musicflow/core/playlist/PlaylistMigrationTest.kt`; android)

- `PLAYLIST/PlaylistMigrationTest#migration1To2PreservesRecentHistory`

**PlaylistPlaybackOrderTest** (`app/src/androidTest/java/com/example/musicflow/core/playlist/PlaylistPlaybackOrderTest.kt`; android)

- `PLAYLIST/PlaylistPlaybackOrderTest#playlistReorderControlsMedia3Playback`
- `PLAYLIST/PlaylistPlaybackOrderTest#playlistUiCreateRenameDelete`
- `PLAYLIST/PlaylistPlaybackOrderTest#playlistUiReorderPassesCorrectQueue`

**PlaylistReorderDatabaseTest** (`app/src/androidTest/java/com/example/musicflow/core/playlist/PlaylistReorderDatabaseTest.kt`; android)

- `PLAYLIST/PlaylistReorderDatabaseTest#reorderDoesNotAffectOtherPlaylistOrHistory`
- `PLAYLIST/PlaylistReorderDatabaseTest#reorderPersistsAfterDatabaseReopen`

**PlaylistRepositoryTest** (`app/src/androidTest/java/com/example/musicflow/core/playlist/PlaylistRepositoryTest.kt`; android)

- `PLAYLIST/PlaylistRepositoryTest#createRenameAddRemoveAndCascade`
- `PLAYLIST/PlaylistRepositoryTest#playlistChangesDoNotDeleteRecentHistory`

**MusicFlowPlaylistPolishV3Test** (`app/src/androidTest/java/com/example/musicflow/ui/MusicFlowPlaylistPolishV3Test.kt`; android)

- `PLAYLIST/MusicFlowPlaylistPolishV3Test#firstTrackDisablesMovingUpButAllowsMovingDown`
- `PLAYLIST/MusicFlowPlaylistPolishV3Test#trackMenuWorksAtLargeFontWithoutImmediateDeletion`

**PlaylistBackupRealPickerUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/PlaylistBackupRealPickerUiTest.kt`; android)

- `PLAYLIST/PlaylistBackupRealPickerUiTest#actualJsonDocumentPickerImportsOnlyAfterConfirmation`

**PlaylistQuickAddRealSafUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/PlaylistQuickAddRealSafUiTest.kt`; android)

- `PLAYLIST/PlaylistQuickAddRealSafUiTest#realOpenDocumentAddsPersistedReadableTrackToSelectedPlaylist`

**PlaylistQuickAddUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/PlaylistQuickAddUiTest.kt`; android)

- `PLAYLIST/PlaylistQuickAddUiTest#deletedPlaylistNeverShowsAddButton`
- `PLAYLIST/PlaylistQuickAddUiTest#firstUseDoesNotDemandScanPermissionOrOpenPickerAutomatically`
- `PLAYLIST/PlaylistQuickAddUiTest#noConcurrentAddWhenSavingEvenOnLargeText`
- `PLAYLIST/PlaylistQuickAddUiTest#oneTapFromUiPersistsExactlyOneTrackLocallyAndNoDuplicate`
- `PLAYLIST/PlaylistQuickAddUiTest#searchWithoutAccentsAddsInOneTapAndMarksAlreadyStoredTracks`

**PlaylistSearchUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/PlaylistSearchUiTest.kt`; android)

- `PLAYLIST/PlaylistSearchUiTest#accentInsensitiveSearchOpensCorrectPlaylistAndClearsEmptyState`

**PlaylistBackupRulesTest** (`app/src/test/java/com/example/musicflow/core/playlist/PlaylistBackupRulesTest.kt`; jvm)

- `PLAYLIST/PlaylistBackupRulesTest#acceptsLocalMetadataAndEmptyPlaylists`
- `PLAYLIST/PlaylistBackupRulesTest#refusesOversizedOrMalformedPlaylistMetadata`
- `PLAYLIST/PlaylistBackupRulesTest#refusesRemoteStreamingLinksAndDuplicateTracks`

**PlaylistQuickAddRulesTest** (`app/src/test/java/com/example/musicflow/core/playlist/PlaylistQuickAddRulesTest.kt`; jvm)

- `PLAYLIST/PlaylistQuickAddRulesTest#addingIsLockedWhileAnotherWriteIsPending`
- `PLAYLIST/PlaylistQuickAddRulesTest#safFileUsesItsPersistedContentUriToPreventDuplicates`
- `PLAYLIST/PlaylistQuickAddRulesTest#scannedMusicUsesSameKeyAsStoredPlaylistTrack`
- `PLAYLIST/PlaylistQuickAddRulesTest#searchIsAccentInsensitiveAndBlankShowsEverything`

**PlaylistReorderRulesTest** (`app/src/test/java/com/example/musicflow/core/playlist/PlaylistReorderRulesTest.kt`; jvm)

- `PLAYLIST/PlaylistReorderRulesTest#boundariesAreRejected`
- `PLAYLIST/PlaylistReorderRulesTest#invalidMovesAreRejected`
- `PLAYLIST/PlaylistReorderRulesTest#moveDown`
- `PLAYLIST/PlaylistReorderRulesTest#moveUp`
- `PLAYLIST/PlaylistReorderRulesTest#sourceListIsNotModified`

**PlaylistRulesTest** (`app/src/test/java/com/example/musicflow/core/playlist/PlaylistRulesTest.kt`; jvm)

- `PLAYLIST/PlaylistRulesTest#addingTracksPreservesOrder`
- `PLAYLIST/PlaylistRulesTest#duplicateTrackIsIgnored`
- `PLAYLIST/PlaylistRulesTest#emptyNameIsRejected`
- `PLAYLIST/PlaylistRulesTest#invalidRenameIsRejected`
- `PLAYLIST/PlaylistRulesTest#nameIsTrimmedAndNormalized`
- `PLAYLIST/PlaylistRulesTest#overlyLongNameIsRejected`
- `PLAYLIST/PlaylistRulesTest#removingTrackDoesNotChangeOtherTracks`
- `PLAYLIST/PlaylistRulesTest#renamePreservesIdentity`
- `PLAYLIST/PlaylistRulesTest#sameTitleDifferentIdsAreAllowed`
- `PLAYLIST/PlaylistRulesTest#trackWithoutUriIsIgnored`

**PlaylistNameFilterTest** (`app/src/test/java/com/example/musicflow/feature/playlist/PlaylistNameFilterTest.kt`; jvm)

- `PLAYLIST/PlaylistNameFilterTest#blankQueryKeepsOriginalOrderAndInputUntouched`
- `PLAYLIST/PlaylistNameFilterTest#ignoresCaseAndOuterWhitespace`
- `PLAYLIST/PlaylistNameFilterTest#matchesVietnameseNamesWithoutDiacritics`
- `PLAYLIST/PlaylistNameFilterTest#noMatchReturnsEmptyWithoutAlteringPlaylists`


### FAVORITES — Yêu thích bài hát và album

**Source paths:** `app/src/main/java/com/example/musicflow/core/favorites/**`

**Downstream contract/smoke:** `FAVORITES/SmartLibraryUiTest#changingFavoriteRecomputesSmartCollectionImmediately`.

**FavoritesBatchTest** (`app/src/androidTest/java/com/example/musicflow/FavoritesBatchTest.kt`; android)

- `FAVORITES/FavoritesBatchTest#favoriteLibraryAndAlbum`
- `FAVORITES/FavoritesBatchTest#favoritesPersistSongAndAlbum`
- `FAVORITES/FavoritesBatchTest#homeFavoritesShortcuts`
- `FAVORITES/FavoritesBatchTest#safFavoriteWithoutMediaStorePermission`

**SmartLibraryUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/SmartLibraryUiTest.kt`; android)

- `FAVORITES/SmartLibraryUiTest#changingFavoriteRecomputesSmartCollectionImmediately`
- `FAVORITES/SmartLibraryUiTest#changingSmartCollectionClearsOldSearch`
- `FAVORITES/SmartLibraryUiTest#emptySmartCollectionCanOpenOrdinaryLibrary`
- `FAVORITES/SmartLibraryUiTest#emptySmartListIsSafeAtLargeFontAndNoPlayAction`
- `FAVORITES/SmartLibraryUiTest#homeEntryForSmartLibraryIsClickableWithLargeFont`
- `FAVORITES/SmartLibraryUiTest#mixPlaysCorrectQueueAndIndexWithoutWritingPlaylist`
- `FAVORITES/SmartLibraryUiTest#pickedEmptyStateExplainsHowToAddFiles`
- `FAVORITES/SmartLibraryUiTest#pickedTabNeedsNoMediaStorePermissionAndOnlyPlaysReadableTracks`
- `FAVORITES/SmartLibraryUiTest#pickedTrackAndMediaStoreDuplicateAppearOnlyOnceInSmartMix`
- `FAVORITES/SmartLibraryUiTest#pickedTrackIsIncludedInSmartMixEvenWithEmptyMediaStore`
- `FAVORITES/SmartLibraryUiTest#smartLibraryRouteShowsSmartCollectionsWhenMediaStoreIsEmpty`
- `FAVORITES/SmartLibraryUiTest#smartSearchIgnoresVietnameseAccentsAndPlaysOnlyVisibleQueue`
- `FAVORITES/SmartLibraryUiTest#smartSearchNoResultsCanClearFilterWithoutLosingCollection`


### SMART — Smart Library và gợi ý thư viện

**Source paths:** `app/src/main/java/com/example/musicflow/core/smart/**`, `app/src/main/java/com/example/musicflow/feature/library/Smart*`

**Downstream contract/smoke:** `SMART/SmartLibraryUiTest#pickedTrackAndMediaStoreDuplicateAppearOnlyOnceInSmartMix`.

**SmartLibraryUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/SmartLibraryUiTest.kt`; android)

- `SMART/SmartLibraryUiTest#changingFavoriteRecomputesSmartCollectionImmediately`
- `SMART/SmartLibraryUiTest#changingSmartCollectionClearsOldSearch`
- `SMART/SmartLibraryUiTest#emptySmartCollectionCanOpenOrdinaryLibrary`
- `SMART/SmartLibraryUiTest#emptySmartListIsSafeAtLargeFontAndNoPlayAction`
- `SMART/SmartLibraryUiTest#homeEntryForSmartLibraryIsClickableWithLargeFont`
- `SMART/SmartLibraryUiTest#mixPlaysCorrectQueueAndIndexWithoutWritingPlaylist`
- `SMART/SmartLibraryUiTest#pickedEmptyStateExplainsHowToAddFiles`
- `SMART/SmartLibraryUiTest#pickedTabNeedsNoMediaStorePermissionAndOnlyPlaysReadableTracks`
- `SMART/SmartLibraryUiTest#pickedTrackAndMediaStoreDuplicateAppearOnlyOnceInSmartMix`
- `SMART/SmartLibraryUiTest#pickedTrackIsIncludedInSmartMixEvenWithEmptyMediaStore`
- `SMART/SmartLibraryUiTest#smartLibraryRouteShowsSmartCollectionsWhenMediaStoreIsEmpty`
- `SMART/SmartLibraryUiTest#smartSearchIgnoresVietnameseAccentsAndPlaysOnlyVisibleQueue`
- `SMART/SmartLibraryUiTest#smartSearchNoResultsCanClearFilterWithoutLosingCollection`

**SmartLibraryRulesTest** (`app/src/test/java/com/example/musicflow/core/smart/SmartLibraryRulesTest.kt`; jvm)

- `SMART/SmartLibraryRulesTest#choosesOnlyCurrentlyScannedLocalFiles`
- `SMART/SmartLibraryRulesTest#durationThresholdAndFavoritesAreComputedDynamically`
- `SMART/SmartLibraryRulesTest#eachListIsCappedAtThirtyTracks`
- `SMART/SmartLibraryRulesTest#emptyLibraryIsSafeAndCannotManufacturePlaybackTracks`
- `SMART/SmartLibraryRulesTest#newestIgnoresUnknownDateAndUsesDeterministicTieBreaking`
- `SMART/SmartLibraryRulesTest#recentOrderAndMixPriorityAreStableAndDeduplicated`


### HISTORY — Lịch sử nghe và Room recent

**Source paths:** `app/src/main/java/com/example/musicflow/core/history/**`

**Downstream contract/smoke:** `HISTORY/RecentHistoryDatabaseTest#historySurvivesDatabaseReopen`.

**RecentHistoryDatabaseTest** (`app/src/androidTest/java/com/example/musicflow/core/history/RecentHistoryDatabaseTest.kt`; android)

- `HISTORY/RecentHistoryDatabaseTest#historySurvivesDatabaseReopen`
- `HISTORY/RecentHistoryDatabaseTest#invalidSongIsNotStored`
- `HISTORY/RecentHistoryDatabaseTest#recordKeepsTwentyAndMovesReplayToFront`

**CloudHistoryRulesTest** (`app/src/test/java/com/example/musicflow/core/history/CloudHistoryRulesTest.kt`; jvm)

- `HISTORY/CloudHistoryRulesTest#newerRemoteEntryCanUpdateTimestamp`
- `HISTORY/CloudHistoryRulesTest#normalizationIsStable`
- `HISTORY/CloudHistoryRulesTest#olderRemoteEntryNeverReplacesNewerLocal`
- `HISTORY/CloudHistoryRulesTest#retainsOnlyTwentyMostRecent`

**RecentHistoryTest** (`app/src/test/java/com/example/musicflow/core/history/RecentHistoryTest.kt`; jvm)

- `HISTORY/RecentHistoryTest#historyKeepsAtMostTwentySongs`
- `HISTORY/RecentHistoryTest#invalidSongIsIgnored`
- `HISTORY/RecentHistoryTest#newestSongComesFirst`
- `HISTORY/RecentHistoryTest#replayMovesSongToFrontWithoutDuplicate`
- `HISTORY/RecentHistoryTest#sameTitleDifferentIdsAreKept`

**RecentPlaybackTrackerTest** (`app/src/test/java/com/example/musicflow/core/history/RecentPlaybackTrackerTest.kt`; jvm)

- `HISTORY/RecentPlaybackTrackerTest#artworkUpdateDoesNotRecordAgain`
- `HISTORY/RecentPlaybackTrackerTest#doesNotRecordBeforePlaybackStarts`
- `HISTORY/RecentPlaybackTrackerTest#missingIdentityIsIgnored`
- `HISTORY/RecentPlaybackTrackerTest#nextAndPreviousSongsAreRecorded`
- `HISTORY/RecentPlaybackTrackerTest#pauseAndResumeDoNotRecordAgain`


### LYRICS — Lời bài hát LRC, đồng bộ lời và SAF

**Source paths:** `app/src/main/java/com/example/musicflow/core/lyrics/**`

**Downstream contract/smoke:** `LYRICS/LyricsSafImportTest#realSafDocumentImportAndPersistentAudioAssociation`.

**LyricsImportUiE2eTest** (`app/src/androidTest/java/com/example/musicflow/core/lyrics/LyricsImportUiE2eTest.kt`; android)

- `LYRICS/LyricsImportUiE2eTest#playerButtonOpensRealLrcAndAutomaticallyHighlightsAfterSeek`

**LyricsSafImportTest** (`app/src/androidTest/java/com/example/musicflow/core/lyrics/LyricsSafImportTest.kt`; android)

- `LYRICS/LyricsSafImportTest#realSafDocumentImportAndPersistentAudioAssociation`

**LyricsScreenSyncTest** (`app/src/androidTest/java/com/example/musicflow/core/lyrics/LyricsScreenSyncTest.kt`; android)

- `LYRICS/LyricsScreenSyncTest#highlightsLrcLineAtRealMediaSessionSeekPositions`

**LrcParserTest** (`app/src/test/java/com/example/musicflow/core/lyrics/LrcParserTest.kt`; jvm)

- `LYRICS/LrcParserTest#bomMetadataAndMillisecondFractions`
- `LYRICS/LrcParserTest#invalidAndBlankTimestampsIgnored`
- `LYRICS/LrcParserTest#longTextTruncatedToFourHundredCharacters`
- `LYRICS/LrcParserTest#multipleTimestampsAndUnsortedLinesAreSorted`
- `LYRICS/LrcParserTest#offsetAndNegativeClamp`
- `LYRICS/LrcParserTest#timelineFollowsSeekAndBeforeFirstTimestamp`


### AUTH — Đăng nhập, khách, OAuth, hồ sơ

**Source paths:** `app/src/main/java/com/example/musicflow/core/account/AccountApi.kt`, `app/src/main/java/com/example/musicflow/core/account/AccountOAuth*`, `app/src/main/java/com/example/musicflow/core/account/GoogleOAuth*`, `app/src/main/java/com/example/musicflow/core/account/DemoAccountStore.kt`, `app/src/main/java/com/example/musicflow/core/account/DemoPasswordHash.kt`, `app/src/main/java/com/example/musicflow/core/account/AccountAuth*`, `app/src/main/java/com/example/musicflow/feature/account/AccountScreen.kt`, `app/src/main/java/com/example/musicflow/feature/account/AccountViewModel.kt`, `app/src/main/java/com/example/musicflow/feature/account/AccountFormValidation.kt`, `app/src/main/java/com/example/musicflow/feature/account/AccountFriendlyErrors.kt`

**Downstream contract/smoke:** `AUTH/AccountAuthUiTest#signupTabInlineErrorsGoogleDisabledAndGuestNavigation`, `AUTH/AccountAuthErrorLabelsTest#doesNotEchoUnknownServerErrorData`.

**AccountAuthUiTest** (`app/src/androidTest/java/com/example/musicflow/AccountAuthUiTest.kt`; android)

- `AUTH/AccountAuthUiTest#signupTabInlineErrorsGoogleDisabledAndGuestNavigation`

**AccountJourneysV4UiTest** (`app/src/androidTest/java/com/example/musicflow/ui/AccountJourneysV4UiTest.kt`; android)

- `AUTH/AccountJourneysV4UiTest#androidBackClosesOwnerOverviewInsteadOfLosingRolePage`
- `AUTH/AccountJourneysV4UiTest#catalogMutationNeedsConfirmationAndServerAcknowledgment`
- `AUTH/AccountJourneysV4UiTest#creatorProfileOpensWithSingleLoadAndClosesAfterSave`
- `AUTH/AccountJourneysV4UiTest#draftReviewLoadsLightweightSummaryThenRequiresOpeningAndAcknowledgment`
- `AUTH/AccountJourneysV4UiTest#oneReviewerAreaAtATimeAndNoUnrelatedPanels`
- `AUTH/AccountJourneysV4UiTest#ownerActionsAppearOnlyAfterSelectingAnAccountAndUseConfirmedSnapshot`
- `AUTH/AccountJourneysV4UiTest#studioOptionalAlbumIsHiddenAndUnsavedEditsOfferKeepOrDiscard`

**AccountAuthErrorLabelsTest** (`app/src/test/java/com/example/musicflow/core/account/AccountAuthErrorLabelsTest.kt`; jvm)

- `AUTH/AccountAuthErrorLabelsTest#describesUnapprovedRecipientForHostedEmail`
- `AUTH/AccountAuthErrorLabelsTest#distinguishesOtherCommonAuthProblems`
- `AUTH/AccountAuthErrorLabelsTest#doesNotEchoUnknownServerErrorData`
- `AUTH/AccountAuthErrorLabelsTest#handlesLegacyEmailQuotaCode`
- `AUTH/AccountAuthErrorLabelsTest#identifiesEmailSendQuotaInsteadOfGenericTooManyRequests`
- `AUTH/AccountAuthErrorLabelsTest#retainsGenericRateLimitFallbackWithoutLeakingOtherDetails`
- `AUTH/AccountAuthErrorLabelsTest#tellsUserAboutRejectedEmailAndConfirmation`

**DemoPasswordHashTest** (`app/src/test/java/com/example/musicflow/core/account/DemoPasswordHashTest.kt`; jvm)

- `AUTH/DemoPasswordHashTest#rejectsIncorrectPassword`
- `AUTH/DemoPasswordHashTest#rejectsInvalidHashLength`
- `AUTH/DemoPasswordHashTest#saltChangesStoredHash`
- `AUTH/DemoPasswordHashTest#verifiesCorrectPasswordWithoutSavingPlaintext`

**GoogleOAuthFlowTest** (`app/src/test/java/com/example/musicflow/core/account/GoogleOAuthFlowTest.kt`; jvm)

- `AUTH/GoogleOAuthFlowTest#acceptsValidOneTimeCallback`
- `AUTH/GoogleOAuthFlowTest#authorizeUsesSupabaseAndCodeChallenge`
- `AUTH/GoogleOAuthFlowTest#challengeMatchesPkceKnownVector`
- `AUTH/GoogleOAuthFlowTest#rejectsCallbackWithFragment`
- `AUTH/GoogleOAuthFlowTest#rejectsCallbackWithPort`
- `AUTH/GoogleOAuthFlowTest#rejectsDuplicateCode`
- `AUTH/GoogleOAuthFlowTest#rejectsDuplicateFlow`
- `AUTH/GoogleOAuthFlowTest#rejectsExpiredRequest`
- `AUTH/GoogleOAuthFlowTest#rejectsOAuthErrorEvenWithCode`
- `AUTH/GoogleOAuthFlowTest#rejectsOversizedCallbackQuery`
- `AUTH/GoogleOAuthFlowTest#rejectsUnexpectedOrigin`
- `AUTH/GoogleOAuthFlowTest#rejectsWrongNonce`

**AccountFormValidationTest** (`app/src/test/java/com/example/musicflow/feature/account/AccountFormValidationTest.kt`; jvm)

- `AUTH/AccountFormValidationTest#emptyFormExplainsMissingFields`
- `AUTH/AccountFormValidationTest#invalidEmailAndShortPasswordShowInlineErrors`
- `AUTH/AccountFormValidationTest#signinDoesNotRequireConfirmation`
- `AUTH/AccountFormValidationTest#signupAcceptsMatchingPasswordAndTrimmedEmail`
- `AUTH/AccountFormValidationTest#signupRequiresMatchingPassword`

**AccountFriendlyErrorsTest** (`app/src/test/java/com/example/musicflow/feature/account/AccountFriendlyErrorsTest.kt`; jvm)

- `AUTH/AccountFriendlyErrorsTest#emailConfirmationHasClearNextStep`
- `AUTH/AccountFriendlyErrorsTest#incorrectPasswordHasFriendlyNextStep`
- `AUTH/AccountFriendlyErrorsTest#rawBackendDetailsAreNeverExposedForUnexpectedErrors`
- `AUTH/AccountFriendlyErrorsTest#registeredEmailExplainsHowToProceed`


### ROLES_ADMIN — Owner, quản trị viên, Developer và RBAC

**Source paths:** `app/src/main/java/com/example/musicflow/core/account/MusicFlowAccess.kt`, `app/src/main/java/com/example/musicflow/core/account/OwnerDemoSnapshot.kt`, `app/src/main/java/com/example/musicflow/core/account/AccountHomePlan.kt`, `app/src/main/java/com/example/musicflow/core/account/AccountReviewPlan.kt`, `app/src/main/java/com/example/musicflow/core/account/AccountTaskGuide.kt`, `app/src/main/java/com/example/musicflow/core/account/AccountWorkspacePlan.kt`, `app/src/main/java/com/example/musicflow/feature/account/AccountRolePanel.kt`, `app/src/main/java/com/example/musicflow/feature/account/OwnerDemoDashboard.kt`, `admin-web/**`

**Downstream contract/smoke:** `ROLES_ADMIN/MusicFlowRolesUiTest#scopedPlatformAdminCannotAccessOwnerGrants`, `ROLES_ADMIN/MusicFlowRolesUiTest#failedAccessRefreshHidesPreviousAdminTools`.

**AccountHomeV2UiTest** (`app/src/androidTest/java/com/example/musicflow/ui/AccountHomeV2UiTest.kt`; android)

- `ROLES_ADMIN/AccountHomeV2UiTest#accountHidesProjectDemoUntilUserExplicitlyRequestsIt`
- `ROLES_ADMIN/AccountHomeV2UiTest#approvedArtistSeesCreatorShortcut`
- `ROLES_ADMIN/AccountHomeV2UiTest#demoSessionKeepsOfflineOnlyTools`
- `ROLES_ADMIN/AccountHomeV2UiTest#developerSeesDebugQaShortcutOnly`
- `ROLES_ADMIN/AccountHomeV2UiTest#freeListenerSeesCompactActionsAndOpensDetailsOnDemand`
- `ROLES_ADMIN/AccountHomeV2UiTest#guestCanChooseOfflineListeningBeforeSignIn`
- `ROLES_ADMIN/AccountHomeV2UiTest#moderatorSeesReviewShortcutWithoutAdminClaim`
- `ROLES_ADMIN/AccountHomeV2UiTest#ownerAndModeratorHaveDifferentRelevantActions`
- `ROLES_ADMIN/AccountHomeV2UiTest#premiumNeverShowsAdminToolsWithoutBackendRole`
- `ROLES_ADMIN/AccountHomeV2UiTest#signupShowsEmailConfirmationInstructionsWithoutAdminSettings`
- `ROLES_ADMIN/AccountHomeV2UiTest#unconfiguredOnlineAccountExplainsOfflineAlternativeAndOpensLocalDemoForm`

**AccountJourneysV4UiTest** (`app/src/androidTest/java/com/example/musicflow/ui/AccountJourneysV4UiTest.kt`; android)

- `ROLES_ADMIN/AccountJourneysV4UiTest#androidBackClosesOwnerOverviewInsteadOfLosingRolePage`
- `ROLES_ADMIN/AccountJourneysV4UiTest#catalogMutationNeedsConfirmationAndServerAcknowledgment`
- `ROLES_ADMIN/AccountJourneysV4UiTest#creatorProfileOpensWithSingleLoadAndClosesAfterSave`
- `ROLES_ADMIN/AccountJourneysV4UiTest#draftReviewLoadsLightweightSummaryThenRequiresOpeningAndAcknowledgment`
- `ROLES_ADMIN/AccountJourneysV4UiTest#oneReviewerAreaAtATimeAndNoUnrelatedPanels`
- `ROLES_ADMIN/AccountJourneysV4UiTest#ownerActionsAppearOnlyAfterSelectingAnAccountAndUseConfirmedSnapshot`
- `ROLES_ADMIN/AccountJourneysV4UiTest#studioOptionalAlbumIsHiddenAndUnsavedEditsOfferKeepOrDiscard`

**AccountTaskUxV3UiTest** (`app/src/androidTest/java/com/example/musicflow/ui/AccountTaskUxV3UiTest.kt`; android)

- `ROLES_ADMIN/AccountTaskUxV3UiTest#actorNavigationIsAvailableAtLargeFontScale`
- `ROLES_ADMIN/AccountTaskUxV3UiTest#creatorStudioIsOptInAndShowsUsefulEmptyState`
- `ROLES_ADMIN/AccountTaskUxV3UiTest#emptyModerationQueueOnlyAppearsAfterSuccessfulLoad`
- `ROLES_ADMIN/AccountTaskUxV3UiTest#failedReloadClearsCachedAdminSearchResult`
- `ROLES_ADMIN/AccountTaskUxV3UiTest#ownerStartsWithUserWorkAndOpensOverviewOnlyWhenAsked`

**AccountWorkspacesUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/AccountWorkspacesUiTest.kt`; android)

- `ROLES_ADMIN/AccountWorkspacesUiTest#ownerStartsInManagementAndCanSwitchToReviewOrCreator`
- `ROLES_ADMIN/AccountWorkspacesUiTest#reviewOnlyAdminNeverSeesUserManagement`
- `ROLES_ADMIN/AccountWorkspacesUiTest#revokedAdminRoleReturnsToNonPrivilegedWorkspace`

**MusicFlowRolesUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/MusicFlowRolesUiTest.kt`; android)

- `ROLES_ADMIN/MusicFlowRolesUiTest#creatorRequestUsesServerRpcAndNeverSelfAssignsRole`
- `ROLES_ADMIN/MusicFlowRolesUiTest#developerHasDebugShortcutWithoutAdminPowers`
- `ROLES_ADMIN/MusicFlowRolesUiTest#failedAccessRefreshHidesPreviousAdminTools`
- `ROLES_ADMIN/MusicFlowRolesUiTest#moderatorCanReviewDraftMetadataButNotRolesUsersOrCatalog`
- `ROLES_ADMIN/MusicFlowRolesUiTest#premiumDoesNotExposeAdministrationOrDeveloper`
- `ROLES_ADMIN/MusicFlowRolesUiTest#refreshingAccessRemovesRevokedDeveloperShortcut`
- `ROLES_ADMIN/MusicFlowRolesUiTest#scopedPlatformAdminCannotAccessOwnerGrants`

**OwnerDemoDashboardUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/OwnerDemoDashboardUiTest.kt`; android)

- `ROLES_ADMIN/OwnerDemoDashboardUiTest#incompleteOrInvalidV2CountersNeverShowOwnerCounts`
- `ROLES_ADMIN/OwnerDemoDashboardUiTest#ownerSeesCountsAndDemoNextSteps`
- `ROLES_ADMIN/OwnerDemoDashboardUiTest#serverDenialNeverShowsOwnerCounts`

**AccountHomePlanTest** (`app/src/test/java/com/example/musicflow/core/account/AccountHomePlanTest.kt`; jvm)

- `ROLES_ADMIN/AccountHomePlanTest#demoIsClearlyOfflineOnlyWithoutPrivilegedLinks`
- `ROLES_ADMIN/AccountHomePlanTest#freeListenerSeesSimpleWelcomeAndNoAdminClaims`
- `ROLES_ADMIN/AccountHomePlanTest#ownerGetsFriendlyLabelsAndManagementShortcut`
- `ROLES_ADMIN/AccountHomePlanTest#premiumDoesNotGrantAdministrativeOrCreatorPrivilege`
- `ROLES_ADMIN/AccountHomePlanTest#reviewerAndArtistSeeTheirActualTask`
- `ROLES_ADMIN/AccountHomePlanTest#unverifiedRoleDisplaysNoPreviousPrivilege`

**AccountReviewPlanTest** (`app/src/test/java/com/example/musicflow/core/account/AccountReviewPlanTest.kt`; jvm)

- `ROLES_ADMIN/AccountReviewPlanTest#guestPremiumAndArtistDoNotSeeReviewAreas`
- `ROLES_ADMIN/AccountReviewPlanTest#metadataOnlyModeratorCanReviewDraftsNotCreatorOrCatalog`
- `ROLES_ADMIN/AccountReviewPlanTest#ownerCanSwitchBetweenAllReviewTasks`
- `ROLES_ADMIN/AccountReviewPlanTest#permissionsShowReadableLabelsInsteadOfTechnicalCodes`
- `ROLES_ADMIN/AccountReviewPlanTest#revokedPermissionNeverKeepsPreviouslySelectedReviewTask`
- `ROLES_ADMIN/AccountReviewPlanTest#scopedAdminGetsCreatorAndMetadataReviewButNotCatalog`

**AccountTaskGuideTest** (`app/src/test/java/com/example/musicflow/core/account/AccountTaskGuideTest.kt`; jvm)

- `ROLES_ADMIN/AccountTaskGuideTest#approvedCreatorGetsWorkSteps`
- `ROLES_ADMIN/AccountTaskGuideTest#delegatedAdminOnlyGetsScopedWorkHint`
- `ROLES_ADMIN/AccountTaskGuideTest#developerDescriptionNeverClaimsOwnerAccess`
- `ROLES_ADMIN/AccountTaskGuideTest#listenerIsDirectedToApplicationNotStudio`
- `ROLES_ADMIN/AccountTaskGuideTest#moderatorDoesNotClaimPublishingRights`
- `ROLES_ADMIN/AccountTaskGuideTest#ownerGetsManagementAndOptionalOverview`

**AccountWorkspacePlanTest** (`app/src/test/java/com/example/musicflow/core/account/AccountWorkspacePlanTest.kt`; jvm)

- `ROLES_ADMIN/AccountWorkspacePlanTest#delegatedAdminOnlySeesPermittedArea`
- `ROLES_ADMIN/AccountWorkspacePlanTest#developerToolsOnlyAvailableInDebugBuild`
- `ROLES_ADMIN/AccountWorkspacePlanTest#guestLikeListenerAndPremiumOnlySeeCreatorArea`
- `ROLES_ADMIN/AccountWorkspacePlanTest#moderatorAndCreatorReceiveDifferentLandings`
- `ROLES_ADMIN/AccountWorkspacePlanTest#ownerStartsWithManagementAndHasModeration`
- `ROLES_ADMIN/AccountWorkspacePlanTest#revokedAccessDoesNotRetainOldWorkspace`
- `ROLES_ADMIN/AccountWorkspacePlanTest#roleLabelsAndPrimaryActionsAreAudienceFriendly`

**MusicFlowAccessTest** (`app/src/test/java/com/example/musicflow/core/account/MusicFlowAccessTest.kt`; jvm)

- `ROLES_ADMIN/MusicFlowAccessTest#artistOrAuthorIsNotAdmin`
- `ROLES_ADMIN/MusicFlowAccessTest#collaboratorRequiresExplicitPermission`
- `ROLES_ADMIN/MusicFlowAccessTest#developerDoesNotInheritAdminPower`
- `ROLES_ADMIN/MusicFlowAccessTest#guestAndFreeNeverHaveStaffRights`
- `ROLES_ADMIN/MusicFlowAccessTest#moderatorPermissionTextCannotCreateAdminRights`
- `ROLES_ADMIN/MusicFlowAccessTest#moderatorReviewsMetadataButCannotGrantRolesOrPublish`
- `ROLES_ADMIN/MusicFlowAccessTest#ownerHasBusinessAdministrationButNotDeveloperRole`
- `ROLES_ADMIN/MusicFlowAccessTest#permissionsWithoutPlatformAdminRoleHaveNoEffect`

**OwnerDemoSnapshotTest** (`app/src/test/java/com/example/musicflow/core/account/OwnerDemoSnapshotTest.kt`; jvm)

- `ROLES_ADMIN/OwnerDemoSnapshotTest#missingOrDuplicateOwnerIsNotHealthy`
- `ROLES_ADMIN/OwnerDemoSnapshotTest#missingTierAndPendingExamplesAreReportedIndependently`
- `ROLES_ADMIN/OwnerDemoSnapshotTest#oneVerifiedOwnerAndNoTestActorsMeansGuidanceNeeded`
- `ROLES_ADMIN/OwnerDemoSnapshotTest#platformAdminWithoutThreePermissionsIsNotReady`
- `ROLES_ADMIN/OwnerDemoSnapshotTest#rolesReadyForDemoDoNotImplyPublishingOrProduction`


### CREATOR — Creator, draft, xét duyệt nội dung

**Source paths:** `app/src/main/java/com/example/musicflow/core/account/Creator*`, `app/src/main/java/com/example/musicflow/feature/account/Creator*`

**Downstream contract/smoke:** `CREATOR/CreatorApplicationReviewUiTest#pendingArtistRequestIsShownAndCannotBeResubmitted`.

**AccountJourneysV4UiTest** (`app/src/androidTest/java/com/example/musicflow/ui/AccountJourneysV4UiTest.kt`; android)

- `CREATOR/AccountJourneysV4UiTest#androidBackClosesOwnerOverviewInsteadOfLosingRolePage`
- `CREATOR/AccountJourneysV4UiTest#catalogMutationNeedsConfirmationAndServerAcknowledgment`
- `CREATOR/AccountJourneysV4UiTest#creatorProfileOpensWithSingleLoadAndClosesAfterSave`
- `CREATOR/AccountJourneysV4UiTest#draftReviewLoadsLightweightSummaryThenRequiresOpeningAndAcknowledgment`
- `CREATOR/AccountJourneysV4UiTest#oneReviewerAreaAtATimeAndNoUnrelatedPanels`
- `CREATOR/AccountJourneysV4UiTest#ownerActionsAppearOnlyAfterSelectingAnAccountAndUseConfirmedSnapshot`
- `CREATOR/AccountJourneysV4UiTest#studioOptionalAlbumIsHiddenAndUnsavedEditsOfferKeepOrDiscard`

**CreatorApplicationReviewUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/CreatorApplicationReviewUiTest.kt`; android)

- `CREATOR/CreatorApplicationReviewUiTest#adminDecisionWaitsForExplicitConfirmationAndCanBeCancelled`
- `CREATOR/CreatorApplicationReviewUiTest#pendingArtistRequestIsShownAndCannotBeResubmitted`
- `CREATOR/CreatorApplicationReviewUiTest#serverRefusalPreservesPendingReview`

**CreatorDraftStudioUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/CreatorDraftStudioUiTest.kt`; android)

- `CREATOR/CreatorDraftStudioUiTest#creatorCanStartDraftAndSeeCopyrightFields`
- `CREATOR/CreatorDraftStudioUiTest#moderatorReviewScreenDoesNotPublishWithoutDecision`

**CreatorPrivateAttachmentUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/CreatorPrivateAttachmentUiTest.kt`; android)

- `CREATOR/CreatorPrivateAttachmentUiTest#approvedCreatorCanChooseTwoPrivateUploads`
- `CREATOR/CreatorPrivateAttachmentUiTest#reviewerCannotSeeUploadButtons`

**CreatorApplicationUiRulesTest** (`app/src/test/java/com/example/musicflow/core/account/CreatorApplicationUiRulesTest.kt`; jvm)

- `CREATOR/CreatorApplicationUiRulesTest#creatorSubmissionRequiresLoadedStateAndValidName`
- `CREATOR/CreatorApplicationUiRulesTest#pendingArtistDoesNotBlockAuthorApplication`
- `CREATOR/CreatorApplicationUiRulesTest#reviewRequiresAuditableReason`
- `CREATOR/CreatorApplicationUiRulesTest#statusesAndRolesHaveVietnameseLabels`

**CreatorDraftRulesTest** (`app/src/test/java/com/example/musicflow/core/account/CreatorDraftRulesTest.kt`; jvm)

- `CREATOR/CreatorDraftRulesTest#creditIdentityAndRoleAreValidated`
- `CREATOR/CreatorDraftRulesTest#draftCanBeSavedBeforeRightsReviewed`
- `CREATOR/CreatorDraftRulesTest#submissionRequiresCopyrightEvidenceAndStreamingDeclaration`

**CreatorPrivateFileRulesTest** (`app/src/test/java/com/example/musicflow/core/account/CreatorPrivateFileRulesTest.kt`; jvm)

- `CREATOR/CreatorPrivateFileRulesTest#onlyDemoSizedAudioOrRightsEvidence`
- `CREATOR/CreatorPrivateFileRulesTest#storagePathsAreServerIssuedAndNotTraversal`


### BILLING_DEMO — Premium và thanh toán giả lập

**Source paths:** `app/src/main/java/com/example/musicflow/core/account/DemoBilling*`, `app/src/main/java/com/example/musicflow/core/account/AccountTier.kt`, `app/src/main/java/com/example/musicflow/feature/account/DemoBillingScreen.kt`

**Downstream contract/smoke:** `BILLING_DEMO/DemoBillingUiTest#failedCheckoutNeverChangesPriorPaidDemoReceipt`, `BILLING_DEMO/AccountTierTest#expiredPremiumIsFree`.

**DemoBillingUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/DemoBillingUiTest.kt`; android)

- `BILLING_DEMO/DemoBillingUiTest#annualCardCheckoutNeedsConfirmationThenShowsFakeReceipt`
- `BILLING_DEMO/DemoBillingUiTest#cancellingCheckoutBeforeConfirmCreatesNoRecord`
- `BILLING_DEMO/DemoBillingUiTest#cancellingRequiresConfirmationAndResetClearsOnlyDemoRecord`
- `BILLING_DEMO/DemoBillingUiTest#failedCheckoutNeverChangesPriorPaidDemoReceipt`
- `BILLING_DEMO/DemoBillingUiTest#guestCanOpenCheckoutWithoutSigningIn`
- `BILLING_DEMO/DemoBillingUiTest#receiptsPersistOnDeviceButNeverCrossAccountScopesOrGrantAccess`

**AccountTierTest** (`app/src/test/java/com/example/musicflow/core/account/AccountTierTest.kt`; jvm)

- `BILLING_DEMO/AccountTierTest#activePremiumIsPremium`
- `BILLING_DEMO/AccountTierTest#expiredPremiumIsFree`
- `BILLING_DEMO/AccountTierTest#missingEntitlementIsFree`
- `BILLING_DEMO/AccountTierTest#premiumWithoutExpiryIsFree`
- `BILLING_DEMO/AccountTierTest#unknownOrMalformedEntitlementIsFree`

**DemoBillingRulesTest** (`app/src/test/java/com/example/musicflow/core/account/DemoBillingRulesTest.kt`; jvm)

- `BILLING_DEMO/DemoBillingRulesTest#activePlanRenewalExtendsExistingExpiry`
- `BILLING_DEMO/DemoBillingRulesTest#cancelledPlanIsNotActiveAndRenewalStartsFromNow`
- `BILLING_DEMO/DemoBillingRulesTest#expiredPlanBeginsAgainFromPresentTime`
- `BILLING_DEMO/DemoBillingRulesTest#monthlyPlanIs30DaysAndIllustrativePrice`
- `BILLING_DEMO/DemoBillingRulesTest#receiptIsNotBackendPremiumOrAdministrativeAccess`
- `BILLING_DEMO/DemoBillingRulesTest#yearPlanUses365Days`


### CLOUD_SYNC — Cloud library/history và đồng bộ Supabase demo

**Source paths:** `app/src/main/java/com/example/musicflow/core/account/Cloud*`, `app/src/main/java/com/example/musicflow/core/history/Cloud*`

**Downstream contract/smoke:** `CLOUD_SYNC/CloudSyncRulesTest#cloudMergeKeepsBothAndNeverDuplicates`.

**CloudSyncRulesTest** (`app/src/test/java/com/example/musicflow/core/account/CloudSyncRulesTest.kt`; jvm)

- `CLOUD_SYNC/CloudSyncRulesTest#cloudMergeKeepsBothAndNeverDuplicates`
- `CLOUD_SYNC/CloudSyncRulesTest#cloudMergeSkipsBlankMetadata`
- `CLOUD_SYNC/CloudSyncRulesTest#matchesOnlyOneLocalSong`
- `CLOUD_SYNC/CloudSyncRulesTest#rejectsAmbiguousOrMissingSongs`

**CloudHistoryRulesTest** (`app/src/test/java/com/example/musicflow/core/history/CloudHistoryRulesTest.kt`; jvm)

- `CLOUD_SYNC/CloudHistoryRulesTest#newerRemoteEntryCanUpdateTimestamp`
- `CLOUD_SYNC/CloudHistoryRulesTest#normalizationIsStable`
- `CLOUD_SYNC/CloudHistoryRulesTest#olderRemoteEntryNeverReplacesNewerLocal`
- `CLOUD_SYNC/CloudHistoryRulesTest#retainsOnlyTwentyMostRecent`


### ONLINE_RIGHTS — Online catalog và quyền phát có giấy phép

**Source paths:** `app/src/main/java/com/example/musicflow/core/online/**`, `app/src/main/java/com/example/musicflow/feature/online/**`, `scripts/musicflow_licensed_upload.py`

**Downstream contract/smoke:** `ONLINE_RIGHTS/OnlineRightsPolicyTest#missingRightsDefaultToDenied`, `ONLINE_RIGHTS/OnlineRightsPolicyTest#missingAttributionOrSourceBlocksPlayback`.

**OnlineRightsJsonTest** (`app/src/androidTest/java/com/example/musicflow/core/online/OnlineRightsJsonTest.kt`; android)

- `ONLINE_RIGHTS/OnlineRightsJsonTest#absentPublicFieldsNeverGrantStreaming`
- `ONLINE_RIGHTS/OnlineRightsJsonTest#externalCatalogRequiresNestedRightsObject`
- `ONLINE_RIGHTS/OnlineRightsJsonTest#fakeBooleanAndBrokenSourceFailClosed`
- `ONLINE_RIGHTS/OnlineRightsJsonTest#supabaseRightsDecodeAllFields`

**OnlineRightsScreenTest** (`app/src/androidTest/java/com/example/musicflow/core/online/OnlineRightsScreenTest.kt`; android)

- `ONLINE_RIGHTS/OnlineRightsScreenTest#unverifiedTrackShowsDetailsButIsNotPlayable`
- `ONLINE_RIGHTS/OnlineRightsScreenTest#verifiedTrackCanPlayAndShowCredits`

**OnlineRightsPolicyTest** (`app/src/test/java/com/example/musicflow/core/online/OnlineRightsPolicyTest.kt`; jvm)

- `ONLINE_RIGHTS/OnlineRightsPolicyTest#booleanClaimsWithoutReviewDateDoNotGrantRights`
- `ONLINE_RIGHTS/OnlineRightsPolicyTest#cc0StillRequiresCatalogSourceAndAttributionAsAppPolicy`
- `ONLINE_RIGHTS/OnlineRightsPolicyTest#completeRightsPermitStreaming`
- `ONLINE_RIGHTS/OnlineRightsPolicyTest#directPermissionAllowsPrivateEvidenceReference`
- `ONLINE_RIGHTS/OnlineRightsPolicyTest#missingAttributionOrSourceBlocksPlayback`
- `ONLINE_RIGHTS/OnlineRightsPolicyTest#missingRightsDefaultToDenied`
- `ONLINE_RIGHTS/OnlineRightsPolicyTest#requiredCcLicenseLinkMustBePresent`
- `ONLINE_RIGHTS/OnlineRightsPolicyTest#safeLinksMustBeHttpsWithoutCredentialsOrFragments`
- `ONLINE_RIGHTS/OnlineRightsPolicyTest#streamingIsSeparateFromDownloadAndCommercialPermission`
- `ONLINE_RIGHTS/OnlineRightsPolicyTest#unsupportedAndNcLicensesBlocked`


### HOME_NAV — Trang chủ, mini-player, điều hướng

**Source paths:** `app/src/main/java/com/example/musicflow/feature/home/**`, `app/src/main/java/com/example/musicflow/core/ux/**`

**Downstream contract/smoke:** `HOME_NAV/ListeningJourneysV1UiTest#homeShowsShortHistoryThenAllSongsWithOriginalPlaybackIndex`.

**ListeningJourneysV1UiTest** (`app/src/androidTest/java/com/example/musicflow/ui/ListeningJourneysV1UiTest.kt`; android)

- `HOME_NAV/ListeningJourneysV1UiTest#emptyPlayerHasImmediateOfflineActions`
- `HOME_NAV/ListeningJourneysV1UiTest#emptyPlaylistGuidesDirectlyToLocalLibrary`
- `HOME_NAV/ListeningJourneysV1UiTest#homeShowsShortHistoryThenAllSongsWithOriginalPlaybackIndex`
- `HOME_NAV/ListeningJourneysV1UiTest#playerShowsFirstQueuePositionAndAccessibleRepeatAtLargeText`
- `HOME_NAV/ListeningJourneysV1UiTest#searchOffersDirectJumpToMatchingAlbumWithoutRescanning`

**MusicFlowUxFoundationV3Test** (`app/src/androidTest/java/com/example/musicflow/ui/MusicFlowUxFoundationV3Test.kt`; android)

- `HOME_NAV/MusicFlowUxFoundationV3Test#firstUseHomeKeepsListeningAndSearchVisibleWhileAdvancedActionsAreOptional`
- `HOME_NAV/MusicFlowUxFoundationV3Test#libraryPickerUsesGenericAudioLabelAndKeepsAction`
- `HOME_NAV/MusicFlowUxFoundationV3Test#primaryHomeActionsAndOneTimeGuideCanBeDismissed`

**OceanFlowVisualUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/OceanFlowVisualUiTest.kt`; android)

- `HOME_NAV/OceanFlowVisualUiTest#brandHeaderRendersInDarkAndLightWithoutChangingText`
- `HOME_NAV/OceanFlowVisualUiTest#floatingMiniPlayerKeepsPlayAndOpenActions`
- `HOME_NAV/OceanFlowVisualUiTest#homeHeroPreservesLibrarySearchPlaylistActionsAtLargeFont`
- `HOME_NAV/OceanFlowVisualUiTest#playerArtworkStageShowsSafeOfflinePlaceholder`
- `HOME_NAV/OceanFlowVisualUiTest#searchStillOffersItsInputWithNewOceanHeader`
- `HOME_NAV/OceanFlowVisualUiTest#themedLibrarySongRowStillKeepsActionsSeparate`

**ListeningJourneyRulesTest** (`app/src/test/java/com/example/musicflow/core/ux/ListeningJourneyRulesTest.kt`; jvm)

- `HOME_NAV/ListeningJourneyRulesTest#queuePositionIsHumanFriendlyAndHandlesEmptyPlayer`
- `HOME_NAV/ListeningJourneyRulesTest#recentPreviewCanExpandWithoutChangingHistory`
- `HOME_NAV/ListeningJourneyRulesTest#resultGroupNamesAreReadable`
- `HOME_NAV/ListeningJourneyRulesTest#searchSuggestionFindsAnotherCategoryWithoutNewScan`


### UI_ACCESS — Thiết kế Ocean Flow, focus, TalkBack semantics

**Source paths:** `app/src/main/java/com/example/musicflow/ui/**`

**Downstream contract/smoke:** `UI_ACCESS/LibraryKeyboardAccessibilityV8Test#keyboardEnterCanExpandAndCollapseArtistWithoutTouch`, `UI_ACCESS/AccessibilityPolishV6UiTest#headingsStayNavigableAndFullTextAt200PercentInLightAndDark`.

**ExampleInstrumentedTest** (`app/src/androidTest/java/com/example/musicflow/ExampleInstrumentedTest.kt`; android)

- `UI_ACCESS/ExampleInstrumentedTest#mvpLibrarySearchToRealPlayer`
- `UI_ACCESS/ExampleInstrumentedTest#mvpRealMediaStoreToPlayer`
- `UI_ACCESS/ExampleInstrumentedTest#useAppContext`

**ThemeBatchTest** (`app/src/androidTest/java/com/example/musicflow/ThemeBatchTest.kt`; android)

- `UI_ACCESS/ThemeBatchTest#themePersistsAfterRestart`

**LibraryAccessibilityV7Test** (`app/src/androidTest/java/com/example/musicflow/feature/library/LibraryAccessibilityV7Test.kt`; android)

- `UI_ACCESS/LibraryAccessibilityV7Test#albumRowsPreservePlaybackIndicesAndDoNotCreateDuplicateKeys`
- `UI_ACCESS/LibraryAccessibilityV7Test#albumTracksStayVirtualizedInRealLibraryAt200Percent`
- `UI_ACCESS/LibraryAccessibilityV7Test#artistTracksStayVirtualizedAndKeepCorrectPlaybackQueueAt200Percent`
- `UI_ACCESS/LibraryAccessibilityV7Test#collapsedCollectionsDoNotCreateHundredsOfSongRows`
- `UI_ACCESS/LibraryAccessibilityV7Test#darkThemePrimaryTextTokensMeetWcagAa`
- `UI_ACCESS/LibraryAccessibilityV7Test#lightThemePrimaryTextTokensMeetWcagAa`

**LibraryKeyboardAccessibilityV8Test** (`app/src/androidTest/java/com/example/musicflow/feature/library/LibraryKeyboardAccessibilityV8Test.kt`; android)

- `UI_ACCESS/LibraryKeyboardAccessibilityV8Test#albumHeaderReceivesKeyboardEnterWithNoUnexpectedPlayback`
- `UI_ACCESS/LibraryKeyboardAccessibilityV8Test#dpadMovesFocusBetweenCollectionHeaders`
- `UI_ACCESS/LibraryKeyboardAccessibilityV8Test#dpadReachesOffscreenCollectionHeadersAtLargeFont`
- `UI_ACCESS/LibraryKeyboardAccessibilityV8Test#keyboardEnterCanExpandAndCollapseArtistWithoutTouch`

**AccessibilityPolishV6UiTest** (`app/src/androidTest/java/com/example/musicflow/ui/AccessibilityPolishV6UiTest.kt`; android)

- `UI_ACCESS/AccessibilityPolishV6UiTest#accountModesRemainAccessibleAsSeparateRowsAt200Percent`
- `UI_ACCESS/AccessibilityPolishV6UiTest#headingsStayNavigableAndFullTextAt200PercentInLightAndDark`
- `UI_ACCESS/AccessibilityPolishV6UiTest#homeActionsAreAvailableAt200PercentWithoutAuth`
- `UI_ACCESS/AccessibilityPolishV6UiTest#playerTransportAndSeekSemanticsRemainReachableAt200Percent`
- `UI_ACCESS/AccessibilityPolishV6UiTest#songRowPlaybackIsNamedAndFavoriteStateExposedAt200Percent`
- `UI_ACCESS/AccessibilityPolishV6UiTest#statusPanelAnnouncesHeadingWithNoPermissionOrLoginRequired`

**MusicFlowUiFoundationTest** (`app/src/androidTest/java/com/example/musicflow/ui/MusicFlowUiFoundationTest.kt`; android)

- `UI_ACCESS/MusicFlowUiFoundationTest#brandPrimaryColorsHaveReadableTextContrast`
- `UI_ACCESS/MusicFlowUiFoundationTest#errorPanelHasAccessibleRetryAction`
- `UI_ACCESS/MusicFlowUiFoundationTest#libraryEmptyHasGuidanceAndPickerRemainsAvailable`
- `UI_ACCESS/MusicFlowUiFoundationTest#libraryPermissionStateExplainsManualPicker`
- `UI_ACCESS/MusicFlowUiFoundationTest#miniPlayerAnnouncesTrackAndPlayState`
- `UI_ACCESS/MusicFlowUiFoundationTest#onlineCatalogEmptyCanRefreshWithoutAccount`
- `UI_ACCESS/MusicFlowUiFoundationTest#onlineCatalogErrorHasRetryAction`
- `UI_ACCESS/MusicFlowUiFoundationTest#queueEmptyStateIsClearAndDoesNotStopPlayback`
- `UI_ACCESS/MusicFlowUiFoundationTest#searchClearButtonHasTalkBackLabel`
- `UI_ACCESS/MusicFlowUiFoundationTest#searchErrorRetainsRetryAction`

**MusicFlowUiPolishTest** (`app/src/androidTest/java/com/example/musicflow/ui/MusicFlowUiPolishTest.kt`; android)

- `UI_ACCESS/MusicFlowUiPolishTest#authButtonsStayReachableAtLargeFontWithoutCallingAuth`
- `UI_ACCESS/MusicFlowUiPolishTest#guestThemeSelectorRemainsReachableAtLargeFont`
- `UI_ACCESS/MusicFlowUiPolishTest#libraryExplainsHorizontallyScrollableCategoriesAtLargeFont`
- `UI_ACCESS/MusicFlowUiPolishTest#playerTransportRemainsClickableAtLargeFontOnNarrowScreen`

**MusicFlowUxFoundationV3Test** (`app/src/androidTest/java/com/example/musicflow/ui/MusicFlowUxFoundationV3Test.kt`; android)

- `UI_ACCESS/MusicFlowUxFoundationV3Test#firstUseHomeKeepsListeningAndSearchVisibleWhileAdvancedActionsAreOptional`
- `UI_ACCESS/MusicFlowUxFoundationV3Test#libraryPickerUsesGenericAudioLabelAndKeepsAction`
- `UI_ACCESS/MusicFlowUxFoundationV3Test#primaryHomeActionsAndOneTimeGuideCanBeDismissed`

**OceanFlowVisualUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/OceanFlowVisualUiTest.kt`; android)

- `UI_ACCESS/OceanFlowVisualUiTest#brandHeaderRendersInDarkAndLightWithoutChangingText`
- `UI_ACCESS/OceanFlowVisualUiTest#floatingMiniPlayerKeepsPlayAndOpenActions`
- `UI_ACCESS/OceanFlowVisualUiTest#homeHeroPreservesLibrarySearchPlaylistActionsAtLargeFont`
- `UI_ACCESS/OceanFlowVisualUiTest#playerArtworkStageShowsSafeOfflinePlaceholder`
- `UI_ACCESS/OceanFlowVisualUiTest#searchStillOffersItsInputWithNewOceanHeader`
- `UI_ACCESS/OceanFlowVisualUiTest#themedLibrarySongRowStillKeepsActionsSeparate`

**ExampleUnitTest** (`app/src/test/java/com/example/musicflow/ExampleUnitTest.kt`; jvm)

- `UI_ACCESS/ExampleUnitTest#addition_isCorrect`


### DEMO_CENTER — Trung tâm demo và điều hướng theo tài khoản

**Source paths:** `app/src/main/java/com/example/musicflow/core/account/DemoCenter*`, `app/src/main/java/com/example/musicflow/feature/account/DemoCenter*`

**Downstream contract/smoke:** `DEMO_CENTER/DemoCenterPlanTest#ownerCanIllustrateDelegationButPremiumCannot`, `DEMO_CENTER/DemoCenterV1UiTest#demoProgressAndActorSelectionPersistButCannotGrantAnyRealAuthority`.

**DemoCenterV1UiTest** (`app/src/androidTest/java/com/example/musicflow/ui/DemoCenterV1UiTest.kt`; android)

- `DEMO_CENTER/DemoCenterV1UiTest#demoCenterHasAccessibleControlsAtLargeFontOnNarrowScreen`
- `DEMO_CENTER/DemoCenterV1UiTest#demoProgressAndActorSelectionPersistButCannotGrantAnyRealAuthority`
- `DEMO_CENTER/DemoCenterV1UiTest#guestCanChooseLocalDemoAuthWhenSupabaseIsNotConfigured`
- `DEMO_CENTER/DemoCenterV1UiTest#guestCanOpenDemoCenterWithoutLoggingIn`
- `DEMO_CENTER/DemoCenterV1UiTest#localAccountCanRegisterRestoreSignOutAndSignInWithNoPrivileges`
- `DEMO_CENTER/DemoCenterV1UiTest#ownerStoryShowsMockDelegationAndAuditWithoutRealRoleGrants`
- `DEMO_CENTER/DemoCenterV1UiTest#switchingPersonaKeepsSeparateLocalProgressAndPremiumLinksToMockBilling`

**DemoCenterPlanTest** (`app/src/test/java/com/example/musicflow/core/account/DemoCenterPlanTest.kt`; jvm)

- `DEMO_CENTER/DemoCenterPlanTest#everyPersonaHasACompleteButClearlyMockedJourney`
- `DEMO_CENTER/DemoCenterPlanTest#ownerCanIllustrateDelegationButPremiumCannot`
- `DEMO_CENTER/DemoCenterPlanTest#progressCannotExceedScenarioStepsOrBecomeNegative`
- `DEMO_CENTER/DemoCenterPlanTest#storyActionsAreDescriptiveAndDoNotExposeRpcNames`


### RELEASE_BACKUP — Sao lưu, versioning, APK ký QA và nâng cấp

**Source paths:** `scripts/build-musicflow-demo-apk-v1.ps1`, `scripts/run-signed-upgrade-*.ps1`, `app/src/main/java/com/example/musicflow/core/playlist/PlaylistBackup*`

**Downstream contract/smoke:** `RELEASE_BACKUP/PlaylistLocalBackupIntegrationTest#invalidDocumentCannotPartiallyModifyRoom`, `RELEASE_BACKUP/SignedApkUpgradeV6Test#verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly`, `RELEASE_BACKUP/SignedCommunityNavigationUiTest#guestReachesCommunityDemoFromRealHomeNavigation`.

**PlaylistLocalBackupIntegrationTest** (`app/src/androidTest/java/com/example/musicflow/core/playlist/PlaylistLocalBackupIntegrationTest.kt`; android)

- `RELEASE_BACKUP/PlaylistLocalBackupIntegrationTest#atomicImportCreatesCopiesWithSameOrderAndLeavesOriginalsUntouched`
- `RELEASE_BACKUP/PlaylistLocalBackupIntegrationTest#invalidDocumentCannotPartiallyModifyRoom`
- `RELEASE_BACKUP/PlaylistLocalBackupIntegrationTest#jsonRoundTripPreservesVietnameseAndOrderedTrackMetadata`
- `RELEASE_BACKUP/PlaylistLocalBackupIntegrationTest#onDemandFileCheckDistinguishesReadableLocalFileAndMissingFile`
- `RELEASE_BACKUP/PlaylistLocalBackupIntegrationTest#rejectsCorruptionWrongVersionAndOversizedInput`
- `RELEASE_BACKUP/PlaylistLocalBackupIntegrationTest#relinkingPreservesTrackPositionAndRejectsDuplicateOrUnknown`

**OfflineReliabilityUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/OfflineReliabilityUiTest.kt`; android)

- `RELEASE_BACKUP/OfflineReliabilityUiTest#backupToolsCollapseByDefaultAndExpandOnDemand`
- `RELEASE_BACKUP/OfflineReliabilityUiTest#relinkActionIsVisibleAndDoesNotTriggerDeleteAtLargeFonts`

**PlaylistBackupRealPickerUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/PlaylistBackupRealPickerUiTest.kt`; android)

- `RELEASE_BACKUP/PlaylistBackupRealPickerUiTest#actualJsonDocumentPickerImportsOnlyAfterConfirmation`

**SignedApkUpgradeV6Test** (`app/src/androidTest/java/com/example/musicflow/ui/SignedApkUpgradeV6Test.kt`; android)

- `RELEASE_BACKUP/SignedApkUpgradeV6Test#seedSignedV5PlaylistFavoriteAndRealSafGrant`
- `RELEASE_BACKUP/SignedApkUpgradeV6Test#verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly`

**SignedCommunityNavigationUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/SignedCommunityNavigationUiTest.kt`; android)

- `RELEASE_BACKUP/SignedCommunityNavigationUiTest#guestReachesCommunityDemoFromRealHomeNavigation`

**PlaylistBackupRulesTest** (`app/src/test/java/com/example/musicflow/core/playlist/PlaylistBackupRulesTest.kt`; jvm)

- `RELEASE_BACKUP/PlaylistBackupRulesTest#acceptsLocalMetadataAndEmptyPlaylists`
- `RELEASE_BACKUP/PlaylistBackupRulesTest#refusesOversizedOrMalformedPlaylistMetadata`
- `RELEASE_BACKUP/PlaylistBackupRulesTest#refusesRemoteStreamingLinksAndDuplicateTracks`


### COMMUNITY_RATING — Chấm sao 1–5 và dữ liệu Demo cộng đồng

**Source paths:** `app/src/main/java/com/example/musicflow/core/community/CommunityModels.kt`, `app/src/main/java/com/example/musicflow/core/community/DemoCommunityStore.kt`

**Downstream contract/smoke:** `COMMUNITY_RATING/CommunityRulesTest#starsOnlyAcceptOneThroughFive`, `COMMUNITY_RATING/DemoCommunityStoreTest#changingRatingUpdatesOnlyOneDemoRating`.

**CommunityRulesTest** (`app/src/test/java/com/example/musicflow/core/community/CommunityRulesTest.kt`; jvm)

- `COMMUNITY_RATING/CommunityRulesTest#commentRejectsMoreThanFiveHundredCharacters`
- `COMMUNITY_RATING/CommunityRulesTest#commentTrimsWhitespaceAndRejectsEmpty`
- `COMMUNITY_RATING/CommunityRulesTest#onlineIdsMustBeCanonicalUuidNeverLocalSafUri`
- `COMMUNITY_RATING/CommunityRulesTest#reportRequiresAtLeastFiveAndAtMostThreeHundredCharacters`
- `COMMUNITY_RATING/CommunityRulesTest#starsOnlyAcceptOneThroughFive`

**DemoCommunityStoreTest** (`app/src/test/java/com/example/musicflow/core/community/DemoCommunityStoreTest.kt`; jvm)

- `COMMUNITY_RATING/DemoCommunityStoreTest#cannotEditOrDeleteSomeoneElsesComment`
- `COMMUNITY_RATING/DemoCommunityStoreTest#cannotReportSelfOrUseInvalidReason`
- `COMMUNITY_RATING/DemoCommunityStoreTest#changingRatingUpdatesOnlyOneDemoRating`
- `COMMUNITY_RATING/DemoCommunityStoreTest#demoSeedIsClearlyFlaggedAndDoesNotUseCloudId`
- `COMMUNITY_RATING/DemoCommunityStoreTest#demoUsesPageSizeAndFreshInstanceResets`
- `COMMUNITY_RATING/DemoCommunityStoreTest#myCommentCanBeAddedEditedAndDeleted`


### COMMUNITY_COMMENTS — Đọc, đăng, sửa, xóa bình luận và báo cáo

**Source paths:** `app/src/main/java/com/example/musicflow/core/community/SupabaseCommunityRepository.kt`, `app/src/main/java/com/example/musicflow/feature/community/CommunityRoute.kt`, `app/src/main/java/com/example/musicflow/feature/community/CommunityUiPresentation.kt`, `docs/sql/musicflow_community_delete_report_cleanup_v14_2.sql`

**Downstream contract/smoke:** `COMMUNITY_COMMENTS/CommunityDemoUiTest#demoRatingAndCommentActionsStayLocal`, `COMMUNITY_COMMENTS/SignedCommunityNavigationUiTest#guestReachesCommunityDemoFromRealHomeNavigation`, `COMMUNITY_COMMENTS/CommunityRulesTest#onlineIdsMustBeCanonicalUuidNeverLocalSafUri`.

**CommunityDemoUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/CommunityDemoUiTest.kt`; android)

- `COMMUNITY_COMMENTS/CommunityDemoUiTest#demoDoesNotPretendToBeOnlineUser`
- `COMMUNITY_COMMENTS/CommunityDemoUiTest#demoRatingAndCommentActionsStayLocal`
- `COMMUNITY_COMMENTS/CommunityDemoUiTest#emptyOnlineCatalogOffersDemoWithoutPublishedTracks`
- `COMMUNITY_COMMENTS/CommunityDemoUiTest#largeTextCanReachDemoFromEmptyManagedCatalog`
- `COMMUNITY_COMMENTS/CommunityDemoUiTest#largeTextKeepsAllRatingsAndCommentComposerReachable`
- `COMMUNITY_COMMENTS/CommunityDemoUiTest#managedOnlineSourceDoesNotShowTechnicalServerField`
- `COMMUNITY_COMMENTS/CommunityDemoUiTest#manuallyConfiguredSourceStillOffersExistingEditor`

**SignedCommunityNavigationUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/SignedCommunityNavigationUiTest.kt`; android)

- `COMMUNITY_COMMENTS/SignedCommunityNavigationUiTest#guestReachesCommunityDemoFromRealHomeNavigation`

**CommunityRulesTest** (`app/src/test/java/com/example/musicflow/core/community/CommunityRulesTest.kt`; jvm)

- `COMMUNITY_COMMENTS/CommunityRulesTest#commentRejectsMoreThanFiveHundredCharacters`
- `COMMUNITY_COMMENTS/CommunityRulesTest#commentTrimsWhitespaceAndRejectsEmpty`
- `COMMUNITY_COMMENTS/CommunityRulesTest#onlineIdsMustBeCanonicalUuidNeverLocalSafUri`
- `COMMUNITY_COMMENTS/CommunityRulesTest#reportRequiresAtLeastFiveAndAtMostThreeHundredCharacters`
- `COMMUNITY_COMMENTS/CommunityRulesTest#starsOnlyAcceptOneThroughFive`

**DemoCommunityStoreTest** (`app/src/test/java/com/example/musicflow/core/community/DemoCommunityStoreTest.kt`; jvm)

- `COMMUNITY_COMMENTS/DemoCommunityStoreTest#cannotEditOrDeleteSomeoneElsesComment`
- `COMMUNITY_COMMENTS/DemoCommunityStoreTest#cannotReportSelfOrUseInvalidReason`
- `COMMUNITY_COMMENTS/DemoCommunityStoreTest#changingRatingUpdatesOnlyOneDemoRating`
- `COMMUNITY_COMMENTS/DemoCommunityStoreTest#demoSeedIsClearlyFlaggedAndDoesNotUseCloudId`
- `COMMUNITY_COMMENTS/DemoCommunityStoreTest#demoUsesPageSizeAndFreshInstanceResets`
- `COMMUNITY_COMMENTS/DemoCommunityStoreTest#myCommentCanBeAddedEditedAndDeleted`

**CommunityUiPresentationTest** (`app/src/test/java/com/example/musicflow/feature/community/CommunityUiPresentationTest.kt`; jvm)

- `COMMUNITY_COMMENTS/CommunityUiPresentationTest#demoTimeAndMalformedInputStayReadable`
- `COMMUNITY_COMMENTS/CommunityUiPresentationTest#errorsNeverExposeRawBackendDetails`
- `COMMUNITY_COMMENTS/CommunityUiPresentationTest#loginAndConnectionErrorsHaveActions`
- `COMMUNITY_COMMENTS/CommunityUiPresentationTest#serverDatesAreShownInVietnameseOrderAndLocalZone`


### COMMUNITY_MODERATION — Kiểm duyệt bình luận, phân quyền và nhật ký

**Source paths:** `app/src/main/java/com/example/musicflow/feature/community/CommunityModerationPanel.kt`, `docs/sql/musicflow_community_moderation_guard_v14_1.sql`

**Downstream contract/smoke:** `COMMUNITY_MODERATION/CommunityModerationUiTest#moderationRequiresReasonAndConfirm`, `COMMUNITY_MODERATION/MusicFlowAccessTest#guestAndFreeNeverHaveStaffRights`.

**CommunityModerationUiTest** (`app/src/androidTest/java/com/example/musicflow/ui/CommunityModerationUiTest.kt`; android)

- `COMMUNITY_MODERATION/CommunityModerationUiTest#dismissReportOnlySendsHideFalseWithReason`
- `COMMUNITY_MODERATION/CommunityModerationUiTest#failedPermissionDoesNotShowReports`
- `COMMUNITY_MODERATION/CommunityModerationUiTest#moderationRequiresReasonAndConfirm`

**MusicFlowAccessTest** (`app/src/test/java/com/example/musicflow/core/account/MusicFlowAccessTest.kt`; jvm)

- `COMMUNITY_MODERATION/MusicFlowAccessTest#artistOrAuthorIsNotAdmin`
- `COMMUNITY_MODERATION/MusicFlowAccessTest#collaboratorRequiresExplicitPermission`
- `COMMUNITY_MODERATION/MusicFlowAccessTest#developerDoesNotInheritAdminPower`
- `COMMUNITY_MODERATION/MusicFlowAccessTest#guestAndFreeNeverHaveStaffRights`
- `COMMUNITY_MODERATION/MusicFlowAccessTest#moderatorPermissionTextCannotCreateAdminRights`
- `COMMUNITY_MODERATION/MusicFlowAccessTest#moderatorReviewsMetadataButCannotGrantRolesOrPublish`
- `COMMUNITY_MODERATION/MusicFlowAccessTest#ownerHasBusinessAdministrationButNotDeveloperRole`
- `COMMUNITY_MODERATION/MusicFlowAccessTest#permissionsWithoutPlatformAdminRoleHaveNoEffect`


## Policy and usage

`py -3 scripts/impact_regression.py validate` — fail on test class not mapped, stale smoke, or dependency cycle.

`py -3 scripts/impact_regression.py plan --files app/src/main/java/com/example/musicflow/feature/search/SearchViewModel.kt`

`py -3 scripts/impact_regression.py plan --git-diff --json` — analyze current changes, including untracked files.

`py -3 scripts/impact_regression.py plan --module SEARCH` — explicitly supplied direct module.

Before code edits record direct modules, dependents, selected case IDs and their reasons.
After running report PASS/FAIL/SKIP with command/SDK/log evidence. Failure or uncovered runtime changes
expand the selection; never call a planned test PASS. Full regression is mandatory for releases,
security/data/SAF/schema/shared changes, unknown sources, or unresolvable side effects. Android
instrumentation requires an isolated QA AVD; physical-device TalkBack/keyboard is manual acceptance.
SignedApkUpgradeV6Test is a paired seed/verify test: always use the signed upgrade script.
