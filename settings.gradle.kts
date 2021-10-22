include(":FirstMessage")
include(":ConfigurableStickerSizes")
include(":SplitMessages")
includeNoCI(":OpenDebug")
includeNoCI(":Template")
rootProject.name = "aliucord-plugins"

fun includeNoCI(vararg projectPaths: String?) {
    if (System.getenv("CI") != "true") include(*projectPaths)
}
