package org.actlab.updater;

public enum UpdaterStatus {
	INITIALIZED,
	FAIL,				// 通信エラー等
	ALREADY_UPDATED,	// 最新版を利用中
	PENDING,
	SEE_WEB,
	FINISH;
}
