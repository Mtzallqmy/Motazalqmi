package com.moataz.motazalqmi.domain

object ArabicSearch {
 fun normalize(value: String): String = value.lowercase()
  .replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06EDـ]"), "")
  .replace(Regex("[أإآٱ]"), "ا").replace('ى', 'ي')
 fun matches(query: String, vararg fields: String): Boolean {
  val haystack = normalize(fields.joinToString(" "))
  return normalize(query).trim().split(Regex("\\s+")).all { it in haystack }
 }
}
