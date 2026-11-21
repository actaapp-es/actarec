package es.actarec.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** La clave personal BYOK se cifra mediante una clave AES-GCM del Android Keystore. */
public final class SecureKey {
    private static final String ALIAS="actarec_groq_personal_v1";
    private static final String STORE="prefs";
    private static final String FIELD="groq_key_encrypted";
    private SecureKey() {}
    private static SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if (store.containsAlias(ALIAS)) return ((KeyStore.SecretKeyEntry)store.getEntry(ALIAS,null)).getSecretKey();
        KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return gen.generateKey();
    }
    public static void set(Context c,String plain) throws Exception {
        SharedPreferences p=c.getSharedPreferences(STORE,Context.MODE_PRIVATE);
        if (plain==null || plain.trim().isEmpty()) {p.edit().remove(FIELD).apply();return;}
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] iv=cipher.getIV(), ct=cipher.doFinal(plain.trim().getBytes(StandardCharsets.UTF_8));
        byte[] combined=new byte[iv.length+ct.length];
        System.arraycopy(iv,0,combined,0,iv.length);System.arraycopy(ct,0,combined,iv.length,ct.length);
        p.edit().putString(FIELD,Base64.encodeToString(combined,Base64.NO_WRAP)).apply();
    }
    public static String get(Context c) {
        try {
            String value=c.getSharedPreferences(STORE,Context.MODE_PRIVATE).getString(FIELD,"");
            if(value.isEmpty())return "";
            byte[] raw=Base64.decode(value,Base64.NO_WRAP);
            if(raw.length<29)return "";
            byte[] iv=new byte[12],ct=new byte[raw.length-12];
            System.arraycopy(raw,0,iv,0,12);System.arraycopy(raw,12,ct,0,ct.length);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
            return new String(cipher.doFinal(ct),StandardCharsets.UTF_8);
        }catch(Exception e){return "";}
    }
    public static boolean configured(Context c) {return !get(c).isEmpty();}
}
