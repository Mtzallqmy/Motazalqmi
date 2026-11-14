package com.moataz.motazalqmi.domain

/** Arabic search tolerant of common hamza and taa marbuta variations; not a replacement for displayed text. */
object ArabicSearch {
 private val vowelMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
 fun normalize(value: String): String = value.lowercase()
  .replace(vowelMarks, "")
  .replace(Regex("[أإآٱ]"), "ا")
  .replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')

 fun matches(query: String, vararg fields: String): Boolean {
  val terms = normalize(query).trim().split(Regex("\\s+")).filter { it.isNotBlank() }
  if (terms.isEmpty()) return true
  val haystack = normalize(fields.joinToString(" "))
  return terms.all { it in haystack }
 }
}
