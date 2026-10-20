import React, { useState } from 'react';
import { Upload, FileImage, ShieldAlert, CheckCircle, RefreshCw, FolderOpen } from 'lucide-react';

function App() {
  const [isDragActive, setIsDragActive] = useState(false);
  const [uploadStatus, setUploadStatus] = useState(null); // 'checking', 'done', 'error'
  const [currentFileIndex, setCurrentFileIndex] = useState(0);
  const [totalFiles, setTotalFiles] = useState(0);
  const [currentFileName, setCurrentFileName] = useState('');
  
  // 履歴カウント用
  const [successCount, setSuccessCount] = useState(0);
  const [duplicateCount, setDuplicateCount] = useState(0);

  // マウスがドロップエリアに入ってきたとき
  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragActive(true);
  };

  // マウスがドロップエリアから離れたとき
  const handleDragLeave = () => {
    setIsDragActive(false);
  };

  // ⚡【新機能】フォルダの中身を再帰的にスキャンしてファイルを1枚ずつ抽出する魔法の関数
  const traverseFileTree = async (item, fileList = []) => {
    if (item.isFile) {
      const file = await new Promise((resolve) => item.file(resolve));
      // 写真・動画のみをターゲットにする
      if (file.type.startsWith('image/') || file.type.startsWith('video/') || file.name.endsWith('.mp4') || file.name.endsWith('.mov') || file.name.endsWith('.heic') || file.name.endsWith('.jpg') || file.name.endsWith('.jpeg')) {
        fileList.push(file);
      }
    } else if (item.isDirectory) {
      const dirReader = item.createReader();
      const entries = await new Promise((resolve) => dirReader.readEntries(resolve));
      for (const entry of entries) {
        await traverseFileTree(entry, fileList);
      }
    }
    return fileList;
  };

  // 実際に写真やフォルダがポイッとドロップされたとき
  const handleDrop = async (e) => {
    e.preventDefault();
    setIsDragActive(false);
    
    // カウントをリセット
    setSuccessCount(0);
    setDuplicateCount(0);

    const items = e.dataTransfer.items;
    let allFiles = [];

    if (items) {
      logInfo("Scanning dropped items...");
      // ドロップされたものがフォルダかファイルか解析
      for (let i = 0; i < items.length; i++) {
        const item = items[i].webkitGetAsEntry();
        if (item) {
          await traverseFileTree(item, allFiles);
        }
      }
    }

    if (allFiles.length > 0) {
      setTotalFiles(allFiles.length);
      setUploadStatus('checking');
      
      // ⚡【一括自動連射ループ】全ファイルを上から順番に1枚ずつJavaへ送信！
      for (let i = 0; i < allFiles.length; i++) {
        setCurrentFileIndex(i + 1);
        setCurrentFileName(allFiles[i].name);
        
        // Javaの窓口へ送信して、お返事が来るまで次のループを「待機（await）」する
        await uploadFileToBackend(allFiles[i]);
      }
      
      // すべての連射が終わったら完了状態にする
      setUploadStatus('done');
    }
  };

  // 🚀 Javaのバックエンド窓口へ本物の写真を1枚送信する関数
  const uploadFileToBackend = async (file) => {
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('http://localhost:8080/api/media/upload', {
        method: 'POST',
        body: formData,
      });

      if (response.ok) {
        const data = await response.json();
        // Javaからの本物の結果（success / duplicate）を集計
        if (data.status === 'success') {
          setSuccessCount(prev => prev + 1);
        } else if (data.status === 'duplicate') {
          setDuplicateCount(prev => prev + 1);
        }
      } else {
        console.error('Java側でエラーが発生しました');
      }
    } catch (error) {
      console.error('通信エラーが発生しました:', error);
    }
  };

  function logInfo(msg) {
    console.log("[REACT] " + msg);
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col items-center justify-center p-6" style={{ fontFamily: 'sans-serif' }}>
      <div className="max-w-xl w-full bg-slate-800 rounded-2xl shadow-xl p-8 border border-slate-700">
        
        {/* ヘッダータイトル */}
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-white mb-2">Media Cleaner App</h1>
          <p className="text-sm text-slate-400">一括フォルダドロップ ＆ 超高速自動お掃除</p>
        </div>

        {/* 📸 ドラッグ＆ドロップ ＆ フォルダ対応エリア */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          style={{
            border: isDragActive ? '2px dashed #6366f1' : '2px dashed #475569',
            backgroundColor: isDragActive ? '#312e81' : '#1e293b',
            transition: 'all 0.2s ease'
          }}
          className="rounded-xl p-12 flex flex-col items-center justify-center cursor-pointer mb-6"
        >
          <FolderOpen className={`w-14 h-14 mb-4 ${isDragActive ? 'text-indigo-400' : 'text-slate-400'}`} />
          <p className="text-base font-medium text-slate-200 mb-1">
            ここに複数のファイル、または【フォルダごと】ドロップ
          </p>
          <p className="text-xs text-slate-400">何枚でも一括でお掃除・撮影日リネームを行います</p>
        </div>

        {/* 📊 ⚡進捗メーター ＆ ステータスカード */}
        {uploadStatus && (
          <div 
            style={{
              backgroundColor: uploadStatus === 'checking' ? '#1e1b4b' : '#064e3b',
              borderColor: uploadStatus === 'checking' ? '#6366f1' : '#10b981',
              transition: 'all 0.3s ease'
            }}
            className="mt-4 p-5 rounded-xl border"
          >
            {uploadStatus === 'checking' && (
              <div>
                <div className="flex items-center justify-between mb-3 text-indigo-300">
                  <div className="flex items-center space-x-3">
                    <RefreshCw className="w-5 h-5 animate-spin" />
                    <span className="text-sm font-bold">一括処理を実行中...</span>
                  </div>
                  <span className="text-xs bg-indigo-900 text-indigo-200 px-2 py-1 rounded-full font-mono">
                    {currentFileIndex} / {totalFiles}
                  </span>
                </div>
                
                {/* ゲージメーター */}
                <div className="w-full bg-slate-700 h-2 rounded-full overflow-hidden mb-3">
                  <div 
                    style={{ 
                      width: `${(currentFileIndex / totalFiles) * 100}%`,
                      transition: 'width 0.1s ease'
                    }} 
                    className="bg-indigo-500 h-full"
                  />
                </div>
                <p className="text-xs text-slate-400 truncate">処理中: {currentFileName}</p>
              </div>
            )}

            {uploadStatus === 'done' && (
              <div className="flex items-start space-x-3 text-emerald-300">
                <CheckCircle className="w-5 h-5 mt-0.5" />
                <div className="w-full">
                  <p className="text-sm font-bold">[COMPLETE] すべての一括処理が完了しました！</p>
                  <div className="grid grid-cols-2 gap-2 mt-3 text-xs bg-slate-900/50 p-3 rounded-lg border border-slate-700">
                    <div className="text-emerald-400">✨ 新規整理保存: <span className="font-bold text-sm font-mono">{successCount}</span> 枚</div>
                    <div className="text-amber-400">⚠️ 重複自動退避: <span className="font-bold text-sm font-mono">{duplicateCount}</span> 枚</div>
                  </div>
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
