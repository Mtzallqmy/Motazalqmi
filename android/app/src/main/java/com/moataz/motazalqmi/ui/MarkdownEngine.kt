package com.moataz.motazalqmi.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin

/** CommonMark engine, separate from Compose. No HTML/WebView execution or remote image loading. */
class MarkdownEngine(context: Context) {
 val renderer: Markwon = Markwon.builder(context)
  .usePlugin(io.noties.markwon.SoftBreakAddsNewLinePlugin.create()).usePlugin(TablePlugin.create(context)).usePlugin(StrikethroughPlugin.create())
  .usePlugin(object : AbstractMarkwonPlugin() {
   override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
    builder.linkResolver { view, link ->
     val uri = Uri.parse(link)
     if (uri.scheme == "https" && !uri.host.isNullOrBlank()) {
      runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
     }
    }
   }
  }).build()
 fun render(view: TextView, text: String) { renderer.setMarkdown(view, text) }
}
