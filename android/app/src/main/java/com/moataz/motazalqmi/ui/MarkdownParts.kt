package com.moataz.motazalqmi.ui

/** Split the real article Markdown into text and HTTPS image blocks without WebView/HTML. */
data class MarkdownPart(val text:String?=null,val imageUrl:String?=null)
fun splitMarkdownMedia(markdown:String):List<MarkdownPart> {
 val image=Regex("""!\[[^\]]{0,300}]\(\s*<?(https://[^)\s>]+)>?(?:\s+["'][^"']*["'])?\s*\)""")
 val parts=mutableListOf<MarkdownPart>()
 var offset=0
 for(match in image.findAll(markdown)){
  if(match.range.first>offset)parts.add(MarkdownPart(text=markdown.substring(offset,match.range.first)))
  parts.add(MarkdownPart(imageUrl=match.groupValues[1]))
  offset=match.range.last+1
 }
 if(offset<markdown.length||parts.isEmpty())parts.add(MarkdownPart(text=markdown.substring(offset)))
 return parts
}
