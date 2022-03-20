package org.actlab.updater;

import lombok.Getter;

public class UpdaterResponse {
    public static final int RESPONSE_CODE_NEED_UPDATE = 200;
    public static final int RESPONSE_CODE_UP_TO_DATE = 204;
    public static final int RESPONSE_CODE_VISIT_WEB = 205;
    public static final int RESPONSE_CODE_PARAM_ERROR = 400;
    public static final int RESPONSE_CODE_SOFTWARE_NOT_FOUND = 404;
    

    @Getter
    int code;

    @Getter
    String message;

    @Getter
    String updater_url;
    
    @Getter
    String updater_hash;
    
    @Getter
    String update_version;

    @Getter
    String update_description;

    @Getter
    String URL;

    @Getter
    String info_description;
    
    public String getDisplayMessage() {
        switch(this.code){
        case RESPONSE_CODE_NEED_UPDATE:
            return "バージョン "+update_version+" にアップデートできます。";
        case RESPONSE_CODE_UP_TO_DATE:
            return "最新版を利用中です。アップデートの必要はありません。";
        case RESPONSE_CODE_VISIT_WEB:
            return "重要なお知らせがあります。ウェブサイトを参照してください。";
        case RESPONSE_CODE_PARAM_ERROR:
        case RESPONSE_CODE_SOFTWARE_NOT_FOUND:
            return "通信エラーが発生しました。";
        default:
            return "不明なレスポンス " + this.code + "を受信しました。";
    	}
    }
}
