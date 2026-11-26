package io.github.aedev.flow.presentation.ui

/**
 * Whether this target can actually play a stream. Android can (Media3); the desktop and iOS
 * targets cannot yet, and the player keeps its poster there rather than showing a black surface.
 */
expect val isVideoPlaybackSupported: Boolean
