import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.RxUtils.subscribe
import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.discord.stores.*
import com.discord.utilities.channel.ChannelSelector
import com.discord.views.typing.TypingDots
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.items.ChannelListItem
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel
import com.discord.widgets.chat.overlay.ChatTypingModel
import com.discord.widgets.chat.overlay.`ChatTypingModel$Companion$get$1`
import rx.Observable
import rx.Subscription
import java.util.*

@Suppress("unused")
@AliucordPlugin
class TypingIndicators : Plugin() {
    private val channels = mutableMapOf<Long, Subscription>()
    private val typingDotsId = View.generateViewId()

    override fun start(context: Context) {
        val lp = RelativeLayout.LayoutParams(DimenUtils.dpToPx(24), RelativeLayout.LayoutParams.MATCH_PARENT)
            .apply {
                marginEnd = DimenUtils.dpToPx(16)
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }

        patcher.after<WidgetChannelsListAdapter.ItemChannelText>(
            "onConfigure",
            Integer.TYPE,
            ChannelListItem::class.java
        ) {
            val textChannel = it.args[1] as ChannelListItemTextChannel
            val channelId = textChannel.component1().id
            val itemChannelText = it.thisObject as WidgetChannelsListAdapter.ItemChannelText

//            logger.info("-".repeat(20))
            if (channels.containsKey(channelId)) return@after
//            logger.info("hasnt been initialized")
            if (itemChannelText.itemView.findViewById<TypingDots>(typingDotsId) != null) return@after
//            logger.info("typing dots dont exist")
//            logger.info("channel: ${textChannel.channel.name}")

            val typingDots = TypingDots(Utils.appActivity, null).apply {
                id = typingDotsId
                visibility = View.GONE
                alpha = 0.4f
                scaleY = 0.8f
                scaleX = 0.8f
            }
            (itemChannelText.itemView as RelativeLayout).addView(typingDots, lp)

            val subscription =
                `ChatTypingModel$Companion$get$1`<StoreChannelsSelected.ResolvedSelectedChannel, Observable<ChatTypingModel.Typing>>()
                    .call(
                        StoreChannelsSelected.ResolvedSelectedChannel.Channel(
                            textChannel.channel,
                            null, null
                        )
                    ).subscribe {
                        this as ChatTypingModel.Typing
                        Utils.mainThread.post {
                            if (typingUsers.isEmpty()) {
                                typingDots.b()
                                typingDots.visibility = View.GONE
                            } else {
                                typingDots.a(false)
                                typingDots.visibility = View.VISIBLE
                            }
                        }
                    }

            channels[channelId] = subscription
        }

        patcher.before<ChannelSelector>(
            "gotoChannel",
            Long::class.java,
            Long::class.java,
            java.lang.Long::class.java,
            SelectedChannelAnalyticsLocation::class.java
        ) {
            // only switch on guild change
//            logger.info("selected: " + StoreStream.getGuildSelected().selectedGuildId)
//            logger.info("selecting: " + it.args[0])
            if (StoreStream.getGuildSelected().selectedGuildId == it.args[0]) return@before
//            logger.info("subscriptions removed: " + channels.size.toString())
//            channels.values.forEach(Subscription::unsubscribe)
//            channels.clear()
        }
    }

    override fun stop(context: Context) = patcher.unpatchAll()
}
