-keepattributes Signature
-keepattributes *Annotation*
-keep class com.moataz.motazalqmi.data.**Dto { *; }
-keep class com.moataz.motazalqmi.data.Snapshot { *; }

# Retrofit/Gson accesses wire payloads reflectively in minified release builds.
-keep class com.moataz.motazalqmi.ui.WriterLogin { *; }
-keep class com.moataz.motazalqmi.ui.WriterRefresh { *; }
-keep class com.moataz.motazalqmi.ui.WriterAccess { *; }
-keep class com.moataz.motazalqmi.ui.WriterAction { *; }
-keep class com.moataz.motazalqmi.ui.WriterList { *; }
-keep class com.moataz.motazalqmi.ui.WriterProfile* { *; }
-keep class com.moataz.motazalqmi.ui.MediaDirect* { *; }
-keep class com.moataz.motazalqmi.ui.TicketRequest { *; }
-keep class com.moataz.motazalqmi.ui.UploadTicket { *; }
