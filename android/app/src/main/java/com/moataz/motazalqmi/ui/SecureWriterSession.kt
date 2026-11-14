package com.moataz.motazalqmi.ui

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Refresh credentials are encrypted with an Android Keystore non-exportable AES key.
 * No password or access bearer is persisted. The app has allowBackup=false.
 */
@Singleton class SecureWriterSession @Inject constructor(@ApplicationContext private val context:Context){
 private val prefs=context.getSharedPreferences("writer_session_v2",Context.MODE_PRIVATE)
 private val alias="motazalqmi_writer_refresh_v2"
 private fun key():SecretKey {
  val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
  val existing=store.getKey(alias,null) as? SecretKey
  if(existing!=null)return existing
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{
   init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setRandomizedEncryptionRequired(true).build())
  }.generateKey()
 }
 fun save(refresh:String,expiresAt:Long){
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE,key())
  val text=Base64.encodeToString(cipher.doFinal(refresh.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)
  val iv=Base64.encodeToString(cipher.iv,Base64.NO_WRAP)
  prefs.edit().putString("cipher",text).putString("iv",iv).putLong("expiry",expiresAt).apply()
 }
 fun load():String? {
  if(System.currentTimeMillis()>=prefs.getLong("expiry",0L)){clear();return null}
  return try{
   val cipher=Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(prefs.getString("iv",null),Base64.NO_WRAP)))
   String(cipher.doFinal(Base64.decode(prefs.getString("cipher",null),Base64.NO_WRAP)),Charsets.UTF_8)
  }catch (_:Exception){clear();null}
 }
 fun clear(){prefs.edit().clear().apply()}
}
