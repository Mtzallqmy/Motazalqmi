package com.moataz.motazalqmi.ui

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.util.concurrent.CancellationException

data class TicketRequest(val purpose:String,val mime:String,val size:Long,val prefix:String)
data class UploadTicket(val pathname:String,val token:String)

internal suspend fun uploadWriterFile(context:Context,api:WriterApi,uri:Uri,purpose:String,
 bearer:String,progress:(Int)->Unit):String=withContext(Dispatchers.IO){
 val mime=context.contentResolver.getType(uri) ?: error("تعذرت معرفة نوع الملف")
 val spool=File.createTempFile("attachment-", ".upload", context.cacheDir)
 try {
  val buffer=ByteArray(64*1024)
  context.contentResolver.openInputStream(uri)?.use { input ->
   spool.outputStream().use { out ->
    var total=0L
    while(true){
     ensureActive()
     val count=input.read(buffer)
     if(count<0)break
     total+=count
     require(total<=100L*1024*1024){"يتجاوز الملف 100 ميجابايت"}
     out.write(buffer,0,count)
    }
   }
  } ?: error("تعذرت قراءة الملف")
  val length=spool.length()
  require(length>=12){"الملف فارغ أو غير مكتمل"}
  if(length<=1800000) {
   return@withContext api.uploadMedia(bearer,MediaDirectRequest(mime,Base64.encodeToString(spool.readBytes(),Base64.NO_WRAP),purpose)).url
  }
  val prefix=ByteArray(12)
  java.io.DataInputStream(spool.inputStream()).use { it.readFully(prefix) }
  val ticket=api.ticket(bearer,TicketRequest(purpose,mime,length,Base64.encodeToString(prefix,Base64.NO_WRAP)))
  require(ticket.pathname.matches(Regex("(media|covers|authors)/[a-zA-Z0-9.-]+")))
  val coroutineContext=this.coroutineContext
  val body=object:RequestBody(){
   override fun contentType()=mime.toMediaType()
   override fun contentLength()=length
   override fun writeTo(sink:BufferedSink){
    spool.inputStream().use { input ->
     var sent=0L;var last=-1
     while(true){
      coroutineContext.ensureActive()
      val n=input.read(buffer)
      if(n<0)break
      sink.write(buffer,0,n);sent+=n
      val percent=(sent*100/length).toInt()
      if(percent!=last){progress(percent);last=percent}
     }
    }
   }
  }
  api.putBlob(ticket.pathname,"Bearer "+ticket.token,mime,length.toString(),body).url
 }finally{spool.delete()}
}
