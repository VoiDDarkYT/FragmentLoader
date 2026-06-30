package dev.fragmentcode.installer.download;

/**
 * Печатает прогресс скачивания в консоль в виде простого текстового
 * прогресс-бара, перезаписывая строку через carriage return (\r),
 * чтобы не "заспамить" вывод тысячами строк при скачивании assets.
 */
public final class ConsoleDownloadListener implements DownloadListener {

    private static final int BAR_WIDTH = 30;

    private String currentFileName = "";

    @Override
    public void onFileProgress(String fileName, long bytesDone, long bytesTotal) {

        this.currentFileName = fileName;

        if (bytesTotal <= 0) {
            return;
        }

        double ratio = (double) bytesDone / (double) bytesTotal;
        printBar(fileName, ratio);

    }

    @Override
    public void onFileComplete(String fileName) {
        printBar(fileName, 1.0);
        System.out.println();
    }

    @Override
    public void onGroupProgress(int filesDone, int filesTotal) {

        if (filesTotal <= 0) {
            return;
        }

        double ratio = (double) filesDone / (double) filesTotal;
        printBar("assets (" + filesDone + "/" + filesTotal + ")", ratio);

        if (filesDone == filesTotal) {
            System.out.println();
        }

    }

    private void printBar(String label, double ratio) {

        int filled = (int) (ratio * BAR_WIDTH);

        StringBuilder bar = new StringBuilder();
        bar.append('[');

        for (int i = 0; i < BAR_WIDTH; i++) {
            bar.append(i < filled ? '#' : ' ');
        }

        bar.append(']');

        System.out.printf(
                "\r%s %3d%% %s",
                bar,
                (int) (ratio * 100),
                label
        );

    }

}
