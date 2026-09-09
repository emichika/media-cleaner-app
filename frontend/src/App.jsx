import React, { useState } from 'react';
import { Upload, FileImage, ShieldAlert, CheckCircle, RefreshCw } from 'lucide-react';

function App() {
  const [isDragActive, setIsDragActive] = useState(false);
  const [uploadStatus, setUploadStatus] = useState(null); // 'success', 'duplicate', 'error'
  const [fileName, setFileName] = useState('');

  // マウスがドロップエリアに入ってきたとき
  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragActive(true);
  };

  // マウスがドロップエリアから離れたとき
  const handleDragLeave = () => {
    setIsDragActive(false);
  };

  // 実際に写真がポイッとドロップされたとき
  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragActive(false);
    
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      const file = files[0];
      setFileName(file.name);
      
      // ⚠️ 将来ここにJava（バックエンド）へファイルを送信する通信処理を書きます！
      // 今回は画面のテスト用に、ランダムに「新規保存」か「重複検知」になるシミュレーションを走らせます
      simulateBackendCheck(file.name);
    }
  };

  // Javaとの通信を模した実験用シミュレーター
  const simulateBackendCheck = (name) => {
    setUploadStatus('checking');
    setTimeout(() => {
      const isDuplicate = Math.random() > 0.5;
      setUploadStatus(isDuplicate ? 'duplicate' : 'success');
    }, 1200);
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col items-center justify-center p-6" style={{ fontFamily: 'sans-serif' }}>
      <div className="max-w-xl w-full bg-slate-800 rounded-2xl shadow-xl p-8 border border-slate-700">
        
        {/* ヘッダータイトル */}
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-white mb-2">Media Cleaner App</h1>
          <p className="text-sm text-slate-400">重複写真・動画の自動検知 ＆ 撮影日リネーム</p>
        </div>

        {/* 📸 ドラッグ＆ドロップのエリア */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          style={{
            border: isDragActive ? '2px dashed #6366f1' : '2px dashed #475569',
            backgroundColor: isDragActive ? '#312e81' : '#1e293b',
            transition: 'all 0.2s ease'
          }}
          className="rounded-xl p-10 flex flex-col items-center justify-center cursor-pointer mb-6"
        >
          <Upload className={`w-12 h-12 mb-4 ${isDragActive ? 'text-indigo-400' : 'text-slate-400'}`} />
          <p className="text-base font-medium text-slate-200 mb-1">
            ここに写真ファイルをドラッグ＆ドロップ
          </p>
          <p className="text-xs text-slate-400">またはファイルを選択（JPEG, JPG対応）</p>
        </div>

        {/* 状態表示（ステータスカード） */}
        {uploadStatus && (
          <div className="mt-4 p-4 rounded-xl border bg-slate-900 border-slate-700">
            {uploadStatus === 'checking' && (
              <div className="flex items-center space-x-3 text-indigo-400">
                <RefreshCw className="w-5 h-5 animate-spin" />
                <span className="text-sm font-medium">Javaバックエンドで重複チェック中...</span>
              </div>
            )}

            {uploadStatus === 'success' && (
              <div className="flex items-center space-x-3 text-emerald-400">
                <CheckCircle className="w-5 h-5" />
                <div>
                  <p className="text-sm font-bold">[NEW FILE] 重複はありません！</p>
                  <p className="text-xs text-slate-400">{fileName} をMySQLに新規登録しました。</p>
                </div>
              </div>
            )}

            {uploadStatus === 'duplicate' && (
              <div className="flex items-center space-x-3 text-amber-400">
                <ShieldAlert className="w-5 h-5" />
                <div>
                  <p className="text-sm font-bold">[DUPLICATE DETECTED] 重複を検知しました！</p>
                  <p className="text-xs text-slate-400">{fileName} はすでにデータベースに存在するためスキップします。</p>
                </div>
              </div>
            )}
          </div>
        )}

      </div>
    </div>
  );
}

export default App;
