package com.moataz.motazalqmi.ui

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun scheduleFromLocal(value:String,zone:ZoneId=ZoneId.systemDefault()):String =
 if(value.isBlank())"" else LocalDateTime.parse(value.trim().replace(' ','T')).atZone(zone).toInstant().toString()
fun scheduleToLocal(value:String,zone:ZoneId=ZoneId.systemDefault()):String =
 if(value.isBlank())"" else runCatching {
  Instant.parse(value).atZone(zone).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
 }.getOrDefault("")
fun articleIsScheduled(article:com.moataz.motazalqmi.data.ArticleDto):Boolean =
 article.status=="published"&&runCatching { Instant.parse(article.scheduledAt).isAfter(Instant.now()) }.getOrDefault(false)
