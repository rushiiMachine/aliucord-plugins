include(":BetterTm")
includeNoCI(":TypingIndicators")
include(":NoPingWidthLimit")
include(":HideMessages")
include(":FixBlockedReplies")
include(":NoBuiltInCommands")
include(":OpenProfileInReactions")
include(":NoNitroAvatars")
include(":StreamerMode")
include(":NoUppercase")
include(":FirstMessage")
include(":ConfigurableStickerSizes")
include(":SplitMessages")
include(":OpenDebug")
includeNoCI(":Template")
rootProject.name = "aliucord-plugins"

fun includeNoCI(vararg projectPaths: String?) {
    if (System.getenv("CI") != "true") include(*projectPaths)
}
