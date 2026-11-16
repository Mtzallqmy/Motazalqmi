package com.moataz.motazalqmi.ui
/** Image, video, audio, PDF attachments included inline in Markdown articles. */
data class MarkdownPart(
 val text:String?=null,val imageUrl:String?=null,val videoUrl:String?=null,
 val audioUrl:String?=null,val documentUrl:String?=null,val label:String=""
)
fun splitMarkdownMedia(markdown:String):List<MarkdownPart> {
 val media=Regex("""!\[([^\]]{0,300})]\(\s*<?(https://[^)\s>]+)>?(?:\s+["'][^"']*["'])?\s*\)""")
 val parts=mutableListOf<MarkdownPart>()
 var offset=0
 for(match in media.findAll(markdown)){
  if(match.range.first>offset)parts.add(MarkdownPart(text=markdown.substring(offset,match.range.first)))
  val url=match.groupValues[2]
  val ext=runCatching{java.net.URI(url).path.orEmpty().substringAfterLast('.').lowercase()}.getOrDefault("")
  parts.add(when(ext){
   "mp4","webm"->MarkdownPart(videoUrl=url,label=match.groupValues[1])
   "mp3","m4a","ogg"->MarkdownPart(audioUrl=url,label=match.groupValues[1])
   "pdf"->MarkdownPart(documentUrl=url,label=match.groupValues[1])
   else->MarkdownPart(imageUrl=url,label=match.groupValues[1])
  })
  offset=match.range.last+1
 }
 if(offset<markdown.length||parts.isEmpty())parts.add(MarkdownPart(text=markdown.substring(offset)))
 return parts
}

/** Split at actual ATX headings so the native reader can navigate without changing Markdown. */
fun readerParts(markdown: String): List<MarkdownPart> = splitMarkdownMedia(markdown).flatMap { part ->
 val text = part.text
 if (text == null) listOf(part) else {
  val result = mutableListOf<MarkdownPart>()
  val buffer = StringBuilder()
  var fence: Char? = null
  for (line in text.splitToSequence("\n")) {
   val trimmed = line.trimStart()
   val isFence = trimmed.startsWith("```") || trimmed.startsWith("~~~")
   val heading = fence == null && Regex("^#{1,6}\\s+.+").matches(line)
   if (heading && buffer.isNotEmpty()) { result.add(MarkdownPart(text = buffer.toString())); buffer.clear() }
   buffer.append(line).append('\n')
   if (isFence) {
    if (fence == null) fence = trimmed.first() else if (fence == trimmed.first()) fence = null
   }
  }
  if (buffer.isNotEmpty()) result.add(MarkdownPart(text = buffer.toString().removeSuffix("\n")))
  result
 }
}
fun partHeading(part: MarkdownPart): String? = part.text?.lineSequence()?.firstOrNull()?.let {
 Regex("^#{1,6}\\s+(.+?)\\s*#*\\s*$").matchEntire(it)?.groupValues?.get(1)
}
