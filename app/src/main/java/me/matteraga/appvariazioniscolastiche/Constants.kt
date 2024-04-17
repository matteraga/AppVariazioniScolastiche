package me.matteraga.appvariazioniscolastiche

const val NOTIFICATION_CHANNEL_ID = "changes"

const val GITHUB_RELEASES_URL = "https://github.com/matteraga/action-test/releases"
const val GITHUB_API_LATEST_RELEASE_URL = "https://api.github.com/repos/matteraga/action-test/releases/latest"

object ChangesToCheck {
    const val TODAY = 0
    const val TOMORROW = 1
    const val TODAY_AND_TOMORROW = 2
}