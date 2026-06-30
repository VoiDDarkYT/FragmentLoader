package dev.fragmentcode.installer.download;

/**
 * Получает уведомления о прогрессе скачивания одного файла или целой
 * группы файлов (например всех assets).
 *
 * bytesDone/bytesTotal — для одного файла.
 * filesDone/filesTotal — для группы файлов (например 5000 объектов assets).
 * Если группа состоит из одного файла, filesTotal будет равен 1.
 */
public interface DownloadListener {

    void onFileProgress(String fileName, long bytesDone, long bytesTotal);

    void onFileComplete(String fileName);

    void onGroupProgress(int filesDone, int filesTotal);

    /**
     * Заглушка без вывода — удобно когда прогресс не нужен.
     */
    DownloadListener NO_OP = new DownloadListener() {

        @Override
        public void onFileProgress(String fileName, long bytesDone, long bytesTotal) {
        }

        @Override
        public void onFileComplete(String fileName) {
        }

        @Override
        public void onGroupProgress(int filesDone, int filesTotal) {
        }

    };

}
