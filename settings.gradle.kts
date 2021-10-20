rootProject.name = "aliucord-plugins"
include(":FirstMessage")
include(":SplitMessages")
includeNoCI(":OpenDebug")
includeNoCI(":Template")

fun includeNoCI(vararg projectPaths: String?) {
    if (System.getenv("CI") != "true") include(*projectPaths)
}
