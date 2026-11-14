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
