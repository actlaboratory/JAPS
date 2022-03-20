package org.actlab.updater;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;


public class UpdateChecker {
    // HTTP接続設定
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    // 通信パラメータ
    private static final String BASE_URL = "https://stg.actlab.org/api/checkUpdate";
    private static final String UPDATER_VERSION = "1.0.0";

    // 内部設定
    private static final String PATCH_FILE_NAME="patch.zip";
    private static final String UPDATER_FILE_NAME = "updater.exe";
    private static final String UPDATER_WAKE_WORD = "hello";


    private String name;        // software name
    private String version;     // software version
    private String lang;        // response language

    @Getter
    private UpdaterStatus status = UpdaterStatus.INITIALIZED;
    @Getter
    private String message = "";
    @Getter
    private UpdaterResponse response = null;

    public UpdateChecker(String name, String version){
        this.name = name.toUpperCase();
        this.version = version;
        this.lang = "ja";
    }

    public UpdaterStatus check(){
       try {
           HttpURLConnection con = (HttpURLConnection)getUrl().openConnection();
            con.setReadTimeout(READ_TIMEOUT);
        	con.setConnectTimeout(CONNECTION_TIMEOUT);

            con.connect();

            String result = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            
            String tmp = "";
            while ((tmp = in.readLine()) != null){
                result += tmp;
            }

            ObjectMapper mapper = new ObjectMapper();
            this.response =  mapper.readValue(result, UpdaterResponse.class);

            in.close();
            con.disconnect();
        } catch (SocketTimeoutException e) {
            this.status = UpdaterStatus.FAIL;
            this.message = "通信がタイムアウトしました。";
            return this.status;
        } catch (IOException e) {
            this.status = UpdaterStatus.FAIL;
            this.message = "エラーが発生しました。インターネット接続の状態を確認し、再度お試しください。";
            return this.status;
        }

       responseToStatus(response);
       this.message = response.getDisplayMessage();

        return this.status;
    }

    public boolean download() {
        if ((response != null) && (status == UpdaterStatus.PENDING)) {
            try {
                URL url = new URL(response.getUpdater_url());
                Files.copy(url.openStream(),
                        Paths.get(PATCH_FILE_NAME),
                        REPLACE_EXISTING);
                return true;
            } catch (MalformedURLException e) {
                throw new InternalError(e.toString());
            } catch (IOException e) {
                this.status = UpdaterStatus.FAIL;
                this.message = "ダウンロードに失敗しました。\n\n" + e;
                return false;
            }
        }
        return false;
    }
    
    public boolean run() {
        long pid = ProcessHandle.current().pid();
 
        String[] args = {
           System.getProperty("java.class.path"),
           UPDATER_WAKE_WORD,
           UPDATER_FILE_NAME,           
           this.response.getUpdater_hash(),
           String.valueOf(pid)
        };
        try {
            Runtime.getRuntime().exec(args);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private URL getUrl() {
        try {
            return new URL(BASE_URL + getQuery());
        } catch (MalformedURLException e) {
            throw new InternalError(e.toString());
        }
    }

    private String getQuery(){
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("name", this.name);
        queryMap.put("version", this.version);
        queryMap.put("updater_version", UPDATER_VERSION);
        queryMap.put("lang", this.lang);

        StringJoiner queryJoinner = new StringJoiner("&", "?", "");
        for (Map.Entry<String, String> parameter : queryMap.entrySet()) {
            queryJoinner.add(parameter.getKey() + "=" + parameter.getValue());
        }

        return queryJoinner.toString();
    }

    private void responseToStatus(UpdaterResponse response) {
        switch (response.getCode()){
        case UpdaterResponse.RESPONSE_CODE_NEED_UPDATE:
            this.status = UpdaterStatus.PENDING;
            break;
        case UpdaterResponse.RESPONSE_CODE_UP_TO_DATE:
            this.status = UpdaterStatus.FINISH;
            break;
        case UpdaterResponse.RESPONSE_CODE_VISIT_WEB:
            this.status = UpdaterStatus.SEE_WEB;
            break;
        default:
            this.status = UpdaterStatus.FAIL;
        }
    }
}
